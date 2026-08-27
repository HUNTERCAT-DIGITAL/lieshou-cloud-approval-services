package cn.huntercat.lieshoucloudpro.approval.feign;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import cn.huntercat.lieshou.framework.approval.port.UserQueryPort;
import cn.huntercat.lieshou.framework.approval.port.UserView;

/**
 * approval-service → user-service 查询（薄壳 Feign 装配）。
 *
 * <p>业务契约在 {@link UserQueryPort}（LieShou-framework，上游同源唯一）；
 * 本接口仅补 Feign HTTP 映射，由 OpenFeign 从 Nacos 发现 user-service。
 */
@FeignClient(name = "lieshoucloud-user", path = "/api/users")
public interface UserQueryClient extends UserQueryPort {

  @Override
  @GetMapping
  List<UserView> listTenantUsers(
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId);

  @Override
  @GetMapping("/{id}")
  UserView getUserById(
      @PathVariable Long id,
      @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId);
}
