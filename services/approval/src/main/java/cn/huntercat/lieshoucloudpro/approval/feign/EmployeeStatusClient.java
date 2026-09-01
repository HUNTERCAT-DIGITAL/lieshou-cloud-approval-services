package cn.huntercat.lieshoucloudpro.approval.feign;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * approval-service → project-service 员工状态流转（HR 入转调离审批通过后自动更新档案，B3）。
 *
 * <p>复用 ProjectCreateClient 的 Feign 注册（lieshoucloud-project）；调用失败由调用方捕获降级（不阻断审批）。
 * HR 审批 detail JSON 含 employeeId + action（入职/转正/调岗/离职）时触发。
 */
@FeignClient(name = "lieshoucloud-project", contextId = "employee-status-client", path = "/api/employees")
public interface EmployeeStatusClient {

  record StatusChangeRequest(String status, String remark) {}

  @PutMapping("/{id}/status")
  Object changeStatus(
      @PathVariable("id") Long employeeId,
      @RequestBody StatusChangeRequest body,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
      @RequestHeader(value = "X-User-Id", required = false) String userId);
}
