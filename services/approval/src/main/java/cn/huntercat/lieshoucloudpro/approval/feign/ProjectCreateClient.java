package cn.huntercat.lieshoucloudpro.approval.feign;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.cloud.openfeign.FeignClient;

import java.math.BigDecimal;

/**
 * approval-service → project-service 创建项目（立项审批通过后自动建项目）。
 *
 * <p>仅当存在 project 服务（lieShouCloud-project 注册于 Nacos）时可用；调用失败由调用方 捕获降级（不阻断审批）。立项 detail JSON 含
 * orgId/projectName 时触发。
 */
@FeignClient(name = "lieshoucloud-project", path = "/api/projects")
public interface ProjectCreateClient {

  record CreateProjectRequest(
      String code,
      String name,
      Long orgId,
      String type,
      String status,
      String startDate,
      String endDate,
      BigDecimal budget,
      Long managerId,
      String description) {}

  @PostMapping
  Object create(
      @RequestBody CreateProjectRequest body,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
      @RequestHeader(value = "X-User-Id", required = false) String userId);
}
