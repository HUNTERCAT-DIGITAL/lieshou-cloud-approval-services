package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.huntercat.lieshou.framework.approval.ApprovalService;
import cn.huntercat.lieshou.framework.approval.domain.ApprovalAuditLog;
import cn.huntercat.lieshou.framework.approval.domain.ApprovalRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalDtos.CreateApprovalRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalDtos.DecideRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalDtos.RejectRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalForbiddenException;
import cn.huntercat.lieshou.framework.common.api.TenantContextRequiredException;
import cn.huntercat.lieshoucloudpro.approval.feign.EmployeeStatusClient;
import cn.huntercat.lieshoucloudpro.approval.feign.ProjectCreateClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 审批 Controller（薄壳 · web 装配）。
 *
 * <p>业务逻辑在 {@link ApprovalService}（LieShou-framework，上游同源唯一）； 本层只做租户/用户头解析 + HTTP 装配。
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

  private static final Logger log = LoggerFactory.getLogger(ApprovalController.class);

  private static final String HDR_TENANT_ID = "X-Tenant-Id";
  private static final String HDR_USER_ID = "X-User-Id";

  private final ApprovalService approvalService;
  private final ProjectCreateClient projectCreateClient;
  private final EmployeeStatusClient employeeStatusClient;

  public ApprovalController(
      ApprovalService approvalService,
      ProjectCreateClient projectCreateClient,
      EmployeeStatusClient employeeStatusClient) {
    this.approvalService = approvalService;
    this.projectCreateClient = projectCreateClient;
    this.employeeStatusClient = employeeStatusClient;
  }

  @GetMapping
  public ResponseEntity<List<ApprovalRequest>> list(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String status) {
    return ResponseEntity.ok(
        approvalService.list(
            requireTenant(tenantHeader), parseUserId(userHeader), role, type, status));
  }

  @GetMapping("/counts")
  public ResponseEntity<Map<String, Long>> counts(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader) {
    return ResponseEntity.ok(
        approvalService.counts(requireTenant(tenantHeader), parseUserId(userHeader)));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApprovalRequest> get(
      @PathVariable Long id,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    return ResponseEntity.ok(approvalService.get(id, requireTenant(tenantHeader)));
  }

  @PostMapping
  public ResponseEntity<ApprovalRequest> create(
      @Valid @RequestBody CreateApprovalRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    Long tid = requireTenant(tenantHeader);
    Long requesterId = parseUserId(userHeader);
    if (requesterId == null) {
      throw new ApprovalForbiddenException("X-User-Id 缺失，无法识别发起人");
    }
    return ResponseEntity.ok(
        approvalService.create(
            tid, requesterId, body, clientIp(req), userAgent(req), req.getHeader("X-Request-Id")));
  }

  @GetMapping("/audit-logs")
  public ResponseEntity<List<ApprovalAuditLog>> auditLogs(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestParam(defaultValue = "100") int limit) {
    return ResponseEntity.ok(approvalService.auditLogs(requireTenant(tenantHeader), limit));
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<ApprovalRequest> approve(
      @PathVariable Long id,
      @Valid @RequestBody(required = false) DecideRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    ApprovalRequest approved =
        approvalService.approve(
            id,
            requireTenant(tenantHeader),
            requireUserId(userHeader),
            body,
            clientIp(req),
            userAgent(req),
            req.getHeader("X-Request-Id"));
    // 立项审批通过 → 自动创建项目（幂等：项目名+子公司匹配；失败仅日志，不阻断审批）
    maybeCreateProjectFromProposal(approved, tenantHeader, userHeader);
    // HR 入转调离审批通过 → 自动更新员工档案状态（B3；失败仅日志）
    maybeApplyHrLifecycle(approved, tenantHeader, userHeader);
    return ResponseEntity.ok(approved);
  }

  /**
   * HR 入转调离类审批（title 以【HR 入转调离】开头且 detail 含 employeeId/action）通过后自动更新员工状态。 动作映射：入职→PROBATION /
   * 转正→ACTIVE / 调岗→TRANSFERRED / 离职→RESIGNED。失败降级为日志（审批已成功，HR 可在员工页手动流转）。
   */
  private void maybeApplyHrLifecycle(
      ApprovalRequest approved, String tenantHeader, String userHeader) {
    if (approved == null
        || approved.getTitle() == null
        || !approved.getTitle().startsWith("【HR 入转调离】")) {
      return;
    }
    if (approved.getDetail() == null || approved.getDetail().isBlank()) return;
    try {
      var detail = parseDetail(approved.getDetail());
      Object empIdRaw = detail.get("employeeId");
      String action = str(detail.get("action"));
      if (empIdRaw == null || action == null || action.isBlank()) {
        log.warn("[hr-lifecycle] detail 缺 employeeId/action，跳过 id={}", approved.getId());
        return;
      }
      Long empId = Long.valueOf(String.valueOf(empIdRaw));
      String status = switch (action) {
        case "入职" -> "PROBATION";
        case "转正" -> "ACTIVE";
        case "调岗" -> "TRANSFERRED";
        case "离职" -> "RESIGNED";
        default -> null;
      };
      if (status == null) {
        log.warn("[hr-lifecycle] 未知动作 {}，跳过 id={}", action, approved.getId());
        return;
      }
      String remark =
          "HR审批#" + approved.getId() + " " + action + "：" + str(detail.get("reason"));
      employeeStatusClient.changeStatus(
          empId, new EmployeeStatusClient.StatusChangeRequest(status, remark), tenantHeader, userHeader);
      log.info(
          "[hr-lifecycle] 员工 {} 状态 → {}（审批#{} {})",
          empId, status, approved.getId(), action);
    } catch (Exception e) {
      log.warn("[hr-lifecycle] 更新员工状态失败（不阻断审批）id={}: {}", approved.getId(), e.getMessage());
    }
  }

  private void maybeCreateProjectFromProposal(
      ApprovalRequest approved, String tenantHeader, String userHeader) {
    if (approved == null
        || approved.getTitle() == null
        || !approved.getTitle().startsWith("【立项】")) {
      return;
    }
    if (approved.getDetail() == null || approved.getDetail().isBlank()) return;
    try {
      var detail = parseDetail(approved.getDetail());
      String projectName = str(detail.get("projectName"));
      Object orgIdRaw = detail.get("orgId");
      if (projectName == null || projectName.isBlank() || orgIdRaw == null) {
        log.warn("[auto-project] 立项 detail 缺 projectName/orgId，跳过自动建项目 id={}", approved.getId());
        return;
      }
      Long orgId = Long.valueOf(String.valueOf(orgIdRaw));
      String code =
          "P" + (approved.getId() != null ? approved.getId() : System.currentTimeMillis());
      Object budgetRaw = detail.get("budget");
      BigDecimal budget = budgetRaw == null ? null : new BigDecimal(String.valueOf(budgetRaw));
      var reqBody =
          new ProjectCreateClient.CreateProjectRequest(
              code,
              projectName,
              orgId,
              null,
              "ACTIVE",
              null,
              null,
              budget,
              null,
              proposalSummary(detail));
      projectCreateClient.create(reqBody, tenantHeader, userHeader);
      log.info("[auto-project] 立项审批通过自动建项目 id={} project={}", approved.getId(), projectName);
    } catch (Exception e) {
      // 项目服务不可用 / 契约不匹配 / 已存在等：降级，不影响审批结果
      log.warn("[auto-project] 自动建项目失败（可手动一键建） id={} err={}", approved.getId(), e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> parseDetail(String detail) {
    try {
      var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(detail);
      if (node != null && node.isObject()) {
        return new com.fasterxml.jackson.databind.ObjectMapper().convertValue(node, Map.class);
      }
    } catch (Exception ignored) {
      /* 非 JSON detail */
    }
    return Map.of();
  }

  private static String str(Object v) {
    return v == null ? null : String.valueOf(v);
  }

  /** 汇总文本（对齐前端 proposalSummary：背景/目标/范围/里程碑） */
  private static String proposalSummary(Map<String, Object> d) {
    StringBuilder sb = new StringBuilder();
    String[] keys = {"background", "objectives", "scope", "milestones"};
    String[] labels = {"背景", "目标", "范围", "里程碑"};
    for (int i = 0; i < keys.length; i++) {
      String v = str(d.get(keys[i]));
      if (v != null && !v.isBlank()) {
        if (sb.length() > 0) sb.append("\n");
        sb.append("【").append(labels[i]).append("】").append(v);
      }
    }
    return sb.toString();
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<ApprovalRequest> reject(
      @PathVariable Long id,
      @Valid @RequestBody RejectRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    return ResponseEntity.ok(
        approvalService.reject(
            id,
            requireTenant(tenantHeader),
            requireUserId(userHeader),
            body,
            clientIp(req),
            userAgent(req),
            req.getHeader("X-Request-Id")));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<ApprovalRequest> cancel(
      @PathVariable Long id,
      @Valid @RequestBody(required = false) DecideRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader,
      @RequestHeader(value = HDR_USER_ID, required = false) String userHeader,
      HttpServletRequest req) {
    return ResponseEntity.ok(
        approvalService.cancel(
            id,
            requireTenant(tenantHeader),
            requireUserId(userHeader),
            body,
            clientIp(req),
            userAgent(req),
            req.getHeader("X-Request-Id")));
  }

  // ============ web 辅助 ============

  private Long requireTenant(String header) {
    if (header == null || header.isBlank()) {
      throw new TenantContextRequiredException();
    }
    try {
      return Long.parseLong(header);
    } catch (NumberFormatException e) {
      throw new TenantContextRequiredException();
    }
  }

  private Long requireUserId(String header) {
    Long userId = parseUserId(header);
    if (userId == null) {
      throw new ApprovalForbiddenException("X-User-Id 缺失");
    }
    return userId;
  }

  private Long parseUserId(String header) {
    if (header == null || header.isBlank()) return null;
    try {
      return Long.parseLong(header);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String clientIp(HttpServletRequest req) {
    String xff = req.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
    return req.getRemoteAddr();
  }

  private String userAgent(HttpServletRequest req) {
    String ua = req.getHeader("User-Agent");
    return (ua == null || ua.isBlank()) ? null : (ua.length() > 255 ? ua.substring(0, 255) : ua);
  }
}
