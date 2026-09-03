package com.pipker.server;

import com.pipker.business.common.api.ApiCode;
import com.pipker.business.common.api.ApiResponse;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.server.error.ApiBusinessException;
import com.pipker.starter.security.service.SecurityCryptoService;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PipkerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SecurityCryptoService securityCryptoService;

    @Autowired
    private SpringLiquibase springLiquibase;

    @Test
    void liquibaseCreatesOnlyFrameworkSystemTablesAndExecutesChangesetsOnce() throws Exception {
        Set<String> tableNames = jdbcTemplate.queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE LOWER(table_name) LIKE 'system_%'",
                        String.class
                ).stream()
                .map(tableName -> tableName.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        assertThat(tableNames).containsExactlyInAnyOrder(
                "system_user",
                "system_role",
                "system_user_role",
                "system_permission",
                "system_role_permission",
                "system_menu",
                "system_role_menu"
        );
        assertThat(tableNames).allMatch(name -> name.startsWith("system_"));
        int executedChangesets = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class);

        springLiquibase.afterPropertiesSet();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class))
                .isEqualTo(executedChangesets);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_user WHERE username = 'admin'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void apiResponseUsesCommonDefaultsAndAcceptsCustomApiCodeEnums() {
        assertThat(ApiResponse.success("payload")).isEqualTo(
                new ApiResponse<>(CommonApiCode.SUCCESS.getCode(), "payload", CommonApiCode.SUCCESS.getMessage())
        );
        assertThat(ApiResponse.success(TestApiCode.CREATED, "payload")).isEqualTo(
                new ApiResponse<>(TestApiCode.CREATED.getCode(), "payload", TestApiCode.CREATED.getMessage())
        );
        assertThat(ApiResponse.failure(TestApiCode.NOT_FOUND)).isEqualTo(
                new ApiResponse<>(TestApiCode.NOT_FOUND.getCode(), null, TestApiCode.NOT_FOUND.getMessage())
        );
        assertThat(ApiResponse.failure(TestApiCode.NOT_FOUND, "System user 42 was not found.")).isEqualTo(
                new ApiResponse<>(TestApiCode.NOT_FOUND.getCode(), null, "System user 42 was not found.")
        );
        assertThat(new ApiBusinessException(TestApiCode.NOT_FOUND))
                .hasMessage(TestApiCode.NOT_FOUND.getMessage());
    }

    @Test
    void seededAdminPasswordIsHashedAndCanLogIn() throws Exception {
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM system_user WHERE username = 'admin'", String.class
        );
        assertThat(storedHash).isNotEqualTo("admin123");
        assertThat(securityCryptoService.matchesPassword("admin123", storedHash)).isTrue();

        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.menus[0].children[0].componentKey").value("system/overview/index"));

        mockMvc.perform(get("/api/admin/authorization").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()));
    }

    @Test
    void invalidPasswordAndUnauthenticatedAdminRequestUseUnifiedApiCodes() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_INVALID_CREDENTIALS.getCode()));

        mockMvc.perform(get("/api/admin/authorization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_REQUIRED.getCode()));
    }

    @Test
    void malformedLoginRequestUsesUnifiedValidationCode() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    void disabledAccountCannotCreateSystemSession() throws Exception {
        insertUser("disabled", "disabled-password", "DISABLED");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"disabled\",\"password\":\"disabled-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_ACCOUNT_DISABLED.getCode()));
    }

    @Test
    void multipleRolesAreReportedAndRoleWithoutPermissionCannotReadAdminAuthorization() throws Exception {
        insertUser("ordinary", "ordinary-password");
        insertRole("NO_PERMISSION", 300);
        jdbcTemplate.update("""
                INSERT INTO system_user_role (user_id, role_id)
                SELECT u.id, r.id FROM system_user u, system_role r
                WHERE u.username = 'ordinary' AND r.role_code = 'NO_PERMISSION'
                """);

        String ordinaryToken = login("ordinary", "ordinary-password");
        mockMvc.perform(get("/api/admin/authorization").header(AUTHORIZATION, "Bearer " + ordinaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_FORBIDDEN.getCode()));

        jdbcTemplate.update("""
                INSERT INTO system_role_permission (role_id, permission_id)
                SELECT r.id, p.id FROM system_role r, system_permission p
                WHERE r.role_code = 'NO_PERMISSION' AND p.permission_code = 'system:authorization:read'
                """);
        mockMvc.perform(get("/api/admin/authorization").header(AUTHORIZATION, "Bearer " + ordinaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()));

        insertUser("multi", "multi-password");
        insertRole("SECONDARY", 310);
        jdbcTemplate.update("""
                INSERT INTO system_user_role (user_id, role_id)
                SELECT u.id, r.id FROM system_user u, system_role r
                WHERE u.username = 'multi' AND r.role_code IN ('ADMIN', 'SECONDARY')
                """);

        String multiToken = login("multi", "multi-password");
        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + multiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.roles.length()").value(2))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.menus[0].name").value("系统管理"));
    }

    @Test
    void pingUsesTheUnifiedEnvelopeAndDisabledRouteManifestIsNotRegistered() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value("Pipker Server is running."));

        mockMvc.perform(get("/api/_dev/routes"))
                .andExpect(status().isNotFound());
    }

    @SuppressWarnings("unchecked")
    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andReturn();
        Map<String, Object> response = new tools.jackson.databind.json.JsonMapper().readValue(
                result.getResponse().getContentAsString(), Map.class
        );
        Map<?, ?> data = (Map<?, ?>) response.get("data");
        return String.valueOf(data.get("accessToken"));
    }

    private void insertUser(String username, String rawPassword) {
        insertUser(username, rawPassword, "ENABLED");
    }

    private void insertUser(String username, String rawPassword, String status) {
        jdbcTemplate.update("""
                        INSERT INTO system_user (username, password_hash, nickname, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                username,
                securityCryptoService.hashPassword(rawPassword),
                username,
                status
        );
    }

    private void insertRole(String roleCode, int sort) {
        jdbcTemplate.update("""
                        INSERT INTO system_role (role_code, role_name, status, sort, created_at, updated_at)
                        VALUES (?, ?, 'ENABLED', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                roleCode,
                roleCode,
                sort
        );
    }

    private enum TestApiCode implements ApiCode {
        CREATED(201, "System user created."),
        NOT_FOUND(404, "System user was not found.");

        private final int code;
        private final String message;

        TestApiCode(int code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public int getCode() {
            return code;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }
}
