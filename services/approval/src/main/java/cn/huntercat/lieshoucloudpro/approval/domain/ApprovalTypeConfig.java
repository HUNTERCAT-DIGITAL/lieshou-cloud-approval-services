package cn.huntercat.lieshoucloudpro.approval.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 审批流程类型配置（R5.2 · 可配置流程类型与表单字段）.
 *
 * <p>type 定义元数据：编码/名称/说明/基础分类（映射 {@code ApprovalRequest.Type}）/表单字段模板
 * （JSON：key、label、type、required、options）。发起审批时可按配置类型动态渲染表单（客户端增强）。 数据带 tenant_id 行级隔离（ADR-0025 模式）。
 */
@Entity
@Table(name = "approval_type_configs")
@Schema(description = "审批流程类型配置（R5.2）")
public class ApprovalTypeConfig {

  /** 表单字段类型 */
  public enum FieldType {
    INPUT,
    NUMBER,
    DATE,
    SELECT,
    TEXTAREA
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Schema(description = "Auto-assigned primary key", accessMode = Schema.AccessMode.READ_ONLY)
  private Long id;

  @Column(name = "tenant_id", nullable = false)
  @Schema(description = "Owning tenant", requiredMode = Schema.RequiredMode.REQUIRED)
  private Long tenantId;

  @Column(nullable = false, length = 32)
  @Schema(
      description = "流程类型编码（租户内唯一）",
      example = "expense-review",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String code;

  @Column(nullable = false, length = 64)
  @Schema(description = "流程名称", example = "差旅报销", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Column(length = 255)
  @Schema(description = "流程说明")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  @Schema(
      description = "映射基础审批类型（EXPENSE / PURCHASE / SALE / OTHER）",
      example = "EXPENSE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private cn.huntercat.lieshou.framework.approval.domain.ApprovalRequest.Type category;

  @Column(columnDefinition = "TEXT")
  @Schema(description = "表单字段定义 JSON：[{key,label,type,required,options}]")
  private String fields;

  @Column(nullable = false)
  @Schema(description = "启用状态（false = 停用）")
  private boolean enabled = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Schema(description = "Create timestamp", accessMode = Schema.AccessMode.READ_ONLY)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  @Schema(description = "Update timestamp", accessMode = Schema.AccessMode.READ_ONLY)
  private Instant updatedAt;

  public ApprovalTypeConfig() {}

  public Long getId() {
    return id;
  }

  public Long getTenantId() {
    return tenantId;
  }

  public void setTenantId(Long tenantId) {
    this.tenantId = tenantId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public cn.huntercat.lieshou.framework.approval.domain.ApprovalRequest.Type getCategory() {
    return category;
  }

  public void setCategory(
      cn.huntercat.lieshou.framework.approval.domain.ApprovalRequest.Type category) {
    this.category = category;
  }

  public String getFields() {
    return fields;
  }

  public void setFields(String fields) {
    this.fields = fields;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @jakarta.persistence.PrePersist
  void onCreate() {
    if (createdAt == null) createdAt = Instant.now();
    if (updatedAt == null) updatedAt = Instant.now();
  }

  @jakarta.persistence.PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
