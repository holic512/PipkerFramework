/**
 * @file MybatisPlusSystemPersistenceIntegrationTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 在隔离 SQLite 数据库中验证 MyBatis-Plus 系统表映射、雪花主键、Java 枚举接口权限、用户与角色管理、页面权限和路由管理服务。
 * @logic 由完整 Spring Boot 上下文执行 Liquibase 基线，随后通过八个 Mapper 和应用服务验证实体读写、权限枚举加载、用户角色分配和保护账户约束、用户查看与管理 HTTP 权限分离、角色接口与页面权限替换、历史权限保留、密码重置，以及路由的扁平查询、树形结构和受限配置更新。
 * @dependencies Spring Boot Test、Liquibase、SQLite JDBC、MyBatis-Plus、Pipker Business API、Pipker Security Starter
 * @index_tags server、test、mybatis-plus、liquibase、sqlite、rbac、user、role、route、permission、hidden-menu、password-reset
 * @author holic512
 */
package com.pipker.server;

import com.pipker.business.api.common.mapper.SystemApiResourceMapper;
import com.pipker.business.api.common.mapper.SystemMenuMapper;
import com.pipker.business.api.common.mapper.SystemPermissionMapper;
import com.pipker.business.api.common.mapper.SystemRoleMapper;
import com.pipker.business.api.common.mapper.SystemRoleMenuMapper;
import com.pipker.business.api.common.mapper.SystemRolePermissionMapper;
import com.pipker.business.api.common.mapper.SystemUserMapper;
import com.pipker.business.api.common.mapper.SystemUserRoleMapper;
import com.pipker.business.api.common.model.SystemApiResource;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.common.model.SystemMenu;
import com.pipker.business.api.common.model.SystemPermission;
import com.pipker.business.api.common.model.SystemRole;
import com.pipker.business.api.common.model.SystemRoleMenu;
import com.pipker.business.api.common.model.SystemRolePermission;
import com.pipker.business.api.common.model.SystemRouteDefinition;
import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.common.model.SystemUserRole;
import com.pipker.business.api.system.authorization.SystemAuthorizationService;
import com.pipker.business.api.system.authorization.SystemAuthorizationCache;
import com.pipker.business.api.system.permission.PermissionEnum;
import com.pipker.business.api.system.permission.PermissionRegistry;
import com.pipker.business.api.system.role.RoleManagementService;
import com.pipker.business.api.system.user.UserManagementService;
import com.pipker.business.api.system.user.UserManagementController;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchDelete;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchStatus;
import com.pipker.business.api.system.role.RoleManagementRequest.Create;
import com.pipker.business.api.system.role.RoleManagementRequest.ResetMemberPassword;
import com.pipker.business.api.system.role.RoleManagementRequest.ReplacePermissions;
import com.pipker.business.api.system.role.RoleManagementRequest.ReplaceRoutes;
import com.pipker.business.api.system.role.RoleManagementRequest.Update;
import com.pipker.business.api.system.role.RoleManagementResponse.PageResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleMember;
import com.pipker.business.api.system.role.RoleManagementResponse.RolePermissionConfiguration;
import com.pipker.business.api.system.role.RoleManagementResponse.RolePermissionUpdateResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleRouteConfiguration;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleRouteUpdateResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleSummary;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteDetail;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteSummary;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteTreeNode;
import com.pipker.business.api.system.route.RouteManagementService;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.pipker.business.api.system.route.RouteConfigurationRequest;
import com.pipker.business.api.system.route.RouteManagementController;
import com.pipker.business.api.system.auth.CurrentSystemAuthorizationService;
import com.pipker.business.api.system.permission.PermissionAuthorizationInterceptor;
import com.pipker.business.api.common.web.ApiExceptionHandler;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 系统表 MyBatis-Plus 持久层集成测试。 */
@SpringBootTest(classes = PipkerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MybatisPlusSystemPersistenceIntegrationTests {

    private static final Path DATA_ROOT = createTemporaryDataRoot();

    @Autowired
    private SystemUserMapper systemUserMapper;

    @Autowired
    private SystemRoleMapper systemRoleMapper;

    @Autowired
    private SystemPermissionMapper systemPermissionMapper;

    @Autowired
    private SystemMenuMapper systemMenuMapper;

    @Autowired
    private SystemApiResourceMapper systemApiResourceMapper;

    @Autowired
    private SystemUserRoleMapper systemUserRoleMapper;

    @Autowired
    private SystemRolePermissionMapper systemRolePermissionMapper;

    @Autowired
    private SystemRoleMenuMapper systemRoleMenuMapper;

    @Autowired
    private SystemAuthorizationService systemAuthorizationService;

    @Autowired
    private SystemAuthorizationCache systemAuthorizationCache;

    @Autowired
    private RoleManagementService roleManagementService;

    @Autowired
    private UserManagementService userManagementService;

    @Autowired
    private RouteManagementService routeManagementService;

    @Autowired
    private SecurityCryptoService securityCryptoService;

    @Autowired
    private PermissionRegistry permissionRegistry;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void configureTemporaryStorage(DynamicPropertyRegistry registry) {
        registry.add("pipker.data.database-root", DATA_ROOT::toString);
        registry.add("pipker.data.log-root", () -> DATA_ROOT.resolve("log").toString());
        registry.add("pipker.file.local.root", () -> DATA_ROOT.resolve("file").toString());
        registry.add("pipker.file.enabled", () -> "false");
    }

    @Test
    void routeConfigurationHttpContractProtectsStructureAndManagePermission() throws Exception {
        SystemMenu menu = configurationTestMenu(null, "MENU");
        String id = menu.getId().toString();
        MockMvc manager = routeHttp(true);
        manager.perform(get("/api/admin/routes").param("keyword", menu.getRouteName()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(id));
        manager.perform(get("/api/admin/routes/{id}", id))
                .andExpect(jsonPath("$.data.routePath").value(menu.getRoutePath()));
        String body = """
                {"menuName":"  配置测试  ","icon":"UserFilled","sort":7,"visible":false,"status":"DISABLED"}
                """;
        routeHttp(false).perform(put("/api/admin/routes/{id}/configuration", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(403));
        assertThat(systemMenuMapper.selectById(menu.getId()).getMenuName()).isEqualTo(menu.getMenuName());
        manager.perform(put("/api/admin/routes/{id}/configuration", id)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.menuName").value("配置测试"))
                .andExpect(jsonPath("$.data.icon").value("UserFilled"))
                .andExpect(jsonPath("$.data.visible").value(false));
        SystemMenu saved = systemMenuMapper.selectById(menu.getId());
        assertThat(saved.getRoutePath()).isEqualTo(menu.getRoutePath());
        assertThat(saved.getComponentKey()).isEqualTo(menu.getComponentKey());
        assertThat(saved.getCreatedAt()).isEqualTo(menu.getCreatedAt());
        manager.perform(put("/api/admin/routes/{id}/configuration", id).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("UserFilled", "")))
                .andExpect(jsonPath("$.code").value(200));
        assertThat(systemMenuMapper.selectById(menu.getId()).getIcon()).isNull();
        // Unknown structural fields may be rejected by the JSON mapper or ignored, but never persisted.
        manager.perform(put("/api/admin/routes/{id}/configuration", id).contentType(MediaType.APPLICATION_JSON)
                .content(body.replace("\"sort\":7", "\"sort\":7,\"routePath\":\"/tampered\",\"parentId\":\"1\"")));
        assertThat(systemMenuMapper.selectById(menu.getId()).getRoutePath()).isEqualTo(menu.getRoutePath());
        assertThat(systemMenuMapper.selectById(menu.getId()).getParentId()).isNull();
        for (String invalid : List.of(body.replace("  配置测试  ", " "), body.replace("\"sort\":7", "\"sort\":-1"),
                body.replace("DISABLED", "INVALID"), body.replace("false", "null"), body.replace("UserFilled", "x".repeat(101)))) {
            manager.perform(put("/api/admin/routes/{id}/configuration", id).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(jsonPath("$.code").value(400));
        }
        manager.perform(put("/api/admin/routes/1/configuration").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(400));
        manager.perform(post("/api/admin/routes").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(400));
        manager.perform(delete("/api/admin/routes/{id}", id)).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void routeSubtreeConfigurationRefreshesSnapshotsAndPreservesSuperAdminAccess() {
        SystemMenu directory = configurationTestMenu(null, "DIRECTORY");
        SystemMenu page = configurationTestMenu(directory.getId(), "MENU");
        String pageId = page.getId().toString();
        String directoryId = directory.getId().toString();
        String suffix = String.valueOf(System.nanoTime());
        SystemRole role = new SystemRole();
        role.setRoleCode("ROUTE_CONFIG_" + suffix); role.setRoleName("Route config"); role.setStatus("ENABLED"); role.setSort(99);
        systemRoleMapper.insert(role);
        SystemUser user = new SystemUser();
        user.setUsername("route-config-" + suffix); user.setPasswordHash("test-only"); user.setNickname("Route config"); user.setStatus("ENABLED");
        systemUserMapper.insert(user);
        SystemUserRole link = new SystemUserRole(); link.setUserId(user.getId()); link.setRoleId(role.getId()); systemUserRoleMapper.insert(link);
        roleManagementService.replaceRoleRouteConfiguration(role.getId().toString(), new ReplaceRoutes(List.of(pageId)));
        assertThat(systemAuthorizationService.findSnapshot(user.getId()).routes()).extracting(SystemRouteDefinition::id).contains(pageId);
        SystemAuthorizationSnapshot cachedSuper = systemAuthorizationService.findSnapshot(2090000000000000001L);
        routeManagementService.updateConfiguration(directoryId, new RouteConfigurationRequest("隐藏目录", "Folder", 11, false, "ENABLED"));
        assertThat(systemAuthorizationService.findSnapshot(user.getId()).routes()).extracting(SystemRouteDefinition::id).contains(pageId);
        assertThat(systemAuthorizationService.findSnapshot(user.getId()).menus()).isEmpty();
        assertThat(systemAuthorizationService.findSnapshot(2090000000000000001L)).isNotSameAs(cachedSuper);
        routeManagementService.updateConfiguration(directoryId, new RouteConfigurationRequest("停用目录", "Folder", 11, false, "DISABLED"));
        assertThat(systemAuthorizationService.findSnapshot(user.getId()).routes()).isEmpty();
        assertThat(systemAuthorizationService.findAvailablePageMenus(false)).extracting(SystemMenu::getId).doesNotContain(page.getId());
        assertThatThrownBy(() -> roleManagementService.replaceRoleRouteConfiguration(
                role.getId().toString(), new ReplaceRoutes(List.of(pageId))))
                .isInstanceOf(ApiBusinessException.class);
        assertThat(systemAuthorizationService.findSnapshot(2090000000000000001L).routes()).extracting(SystemRouteDefinition::id).contains(pageId);
        assertThat(systemAuthorizationService.findSnapshot(2090000000000000001L).menus()).extracting(menu -> menu.id()).contains(directoryId);
        assertThat(roleManagementService.findRoleRouteConfiguration("2090000000000000101").routeIds()).contains(pageId);
        assertThatThrownBy(() -> roleManagementService.replaceRoleRouteConfiguration(
                "2090000000000000101", new ReplaceRoutes(List.of())))
                .isInstanceOf(ApiBusinessException.class);
        routeManagementService.updateConfiguration(pageId, new RouteConfigurationRequest(page.getMenuName(), null, 2, true, "DISABLED"));
        routeManagementService.updateConfiguration(directoryId, new RouteConfigurationRequest(directory.getMenuName(), null, 0, true, "ENABLED"));
        assertThat(systemAuthorizationService.findSnapshot(user.getId()).routes()).isEmpty();
        routeManagementService.updateConfiguration(pageId, new RouteConfigurationRequest("恢复页面", "House", 1, true, "ENABLED"));
        assertThat(systemAuthorizationService.findSnapshot(user.getId()).routes()).extracting(SystemRouteDefinition::id).contains(pageId);
        var cached = systemAuthorizationService.findSnapshot(user.getId());
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            routeManagementService.updateConfiguration(pageId, new RouteConfigurationRequest("回滚", null, 0, false, "DISABLED"));
            assertThat(systemAuthorizationService.findSnapshot(user.getId())).isSameAs(cached);
            transaction.setRollbackOnly();
        });
        assertThat(routeManagementService.findRouteDetail(pageId).menuName()).isEqualTo("恢复页面");
        assertThat(systemAuthorizationService.findSnapshot(user.getId())).isSameAs(cached);
    }

    private SystemMenu configurationTestMenu(Long parentId, String type) {
        String suffix = String.valueOf(System.nanoTime());
        SystemMenu menu = new SystemMenu();
        menu.setParentId(parentId); menu.setMenuName("Route test " + suffix); menu.setMenuType(type);
        menu.setRouteName("RouteTest" + suffix); menu.setRoutePath("/route-test/" + suffix);
        if ("MENU".equals(type)) menu.setComponentKey("system/route/index");
        menu.setSort(999); menu.setVisible(true); menu.setStatus("ENABLED");
        systemMenuMapper.insert(menu);
        return menu;
    }

    private MockMvc routeHttp(boolean manage) {
        CurrentSystemAuthorizationService authorization = new CurrentSystemAuthorizationService(null, null) {
            @Override
            public SystemAuthorizationSnapshot currentSnapshot() {
                return new SystemAuthorizationSnapshot(null, List.of(), manage
                        ? List.of(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode(), PermissionEnum.SYSTEM_ROUTE_MANAGE.getCode())
                        : List.of(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode()), List.of(), List.of());
            }
        };
        return MockMvcBuilders.standaloneSetup(new RouteManagementController(routeManagementService))
                .setControllerAdvice(new ApiExceptionHandler())
                .addInterceptors(new PermissionAuthorizationInterceptor(authorization)).build();
    }

    @Test
    void userManagementHttpContractSeparatesViewAndManagePermissions() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        RoleSummary role = roleManagementService.createRole(new Create(
                "USER_HTTP_" + suffix,
                "用户 HTTP 测试角色",
                null,
                "ENABLED",
                521
        ));
        MockMvc viewer = userHttp(false);
        viewer.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        viewer.perform(get("/api/admin/users/assignable-roles"))
                .andExpect(jsonPath("$.code").value(403));

        String body = """
                {"username":"user_http_%s","password":"InitialPass123","nickname":"HTTP 测试用户","status":"ENABLED","roleIds":["%s"]}
                """.formatted(suffix, role.id());
        viewer.perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.code").value(403));
        userHttp(true).perform(post("/api/admin/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("user_http_" + suffix));
    }

    private MockMvc userHttp(boolean manage) {
        CurrentSystemAuthorizationService authorization = new CurrentSystemAuthorizationService(null, null) {
            @Override
            public SystemAuthorizationSnapshot currentSnapshot() {
                return new SystemAuthorizationSnapshot(null, List.of(), manage
                        ? List.of(PermissionEnum.SYSTEM_USER_VIEW.getCode(), PermissionEnum.SYSTEM_USER_MANAGE.getCode())
                        : List.of(PermissionEnum.SYSTEM_USER_VIEW.getCode()), List.of(), List.of());
            }
        };
        return MockMvcBuilders.standaloneSetup(new UserManagementController(userManagementService))
                .setControllerAdvice(new ApiExceptionHandler())
                .addInterceptors(new PermissionAuthorizationInterceptor(authorization)).build();
    }

    @Test
    void mapsAllSystemTablesWithSnowflakeIdsAndUsesEnumBasedPermissions() {
        String suffix = String.valueOf(System.nanoTime());

        SystemUser user = new SystemUser();
        user.setUsername("mapper-test-" + suffix);
        user.setPasswordHash("{bcrypt}test-hash");
        user.setNickname("Mapper Test");
        user.setStatus("ENABLED");
        assertThat(systemUserMapper.insert(user)).isEqualTo(1);
        assertThat(user.getId()).isPositive();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        assertThat(systemUserMapper.selectById(user.getId()).getUsername()).isEqualTo(user.getUsername());

        SystemRole role = new SystemRole();
        role.setRoleCode("MAPPER_TEST_" + suffix);
        role.setRoleName("Mapper Test Role");
        role.setStatus("ENABLED");
        role.setSort(999);
        assertThat(systemRoleMapper.insert(role)).isEqualTo(1);
        assertThat(role.getId()).isPositive();
        assertThat(role.getCreatedAt()).isNotNull();

        SystemPermission permission = new SystemPermission();
        permission.setPermissionCode("mapper:test:" + suffix);
        permission.setPermissionName("Mapper Test Permission");
        permission.setPermissionType("API");
        permission.setStatus("ENABLED");
        assertThat(systemPermissionMapper.insert(permission)).isEqualTo(1);
        assertThat(permission.getId()).isPositive();
        assertThat(permission.getUpdatedAt()).isNotNull();

        SystemMenu menu = new SystemMenu();
        menu.setMenuName("Mapper Test Menu");
        menu.setMenuType("MENU");
        menu.setRoutePath("/mapper-test/" + suffix);
        menu.setRouteName("MapperTest" + suffix);
        menu.setComponentKey("mapper/test/index");
        menu.setSort(999);
        menu.setVisible(true);
        menu.setStatus("ENABLED");
        assertThat(systemMenuMapper.insert(menu)).isEqualTo(1);
        assertThat(menu.getId()).isPositive();
        assertThat(menu.getCreatedAt()).isNotNull();

        SystemApiResource apiResource = new SystemApiResource();
        apiResource.setPermissionId(permission.getId());
        apiResource.setHttpMethod("GET");
        apiResource.setPathPattern("/api/mapper-test/" + suffix);
        assertThat(systemApiResourceMapper.insert(apiResource)).isEqualTo(1);
        assertThat(apiResource.getId()).isPositive();

        SystemUserRole userRole = new SystemUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        assertThat(systemUserRoleMapper.insert(userRole)).isEqualTo(1);
        assertThat(userRole.getId()).isPositive();

        SystemRolePermission rolePermission = new SystemRolePermission();
        rolePermission.setRoleId(role.getId());
        rolePermission.setPermissionCode(PermissionEnum.SYSTEM_ROLE_MANAGE.getCode());
        assertThat(systemRolePermissionMapper.insert(rolePermission)).isEqualTo(1);
        assertThat(rolePermission.getId()).isPositive();

        SystemRoleMenu roleMenu = new SystemRoleMenu();
        roleMenu.setRoleId(role.getId());
        roleMenu.setMenuId(menu.getId());
        assertThat(systemRoleMenuMapper.insert(roleMenu)).isEqualTo(1);
        assertThat(roleMenu.getId()).isPositive();

        SystemUserRole duplicateUserRole = new SystemUserRole();
        duplicateUserRole.setUserId(user.getId());
        duplicateUserRole.setRoleId(role.getId());
        assertThatThrownBy(() -> systemUserRoleMapper.insert(duplicateUserRole))
                .isInstanceOf(RuntimeException.class);

        SystemRolePermission duplicateRolePermission = new SystemRolePermission();
        duplicateRolePermission.setRoleId(role.getId());
        duplicateRolePermission.setPermissionCode(PermissionEnum.SYSTEM_ROLE_MANAGE.getCode());
        assertThatThrownBy(() -> systemRolePermissionMapper.insert(duplicateRolePermission))
                .isInstanceOf(RuntimeException.class);

        SystemAuthorizationSnapshot administratorSnapshot = systemAuthorizationService.findSnapshot(2090000000000000001L);
        assertThat(administratorSnapshot).isNotNull();
        assertThat(administratorSnapshot.roles()).contains(SystemAuthorizationService.SUPER_ADMIN_ROLE);
        assertThat(administratorSnapshot.permissions()).contains(PermissionEnum.SYSTEM_AUTHORIZATION_VIEW.getCode());
        assertThat(administratorSnapshot.menus()).isNotEmpty();
        assertThat(administratorSnapshot.routes())
                .extracting(SystemRouteDefinition::id)
                .contains("2090000000000000302");
        assertThat(administratorSnapshot.menus())
                .filteredOn(overview -> overview.id().equals("2090000000000000302"))
                .singleElement()
                .satisfies(overview -> {
                    assertThat(overview.parentId()).isNull();
                    assertThat(overview.routeName()).isEqualTo("SystemOverview");
                    assertThat(overview.componentKey()).isEqualTo("system/overview/index");
                });
        assertThat(permissionRegistry.list())
                .extracting(PermissionRegistry.PermissionPoint::code)
                .containsExactlyElementsOf(List.of(PermissionEnum.values()).stream()
                        .map(PermissionEnum::getCode)
                        .toList());

        RoleRouteUpdateResult routeUpdate = roleManagementService.replaceRoleRouteConfiguration(
                "2090000000000000102",
                new ReplaceRoutes(List.of("2090000000000000304"))
        );
        assertThat(routeUpdate.routeIds()).containsExactly("2090000000000000304");
        assertThat(roleManagementService.findRoleRouteConfiguration("2090000000000000102").routeIds())
                .containsExactly("2090000000000000304");
    }

    @Test
    void managesOrdinaryRolesWithPaginationBatchOperationsAndMemberPasswordResets() {
        String suffix = String.valueOf(System.nanoTime());
        RoleSummary createdRole = roleManagementService.createRole(new Create(
                "ROLE_TEST_" + suffix,
                "角色管理测试",
                "验证角色生命周期与成员账户操作",
                "ENABLED",
                432
        ));
        assertThat(createdRole.id()).matches("\\d+");
        assertThat(createdRole.roleCode()).isEqualTo("ROLE_TEST_" + suffix);

        PageResult<RoleSummary> filteredRoles = roleManagementService.findRolePage(
                1,
                10,
                suffix,
                "ENABLED"
        );
        assertThat(filteredRoles.total()).isEqualTo(1);
        assertThat(filteredRoles.records()).extracting(RoleSummary::id).containsExactly(createdRole.id());

        RoleSummary updatedRole = roleManagementService.updateRole(createdRole.id(), new Update(
                "已更新的角色管理测试",
                "更新后的说明",
                "ENABLED",
                433
        ));
        assertThat(updatedRole.roleName()).isEqualTo("已更新的角色管理测试");
        assertThat(updatedRole.sort()).isEqualTo(433);

        SystemUser member = new SystemUser();
        member.setUsername("role-member-" + suffix);
        member.setPasswordHash(securityCryptoService.hashPassword("InitialPass123"));
        member.setNickname("角色成员");
        member.setStatus("ENABLED");
        systemUserMapper.insert(member);

        SystemUserRole memberAssignment = new SystemUserRole();
        memberAssignment.setUserId(member.getId());
        memberAssignment.setRoleId(Long.parseLong(createdRole.id()));
        systemUserRoleMapper.insert(memberAssignment);

        PageResult<RoleMember> members = roleManagementService.findMemberPage(
                createdRole.id(),
                1,
                10,
                "role-member"
        );
        assertThat(members.total()).isEqualTo(1);
        assertThat(members.records()).extracting(RoleMember::id)
                .containsExactly(String.valueOf(member.getId()));

        assertThat(roleManagementService.findRoleDetail(createdRole.id()).memberCount()).isEqualTo(1);

        SystemRolePermission historicalPermission = new SystemRolePermission();
        historicalPermission.setRoleId(Long.parseLong(createdRole.id()));
        historicalPermission.setPermissionCode("retired-system-role-manage");
        systemRolePermissionMapper.insert(historicalPermission);

        RolePermissionUpdateResult permissionUpdate = roleManagementService.replaceRolePermissionConfiguration(
                createdRole.id(),
                new ReplacePermissions(List.of(
                        PermissionEnum.SYSTEM_ROUTE_VIEW.getCode(),
                        PermissionEnum.SYSTEM_ROUTE_VIEW.getCode()
                ))
        );
        assertThat(permissionUpdate.permissionCodes()).containsExactly(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode());
        RolePermissionConfiguration permissionConfiguration = roleManagementService.findRolePermissionConfiguration(
                createdRole.id()
        );
        assertThat(permissionConfiguration.permissionCodes()).containsExactly(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode());
        assertThat(permissionConfiguration.historicalPermissionCodes()).containsExactly("retired-system-role-manage");
        assertThat(systemAuthorizationService.findSnapshot(member.getId()).permissions())
                .contains(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode())
                .doesNotContain("retired-system-role-manage");

        assertThatThrownBy(() -> roleManagementService.replaceRolePermissionConfiguration(
                createdRole.id(),
                new ReplacePermissions(List.of("arbitrary-client-permission"))
        )).isInstanceOf(ApiBusinessException.class);
        assertThatThrownBy(() -> roleManagementService.replaceRolePermissionConfiguration(
                "2090000000000000101",
                new ReplacePermissions(List.of(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode()))
        )).isInstanceOf(ApiBusinessException.class);

        roleManagementService.resetMemberPassword(
                createdRole.id(),
                String.valueOf(member.getId()),
                new ResetMemberPassword("ChangedPass123")
        );
        assertThat(securityCryptoService.matchesPassword(
                "ChangedPass123",
                systemUserMapper.selectById(member.getId()).getPasswordHash()
        )).isTrue();

        roleManagementService.updateRoleStatus(new BatchStatus(List.of(createdRole.id()), "DISABLED"));
        assertThat(systemRoleMapper.selectById(Long.parseLong(createdRole.id())).getStatus()).isEqualTo("DISABLED");

        assertThatThrownBy(() -> roleManagementService.deleteRoles(new BatchDelete(List.of("2090000000000000101"))))
                .isInstanceOf(ApiBusinessException.class);

        roleManagementService.deleteRoles(new BatchDelete(List.of(createdRole.id())));
        assertThat(systemRoleMapper.selectById(Long.parseLong(createdRole.id()))).isNull();
        assertThat(systemUserRoleMapper.selectById(memberAssignment.getId())).isNull();
    }

    @Test
    void managesUsersWithRoleAssignmentsAndProtectsSuperAdminAccounts() {
        String suffix = String.valueOf(System.nanoTime());
        RoleSummary role = roleManagementService.createRole(new Create(
                "USER_MANAGER_" + suffix,
                "用户管理测试角色",
                "用于用户管理集成测试",
                "ENABLED",
                520
        ));
        var createdUser = userManagementService.createUser(
                new com.pipker.business.api.system.user.UserManagementRequest.Create(
                        "user-management-" + suffix,
                        "InitialPass123",
                        "用户管理测试",
                        "13800000000",
                        "user-" + suffix + "@example.test",
                        "ENABLED",
                        List.of(role.id())
                )
        );
        assertThat(createdUser.id()).matches("\\d+");
        assertThat(createdUser.roles()).extracting(userRole -> userRole.roleCode())
                .containsExactly(role.roleCode());
        assertThat(createdUser.protectedAccount()).isFalse();

        var userPage = userManagementService.findUserPage(1, 10, suffix, "ENABLED");
        assertThat(userPage.records()).extracting(user -> user.id()).contains(createdUser.id());
        assertThat(userManagementService.findAssignableRoles()).extracting(assignableRole -> assignableRole.id())
                .contains(role.id())
                .doesNotContain("2090000000000000101");

        var updatedUser = userManagementService.updateUser(
                createdUser.id(),
                new com.pipker.business.api.system.user.UserManagementRequest.Update(
                        "已更新的用户管理测试",
                        null,
                        "updated-" + suffix + "@example.test",
                        "ENABLED",
                        List.of(role.id())
                )
        );
        assertThat(updatedUser.nickname()).isEqualTo("已更新的用户管理测试");
        assertThat(userManagementService.findUserDetail(createdUser.id()).roles())
                .extracting(userRole -> userRole.id())
                .containsExactly(role.id());

        userManagementService.resetPassword(
                createdUser.id(),
                new com.pipker.business.api.system.user.UserManagementRequest.ResetPassword("ChangedPass123")
        );
        assertThat(securityCryptoService.matchesPassword(
                "ChangedPass123",
                systemUserMapper.selectById(Long.parseLong(createdUser.id())).getPasswordHash()
        )).isTrue();

        userManagementService.updateUserStatus(
                new com.pipker.business.api.system.user.UserManagementRequest.BatchStatus(
                        List.of(createdUser.id()),
                        "DISABLED"
                )
        );
        assertThat(systemUserMapper.selectById(Long.parseLong(createdUser.id())).getStatus()).isEqualTo("DISABLED");

        assertThat(userManagementService.findUserDetail("2090000000000000001").protectedAccount()).isTrue();
        assertThatThrownBy(() -> userManagementService.resetPassword(
                "2090000000000000001",
                new com.pipker.business.api.system.user.UserManagementRequest.ResetPassword("ChangedPass123")
        )).isInstanceOf(ApiBusinessException.class);

        userManagementService.deleteUsers(
                new com.pipker.business.api.system.user.UserManagementRequest.BatchDelete(List.of(createdUser.id()))
        );
        assertThat(systemUserMapper.selectById(Long.parseLong(createdUser.id()))).isNull();
        assertThat(systemUserRoleMapper.findRolesByUserId(Long.parseLong(createdUser.id()))).isEmpty();
    }

    @Test
    void readsPersistedPageRoutesAndDirectoryCategoriesWithoutRouteWrites() {
        com.pipker.business.api.system.route.RouteManagementResponse.PageResult<RouteSummary> routePage =
                routeManagementService.findRoutePage(1, 10, "/system/routes", "MENU", true, "ENABLED");

        assertThat(routePage.total()).isEqualTo(1);
        RouteSummary route = routePage.records().getFirst();
        assertThat(route.id()).isEqualTo("2090000000000000305");
        assertThat(route.routeName()).isEqualTo("SystemRouteManagement");
        assertThat(route.routePath()).isEqualTo("/system/routes");
        assertThat(route.componentKey()).isEqualTo("system/route/index");
        assertThat(route.componentIndexRequired()).isTrue();
        assertThat(route.visible()).isTrue();

        RouteDetail routeDetail = routeManagementService.findRouteDetail(route.id());
        assertThat(routeDetail.parentName()).isEqualTo("系统管理");
        assertThat(routeDetail.createdAt()).isNotNull();
        assertThat(routeDetail.updatedAt()).isNotNull();

        com.pipker.business.api.system.route.RouteManagementResponse.PageResult<RouteSummary> directoryPage =
                routeManagementService.findRoutePage(1, 10, "系统管理", "DIRECTORY", true, "ENABLED");
        assertThat(directoryPage.records()).singleElement()
                .satisfies(directory -> {
                    assertThat(directory.componentIndexRequired()).isFalse();
                    assertThat(directory.componentKey()).isNull();
                    assertThat(directory.routePath()).isEqualTo("/system");
                });
    }

    @Test
    void readsArbitrarilyNestedRouteManagementTree() throws Exception {
        SystemMenu root = configurationTestMenu(null, "DIRECTORY");
        SystemMenu nestedDirectory = configurationTestMenu(root.getId(), "DIRECTORY");
        SystemMenu page = configurationTestMenu(nestedDirectory.getId(), "MENU");

        List<RouteTreeNode> tree = routeManagementService.findRouteTree();
        RouteTreeNode rootNode = tree.stream()
                .filter(node -> node.id().equals(root.getId().toString()))
                .findFirst()
                .orElseThrow();
        assertThat(rootNode.children()).singleElement().satisfies(child -> {
            assertThat(child.id()).isEqualTo(nestedDirectory.getId().toString());
            assertThat(child.children()).singleElement()
                    .extracting(RouteTreeNode::id)
                    .isEqualTo(page.getId().toString());
        });

        routeHttp(false).perform(get("/api/admin/routes/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void superAdminSeesHiddenRoutesWhileOrdinaryRoleCanStillBeGrantedAccess() {
        String suffix = String.valueOf(System.nanoTime());
        SystemMenu hiddenRoute = new SystemMenu();
        hiddenRoute.setParentId(2090000000000000301L);
        hiddenRoute.setMenuName("隐藏路由 " + suffix);
        hiddenRoute.setMenuType("MENU");
        hiddenRoute.setRoutePath("/system/hidden-route-" + suffix);
        hiddenRoute.setRouteName("HiddenRoute" + suffix);
        hiddenRoute.setComponentKey("system/route/index");
        hiddenRoute.setSort(997);
        hiddenRoute.setVisible(false);
        hiddenRoute.setStatus("ENABLED");
        systemMenuMapper.insert(hiddenRoute);

        systemAuthorizationCache.invalidateAllSnapshots();

        SystemAuthorizationSnapshot superAdminSnapshot = systemAuthorizationService.findSnapshot(2090000000000000001L);
        assertThat(superAdminSnapshot.routes())
                .extracting(SystemRouteDefinition::id)
                .contains(String.valueOf(hiddenRoute.getId()));
        assertThat(superAdminSnapshot.menus().stream()
                .flatMap(menu -> menu.children().stream())
                .map(menu -> menu.id()))
                .contains(String.valueOf(hiddenRoute.getId()));

        RoleRouteConfiguration configuration = roleManagementService.findRoleRouteConfiguration("2090000000000000102");
        assertThat(configuration.routes().stream()
                .flatMap(menu -> menu.children().stream())
                .filter(menu -> menu.id().equals(String.valueOf(hiddenRoute.getId()))))
                .singleElement()
                .satisfies(menu -> assertThat(menu.visible()).isFalse());

        RoleRouteUpdateResult update = roleManagementService.replaceRoleRouteConfiguration(
                "2090000000000000102",
                new ReplaceRoutes(List.of(String.valueOf(hiddenRoute.getId())))
        );
        assertThat(update.routeIds()).containsExactly(String.valueOf(hiddenRoute.getId()));
        assertThat(roleManagementService.findRoleRouteConfiguration("2090000000000000102").routeIds())
                .containsExactly(String.valueOf(hiddenRoute.getId()));
    }

    @Test
    void replacesRolePagePermissionsAndSeparatesHiddenRouteAccessFromNavigation() {
        String suffix = String.valueOf(System.nanoTime());
        SystemRole role = new SystemRole();
        role.setRoleCode("ROUTE_PERMISSION_" + suffix);
        role.setRoleName("页面权限测试角色");
        role.setStatus("ENABLED");
        role.setSort(998);
        systemRoleMapper.insert(role);

        SystemUser user = new SystemUser();
        user.setUsername("route-permission-user-" + suffix);
        user.setPasswordHash("{bcrypt}test-hash");
        user.setStatus("ENABLED");
        systemUserMapper.insert(user);

        SystemUserRole membership = new SystemUserRole();
        membership.setUserId(user.getId());
        membership.setRoleId(role.getId());
        systemUserRoleMapper.insert(membership);

        SystemMenu hiddenRoute = new SystemMenu();
        hiddenRoute.setParentId(2090000000000000301L);
        hiddenRoute.setMenuName("页面权限隐藏路由 " + suffix);
        hiddenRoute.setMenuType("MENU");
        hiddenRoute.setRoutePath("/system/role-permission-hidden-" + suffix);
        hiddenRoute.setRouteName("RolePermissionHidden" + suffix);
        hiddenRoute.setComponentKey("system/route/index");
        hiddenRoute.setSort(998);
        hiddenRoute.setVisible(false);
        hiddenRoute.setStatus("ENABLED");
        systemMenuMapper.insert(hiddenRoute);

        SystemMenu unindexedRoute = new SystemMenu();
        unindexedRoute.setParentId(2090000000000000301L);
        unindexedRoute.setMenuName("未配置索引页面 " + suffix);
        unindexedRoute.setMenuType("MENU");
        unindexedRoute.setSort(999);
        unindexedRoute.setVisible(true);
        unindexedRoute.setStatus("ENABLED");
        systemMenuMapper.insert(unindexedRoute);
        systemAuthorizationCache.invalidateAllSnapshots();

        SystemAuthorizationSnapshot superAdminSnapshot = systemAuthorizationService.findSnapshot(2090000000000000001L);
        assertThat(superAdminSnapshot.routes())
                .extracting(SystemRouteDefinition::id)
                .doesNotContain(String.valueOf(unindexedRoute.getId()));
        assertThat(superAdminSnapshot.menus().stream()
                .flatMap(menu -> menu.children().stream())
                .map(menu -> menu.id()))
                .doesNotContain(String.valueOf(unindexedRoute.getId()));

        RoleRouteConfiguration initialConfiguration = roleManagementService.findRoleRouteConfiguration(
                String.valueOf(role.getId())
        );
        assertThat(initialConfiguration.allRoutes()).isFalse();
        assertThat(initialConfiguration.routeIds()).isEmpty();
        assertThat(initialConfiguration.routes().stream()
                .flatMap(menu -> menu.children().stream())
                .map(menu -> menu.id()))
                .contains(String.valueOf(hiddenRoute.getId()))
                .doesNotContain(String.valueOf(unindexedRoute.getId()));

        RoleRouteUpdateResult update = roleManagementService.replaceRoleRouteConfiguration(
                String.valueOf(role.getId()),
                new ReplaceRoutes(List.of("2090000000000000304", String.valueOf(hiddenRoute.getId())))
        );
        assertThat(update.routeIds())
                .containsExactly("2090000000000000304", String.valueOf(hiddenRoute.getId()));

        SystemAuthorizationSnapshot snapshot = systemAuthorizationService.findSnapshot(user.getId());
        assertThat(snapshot.routes())
                .extracting(SystemRouteDefinition::id)
                .contains("2090000000000000304", String.valueOf(hiddenRoute.getId()));
        assertThat(snapshot.menus().stream()
                .flatMap(menu -> menu.children().stream())
                .map(menu -> menu.id()))
                .contains("2090000000000000304")
                .doesNotContain(String.valueOf(hiddenRoute.getId()));

        assertThatThrownBy(() -> roleManagementService.replaceRoleRouteConfiguration(
                "2090000000000000101",
                new ReplaceRoutes(List.of("2090000000000000304"))
        )).isInstanceOf(ApiBusinessException.class);
        assertThatThrownBy(() -> roleManagementService.replaceRoleRouteConfiguration(
                String.valueOf(role.getId()),
                new ReplaceRoutes(List.of("2090000000000000301"))
        )).isInstanceOf(ApiBusinessException.class);
        assertThatThrownBy(() -> roleManagementService.replaceRoleRouteConfiguration(
                String.valueOf(role.getId()),
                new ReplaceRoutes(List.of(String.valueOf(unindexedRoute.getId())))
        )).isInstanceOf(ApiBusinessException.class);
    }

    private static Path createTemporaryDataRoot() {
        try {
            return Files.createTempDirectory("pipker-mybatis-plus-");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create temporary SQLite directory", exception);
        }
    }
}
