package com.pipker.server;

import com.pipker.business.common.api.ApiCode;
import com.pipker.business.common.api.ApiResponse;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.business.api.system.authorization.SystemAuthorizationCache;
import com.pipker.starter.security.service.SecurityCryptoService;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PipkerApplicationTests {

    private static final Path SQLITE_DATABASE = SqliteTestDatabase.create("pipker-application-");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SecurityCryptoService securityCryptoService;

    @Autowired
    private SpringLiquibase springLiquibase;

    @Autowired
    private SystemAuthorizationCache systemAuthorizationCache;

    @DynamicPropertySource
    static void sqliteProperties(DynamicPropertyRegistry registry) {
        SqliteTestDatabase.register(registry, SQLITE_DATABASE);
    }

    @AfterAll
    static void removeTemporaryDatabase() throws Exception {
        SqliteTestDatabase.delete(SQLITE_DATABASE);
    }

    @Test
    void liquibaseCreatesOnlyFrameworkSystemTablesAndExecutesChangesetsOnce() throws Exception {
        Set<String> tableNames = jdbcTemplate.queryForList(
                        "SELECT name FROM sqlite_master WHERE type = 'table' AND name LIKE 'system_%'",
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
                "system_role_menu",
                "system_api_resource"
        );
        assertThat(tableNames).allMatch(name -> name.startsWith("system_"));
        int executedChangesets = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class);

        springLiquibase.afterPropertiesSet();

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM databasechangelog", Integer.class))
                .isEqualTo(executedChangesets);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM system_user WHERE username = 'admin'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pragma_table_info('system_menu') WHERE name = 'permission_code'",
                Integer.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_permission WHERE permission_type = 'PAGE'",
                Integer.class
        )).isZero();
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
                .andExpect(jsonPath("$.data.menus[0].children[0].componentKey").value("system/overview/index"))
                .andExpect(jsonPath("$.data.menus[0].children[1].componentKey").value("system/role-menu/index"));

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
    void databaseApiAuthorizationRequiresAnExplicitPermissionAndRefreshesAfterLocalCacheInvalidation() throws Exception {
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
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_FORBIDDEN.getCode()));

        long ordinaryUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_user WHERE username = 'ordinary'", Long.class
        );
        systemAuthorizationCache.invalidateSnapshot(ordinaryUserId);
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
        jdbcTemplate.update("""
                INSERT INTO system_menu (parent_id, menu_name, menu_type, route_path, route_name, component_key, icon, sort, visible, status, created_at, updated_at)
                SELECT id, '第二角色页面', 'MENU', '/system/secondary-test', 'SystemSecondaryTest', 'system/overview/index', 'CopyDocument', 30, TRUE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM system_menu WHERE route_name = 'System'
                """);
        jdbcTemplate.update("""
                INSERT INTO system_role_menu (role_id, menu_id)
                SELECT r.id, m.id FROM system_role r, system_menu m
                WHERE r.role_code = 'SECONDARY' AND m.route_name = 'SystemSecondaryTest'
                """);

        String multiToken = login("multi", "multi-password");
        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + multiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.roles.length()").value(2))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.menus[0].name").value("系统管理"))
                .andExpect(jsonPath("$.data.menus[0].children.length()").value(2));
    }

    @Test
    void anonymousRoutesAreExplicitAndUnmappedProtectedRoutesAreForbidden() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value("Pipker Server is running."));

        String token = login("admin", "admin123");
        mockMvc.perform(get("/api/admin/not-configured").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_FORBIDDEN.getCode()));

        mockMvc.perform(post("/api/auth/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_FORBIDDEN.getCode()));
    }

    @Test
    void roleMenuConfigurationIsSuperAdminOnlyAndRefreshingItsAssignmentsInvalidatesLocalSnapshots() throws Exception {
        insertUser("route-reader", "route-reader-password");
        insertRole("ROUTE_READER", 320);
        jdbcTemplate.update("""
                INSERT INTO system_user_role (user_id, role_id)
                SELECT u.id, r.id FROM system_user u, system_role r
                WHERE u.username = 'route-reader' AND r.role_code = 'ROUTE_READER'
                """);
        jdbcTemplate.update("""
                INSERT INTO system_role_permission (role_id, permission_id)
                SELECT r.id, p.id FROM system_role r, system_permission p
                WHERE r.role_code = 'ROUTE_READER' AND p.permission_code = 'system:auth:me'
                """);

        String routeReaderToken = login("route-reader", "route-reader-password");
        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + routeReaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.menus").isEmpty());

        mockMvc.perform(get("/api/admin/role-menu-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_REQUIRED.getCode()));
        mockMvc.perform(get("/api/admin/role-menu-config").header(AUTHORIZATION, "Bearer " + routeReaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.AUTH_FORBIDDEN.getCode()));

        String superAdminToken = login("admin", "admin123");
        mockMvc.perform(get("/api/admin/role-menu-config").header(AUTHORIZATION, "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.roles[0].code").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.data.roles[0].allMenus").value(true));

        long routeReaderRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_role WHERE role_code = 'ROUTE_READER'", Long.class
        );
        long overviewMenuId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_menu WHERE route_name = 'SystemOverview'", Long.class
        );
        long superAdminRoleId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_role WHERE role_code = 'SUPER_ADMIN'", Long.class
        );

        mockMvc.perform(put("/api/admin/role-menu-config/{roleId}", superAdminRoleId)
                        .header(AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"menuIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.VALIDATION_FAILED.getCode()));
        mockMvc.perform(put("/api/admin/role-menu-config/{roleId}", routeReaderRoleId)
                        .header(AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"menuIds\":[" + overviewMenuId + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.menuIds[0]").value(overviewMenuId));

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + routeReaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.menus[0].name").value("系统管理"))
                .andExpect(jsonPath("$.data.menus[0].children[0].routeName").value("SystemOverview"));

        mockMvc.perform(put("/api/admin/role-menu-config/{roleId}", routeReaderRoleId)
                        .header(AUTHORIZATION, "Bearer " + superAdminToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"menuIds\":[999999]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.VALIDATION_FAILED.getCode()));
    }

    @Test
    void disabledRolesAndHiddenOrDisabledMenusDoNotEnterThePageRouteProjection() throws Exception {
        insertUser("visibility-reader", "visibility-reader-password");
        insertRole("VISIBILITY_ENABLED", 330);
        insertRole("VISIBILITY_DISABLED", 340, "DISABLED");
        jdbcTemplate.update("""
                INSERT INTO system_user_role (user_id, role_id)
                SELECT u.id, r.id FROM system_user u, system_role r
                WHERE u.username = 'visibility-reader'
                  AND r.role_code IN ('VISIBILITY_ENABLED', 'VISIBILITY_DISABLED')
                """);
        jdbcTemplate.update("""
                INSERT INTO system_role_permission (role_id, permission_id)
                SELECT r.id, p.id FROM system_role r, system_permission p
                WHERE r.role_code = 'VISIBILITY_ENABLED' AND p.permission_code = 'system:auth:me'
                """);
        jdbcTemplate.update("""
                INSERT INTO system_menu (parent_id, menu_name, menu_type, route_path, route_name, component_key, icon, sort, visible, status, created_at, updated_at)
                SELECT id, '隐藏页面', 'MENU', '/system/hidden-test', 'SystemHiddenTest', 'system/overview/index', 'Hide', 40, FALSE, 'ENABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM system_menu WHERE route_name = 'System'
                """);
        jdbcTemplate.update("""
                INSERT INTO system_menu (parent_id, menu_name, menu_type, route_path, route_name, component_key, icon, sort, visible, status, created_at, updated_at)
                SELECT id, '停用页面', 'MENU', '/system/disabled-test', 'SystemDisabledTest', 'system/overview/index', 'CircleClose', 50, TRUE, 'DISABLED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM system_menu WHERE route_name = 'System'
                """);
        jdbcTemplate.update("""
                INSERT INTO system_role_menu (role_id, menu_id)
                SELECT r.id, m.id FROM system_role r, system_menu m
                WHERE r.role_code = 'VISIBILITY_ENABLED'
                  AND m.route_name IN ('SystemHiddenTest', 'SystemDisabledTest')
                """);
        jdbcTemplate.update("""
                INSERT INTO system_role_menu (role_id, menu_id)
                SELECT r.id, m.id FROM system_role r, system_menu m
                WHERE r.role_code = 'VISIBILITY_DISABLED' AND m.route_name = 'SystemOverview'
                """);

        String visibilityReaderToken = login("visibility-reader", "visibility-reader-password");
        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + visibilityReaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonApiCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.menus").isEmpty());
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
        insertRole(roleCode, sort, "ENABLED");
    }

    private void insertRole(String roleCode, int sort, String status) {
        jdbcTemplate.update("""
                        INSERT INTO system_role (role_code, role_name, status, sort, created_at, updated_at)
                        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                roleCode,
                roleCode,
                status,
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
