package cn.huntercat.lieshoucloudpro.approval.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** 审批流程类型配置 Repository（租户内强制过滤） */
public interface ApprovalTypeConfigRepository extends JpaRepository<ApprovalTypeConfig, Long> {

  List<ApprovalTypeConfig> findByTenantIdOrderByIdAsc(Long tenantId);

  boolean existsByTenantIdAndCode(Long tenantId, String code);
}
