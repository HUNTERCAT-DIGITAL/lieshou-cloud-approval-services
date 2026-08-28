package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import cn.huntercat.lieshou.framework.approval.domain.ApprovalRequest;
import cn.huntercat.lieshou.framework.common.api.TenantContextRequiredException;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalTypeConfig;
import cn.huntercat.lieshoucloudpro.approval.domain.ApprovalTypeConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 审批流程类型配置（R5.2 · 类型/表单字段可配置）.
 *
 * <p>tenant_id 强制过滤（gateway 注入 X-Tenant-Id），缺失/非法 → 401（ADR-0025 模式）。
 */
@Tag(name = "Approval · Type Config", description = "审批流程类型配置（R5.2）")
@RestController
@RequestMapping("/api/approval-types")
public class ApprovalTypeController {

  private final ApprovalTypeConfigRepository repo;

  public ApprovalTypeController(ApprovalTypeConfigRepository repo) {
    this.repo = repo;
  }

  /** 创建/更新请求体 */
  private static final String HDR_TENANT_ID = "X-Tenant-Id";

  public record TypeRequest(
      @NotBlank String code,
      @NotBlank String name,
      String description,
      @NotNull ApprovalRequest.Type category,
      String fields,
      Boolean enabled) {}

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

  @Operation(summary = "流程类型列表（租户内）")
  @GetMapping
  public ResponseEntity<List<ApprovalTypeConfig>> list(
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    return ResponseEntity.ok(repo.findByTenantIdOrderByIdAsc(tid));
  }

  @Operation(summary = "新增流程类型")
  @PostMapping
  public ResponseEntity<?> create(
      @Valid @RequestBody TypeRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    if (repo.existsByTenantIdAndCode(tid, body.code())) {
      return ResponseEntity.badRequest().body(Map.of("error", "TYPE_CODE_EXISTS"));
    }
    ApprovalTypeConfig t = new ApprovalTypeConfig();
    t.setTenantId(tid);
    t.setCode(body.code());
    t.setName(body.name());
    t.setDescription(body.description());
    t.setCategory(body.category());
    t.setFields(body.fields());
    t.setEnabled(body.enabled() == null || body.enabled());
    t.setCreatedAt(Instant.now());
    t.setUpdatedAt(Instant.now());
    return ResponseEntity.ok(repo.save(t));
  }

  @Operation(summary = "更新流程类型（名称/说明/字段/启用状态）")
  @PutMapping("/{id}")
  public ResponseEntity<?> update(
      @PathVariable Long id,
      @Valid @RequestBody TypeRequest body,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    return repo.findById(id)
        .filter(t -> t.getTenantId().equals(tid))
        .map(
            t -> {
              t.setName(body.name());
              t.setDescription(body.description());
              if (body.fields() != null) t.setFields(body.fields());
              if (body.enabled() != null) t.setEnabled(body.enabled());
              t.setUpdatedAt(Instant.now());
              return ResponseEntity.ok(repo.save(t));
            })
        .orElse(ResponseEntity.notFound().build());
  }

  @Operation(summary = "停用流程类型（enabled=false）")
  @DeleteMapping("/{id}")
  public ResponseEntity<?> disable(
      @PathVariable Long id,
      @RequestHeader(value = HDR_TENANT_ID, required = false) String tenantHeader) {
    Long tid = requireTenant(tenantHeader);
    return repo.findById(id)
        .filter(t -> t.getTenantId().equals(tid))
        .map(
            t -> {
              t.setEnabled(false);
              t.setUpdatedAt(Instant.now());
              return ResponseEntity.ok(repo.save(t));
            })
        .orElse(ResponseEntity.notFound().build());
  }
}
