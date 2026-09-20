/**
 * @file AuthenticationLifecycleAuditIntegrationTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 在真实 Spring MVC、Sa-Token 和 SQLite 链路中验证认证生命周期审计与当前令牌注销。
 * @logic 通过错误与成功登录、双令牌会话、当前授权检查、账户失效和管理员查询串联验证每个请求至多写入一条安全日志。
 * @dependencies Spring Boot Test、MockMvc、Sa-Token、Liquibase、SQLite、MyBatis-Plus
 * @index_tags integration-test、authentication、audit、login-log、logout、sa-token、sqlite
 * @author holic512
 */
package com.pipker.server;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jayway.jsonpath.JsonPath;
import com.pipker.business.api.common.mapper.SystemLoginLogMapper;
import com.pipker.business.api.common.mapper.SystemRoleMenuMapper;
import com.pipker.business.api.common.mapper.SystemRolePermissionMapper;
import com.pipker.business.api.common.mapper.SystemUserMapper;
import com.pipker.business.api.common.model.SystemLoginLog;
import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.system.authorization.SystemAuthorizationCache;
import com.pipker.business.api.system.authorization.SystemAuthorizationService;
import com.pipker.business.api.system.permission.PermissionEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 完整认证生命周期审计集成测试。 */
@SpringBootTest(classes = PipkerApplication.class)
@AutoConfigureMockMvc
class AuthenticationLifecycleAuditIntegrationTests {

    private static final long ADMIN_USER_ID = 2090000000000000001L;
    private static final long ADMIN_ROLE_ID = 2090000000000000102L;
    private static final long LOGIN_LOG_MENU_ID = 2090000000000000307L;
    private static final Path DATA_ROOT = createTemporaryDataRoot();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SystemLoginLogMapper systemLoginLogMapper;

    @Autowired
    private SystemUserMapper systemUserMapper;

    @Autowired
    private SystemRolePermissionMapper systemRolePermissionMapper;

    @Autowired
    private SystemRoleMenuMapper systemRoleMenuMapper;

    @Autowired
    private SystemAuthorizationService systemAuthorizationService;

    @Autowired
    private SystemAuthorizationCache systemAuthorizationCache;

    @DynamicPropertySource
    static void configureTemporaryStorage(DynamicPropertyRegistry registry) {
        registry.add("pipker.data.database-root", DATA_ROOT::toString);
        registry.add("pipker.data.log-root", () -> DATA_ROOT.resolve("log").toString());
        registry.add("pipker.file.local.root", () -> DATA_ROOT.resolve("file").toString());
        registry.add("pipker.file.enabled", () -> "false");
    }

    @Test
    void persistsExactlyOneSafeEventPerAuthenticationLifecycleRequest() throws Exception {
        assertDefaultLoginLogAuthorizationSeeds();
        assertThat(systemAuthorizationService.findSnapshot(ADMIN_USER_ID).permissions())
                .contains(PermissionEnum.SYSTEM_LOGIN_LOG_VIEW.getCode());

        SystemUser administrator = systemUserMapper.selectById(ADMIN_USER_ID);
        SystemUser disabledUser = testUser("disabled-audit-user", administrator.getPasswordHash(), "DISABLED");
        SystemUser unprivilegedUser = testUser("unprivileged-audit-user", administrator.getPasswordHash(), "ENABLED");

        login("admin", "incorrect-password", "198.51.100.10", 401);
        login("unknown-audit-user", "incorrect-password", "198.51.100.11", 401);
        login(disabledUser.getUsername(), "admin123", "198.51.100.12", 403);
        mockMvc.perform(post("/api/auth/login")
                        .remoteAddress("198.51.100.13")
                        .header("User-Agent", "authentication-audit-integration-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(post("/api/auth/login")
                        .remoteAddress("198.51.100.14")
                        .header("User-Agent", "authentication-audit-integration-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        String tokenA = login("admin", "admin123", "198.51.100.20", 200);
        String tokenB = login("admin", "admin123", "198.51.100.21", 200);
        String unprivilegedToken = login(unprivilegedUser.getUsername(), "admin123", "198.51.100.22", 200);
        assertThat(systemUserMapper.selectById(ADMIN_USER_ID).getLastLoginTime()).isNotNull();

        authorizedGet("/api/auth/me", unprivilegedToken, 403);
        int eventCountBeforeUnrelatedPermissionFailure = systemLoginLogMapper.selectCount(null).intValue();
        authorizedGet("/api/admin/login-logs", unprivilegedToken, 403);
        assertThat(systemLoginLogMapper.selectCount(null)).isEqualTo(eventCountBeforeUnrelatedPermissionFailure);

        unprivilegedUser.setStatus("DISABLED");
        systemUserMapper.updateById(unprivilegedUser);
        systemAuthorizationCache.invalidateSnapshot(unprivilegedUser.getId());
        authorizedGet("/api/auth/me", unprivilegedToken, 403);

        authorizedGet("/api/auth/me", tokenA, 200);
        authorizedPost("/api/auth/logout", tokenA, 200)
                .andExpect(jsonPath("$.data.loggedOut").value(true));
        authorizedGet("/api/auth/me", tokenA, 401);
        authorizedGet("/api/auth/me", tokenB, 200);
        authorizedPost("/api/auth/logout", "invalid-current-token", 401);

        authorizedGet("/api/admin/login-logs?eventType=UNKNOWN", tokenB, 400);
        authorizedGet("/api/admin/login-logs?startTime=not-a-time", tokenB, 400);
        mockMvc.perform(get("/api/admin/login-logs")
                        .header("Authorization", bearer(tokenB))
                        .param("page", "1")
                        .param("pageSize", "100")
                        .param("keyword", "198.51.100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records.length()").value(15));

        List<SystemLoginLog> logs = allLogs();
        assertThat(logs).hasSize(15);
        assertThat(logs)
                .extracting(SystemLoginLog::getEventType, SystemLoginLog::getResult,
                        SystemLoginLog::getFailureReason)
                .containsOnlyOnce(tuple("AUTH_CHECK", "FAILURE", "AUTH_FORBIDDEN"))
                .containsOnlyOnce(tuple("AUTH_CHECK", "FAILURE", "ACCOUNT_DISABLED"))
                .containsOnlyOnce(tuple("LOGOUT", "SUCCESS", null))
                .containsOnlyOnce(tuple("LOGOUT", "FAILURE", "AUTH_REQUIRED"));
        assertThat(logs).filteredOn(log -> "LOGIN".equals(log.getEventType()))
                .hasSize(8);
        assertThat(logs).filteredOn(log -> "AUTH_CHECK".equals(log.getEventType()))
                .hasSize(5);
        assertThat(logs).filteredOn(log -> "LOGOUT".equals(log.getEventType()))
                .hasSize(2);
        assertThat(logs).filteredOn(log -> "VALIDATION_FAILED".equals(log.getFailureReason()))
                .hasSize(2);
        assertThat(logs).filteredOn(log -> "INVALID_CREDENTIALS".equals(log.getFailureReason()))
                .hasSize(2);

        SystemLoginLog wrongPassword = logs.stream()
                .filter(log -> "admin".equals(log.getUsername()))
                .filter(log -> "INVALID_CREDENTIALS".equals(log.getFailureReason()))
                .findFirst()
                .orElseThrow();
        assertThat(wrongPassword.getClientIp()).isEqualTo("198.51.100.10");
        assertThat(wrongPassword.getUserAgent()).isEqualTo("authentication-audit-integration-test");
        assertThat(wrongPassword.getHttpMethod()).isEqualTo("POST");
        assertThat(wrongPassword.getRequestPath()).isEqualTo("/api/auth/login");
        assertThat(wrongPassword.getTraceId()).isNotBlank();

        assertThat(logs).allSatisfy(log -> {
            assertThat(secretSafeText(log)).doesNotContain(
                    "admin123", "incorrect-password", tokenA, tokenB,
                    unprivilegedToken, "invalid-current-token", "Authorization", "Bearer "
            );
        });
    }

    private void assertDefaultLoginLogAuthorizationSeeds() {
        assertThat(systemRolePermissionMapper.selectById(2090000000000000606L)).satisfies(relation -> {
            assertThat(relation.getRoleId()).isEqualTo(ADMIN_ROLE_ID);
            assertThat(relation.getPermissionCode()).isEqualTo(PermissionEnum.SYSTEM_LOGIN_LOG_VIEW.getCode());
        });
        assertThat(systemRoleMenuMapper.selectById(2090000000000000706L)).satisfies(relation -> {
            assertThat(relation.getRoleId()).isEqualTo(ADMIN_ROLE_ID);
            assertThat(relation.getMenuId()).isEqualTo(LOGIN_LOG_MENU_ID);
        });
    }

    private SystemUser testUser(String username, String passwordHash, String status) {
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setNickname(username);
        user.setStatus(status);
        assertThat(systemUserMapper.insert(user)).isEqualTo(1);
        return user;
    }

    private String login(String username, String password, String remoteAddress, int expectedCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .remoteAddress(remoteAddress)
                        .header("User-Agent", "authentication-audit-integration-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andReturn();
        return expectedCode == 200
                ? JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken")
                : null;
    }

    private org.springframework.test.web.servlet.ResultActions authorizedGet(
            String path,
            String token,
            int expectedCode
    ) throws Exception {
        return mockMvc.perform(get(path)
                        .remoteAddress("198.51.100.30")
                        .header("User-Agent", "authentication-audit-integration-test")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private org.springframework.test.web.servlet.ResultActions authorizedPost(
            String path,
            String token,
            int expectedCode
    ) throws Exception {
        return mockMvc.perform(post(path)
                        .remoteAddress("198.51.100.31")
                        .header("User-Agent", "authentication-audit-integration-test")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private List<SystemLoginLog> allLogs() {
        return systemLoginLogMapper.selectList(new LambdaQueryWrapper<SystemLoginLog>()
                .orderByAsc(SystemLoginLog::getOccurredAt)
                .orderByAsc(SystemLoginLog::getId));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String secretSafeText(SystemLoginLog log) {
        return String.join("|",
                String.valueOf(log.getUserId()),
                String.valueOf(log.getUsername()),
                String.valueOf(log.getEventType()),
                String.valueOf(log.getResult()),
                String.valueOf(log.getFailureReason()),
                String.valueOf(log.getClientIp()),
                String.valueOf(log.getUserAgent()),
                String.valueOf(log.getHttpMethod()),
                String.valueOf(log.getRequestPath()),
                String.valueOf(log.getTraceId()),
                String.valueOf(log.getOccurredAt())
        );
    }

    private static Path createTemporaryDataRoot() {
        try {
            return Files.createTempDirectory("pipker-auth-audit-integration-");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create authentication audit integration data root", exception);
        }
    }
}
