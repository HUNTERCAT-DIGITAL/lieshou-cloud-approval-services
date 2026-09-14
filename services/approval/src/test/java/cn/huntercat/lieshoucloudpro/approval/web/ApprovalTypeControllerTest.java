package cn.huntercat.lieshoucloudpro.approval.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.huntercat.lieshoucloudpro.approval.PostgresTestSupport;

/**
 * 审批流程类型配置端点集成测试（R5.2 · 强制租户 + CRUD + 租户隔离）.
 *
 * <p>覆盖：无租户 401 / 创建与列表 / 编码唯一冲突 / 更新字段 / 停用 / 跨租户不可见。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApprovalTypeControllerTest extends PostgresTestSupport {

  @Autowired MockMvc mockMvc;

  private static final String CREATE_BODY =
      "{\"code\":\"expense-review\",\"name\":\"差旅报销\",\"category\":\"EXPENSE\","
          + "\"description\":\"出差费用报销\","
          + "\"fields\":\"[{\\\"key\\\":\\\"amount\\\",\\\"label\\\":\\\"金额\\\",\\\"type\\\":\\\"NUMBER\\\",\\\"required\\\":true}]\"}";

  @Test
  void createWithoutTenant_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/approval-types")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("TENANT_CONTEXT_REQUIRED"));
  }

  @Test
  void createAndList_tenantScoped() throws Exception {
    mockMvc
        .perform(
            post("/api/approval-types")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("expense-review"))
        .andExpect(jsonPath("$.name").value("差旅报销"))
        .andExpect(jsonPath("$.enabled").value(true));

    mockMvc
        .perform(get("/api/approval-types").header("X-Tenant-Id", "1").header("X-User-Id", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("expense-review"))
        .andExpect(jsonPath("$[0].category").value("EXPENSE"));
  }

  @Test
  void duplicateCode_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/approval-types")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/approval-types")
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("TYPE_CODE_EXISTS"));
  }

  @Test
  void updateType_changesFieldsAndEnabled() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/approval-types")
                    .header("X-Tenant-Id", "1")
                    .header("X-User-Id", "9")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CREATE_BODY))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asLong();

    mockMvc
        .perform(
            put("/api/approval-types/" + id)
                .header("X-Tenant-Id", "1")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"expense-review\",\"name\":\"差旅报销（新）\",\"category\":\"EXPENSE\","
                        + "\"fields\":\"[]\",\"enabled\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("差旅报销（新）"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void disableType_setsEnabledFalse() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/approval-types")
                    .header("X-Tenant-Id", "1")
                    .header("X-User-Id", "9")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CREATE_BODY))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asLong();

    mockMvc
        .perform(
            delete("/api/approval-types/" + id).header("X-Tenant-Id", "1").header("X-User-Id", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));
  }

  @Test
  void crossTenantIsolation_invisibleAndNotFound() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/api/approval-types")
                    .header("X-Tenant-Id", "1")
                    .header("X-User-Id", "9")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(CREATE_BODY))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id =
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asLong();

    // 租户 3（无 seed 数据）列表不可见 —— 租户 2 有 V4 seed，不能拿来断言“空”
    mockMvc
        .perform(get("/api/approval-types").header("X-Tenant-Id", "3").header("X-User-Id", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    // 租户 3 无法更新租户 1 刚建的类型
    mockMvc
        .perform(
            put("/api/approval-types/" + id)
                .header("X-Tenant-Id", "3")
                .header("X-User-Id", "9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
        .andExpect(status().isNotFound());
  }
}
