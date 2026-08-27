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

import cn.huntercat.lieshou.framework.approval.ApprovalService;
import cn.huntercat.lieshou.framework.approval.domain.ApprovalAuditLog;
import cn.huntercat.lieshou.framework.approval.domain.ApprovalRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalDtos.CreateApprovalRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalDtos.DecideRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalDtos.RejectRequest;
import cn.huntercat.lieshou.framework.approval.dto.ApprovalForbiddenException;
import cn.huntercat.lieshou.framework.common.api.TenantContextRequiredException;
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

  private static final String HDR_TENANT_ID = "X-Tenant-Id";
  private static final String HDR_USER_ID = "X-User-Id";

  private final ApprovalService approvalService;

  public ApprovalController(ApprovalService approvalService) {
    this.approvalService = approvalService;
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
    return ResponseEntity.ok(
        approvalService.approve(
            id,
            requireTenant(tenantHeader),
            requireUserId(userHeader),
            body,
            clientIp(req),
            userAgent(req),
            req.getHeader("X-Request-Id")));
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
