/**
 * @file MybatisPlusSystemPersistenceIntegrationTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 在隔离 SQLite 数据库中验证 MyBatis-Plus 系统表映射、雪花主键、授权初始化数据、角色管理与只读路由管理服务。
 * @logic 由完整 Spring Boot 上下文执行 Liquibase 基线，随后通过八个 Mapper 和应用服务验证实体读写、授权、真实分页、批量角色操作、密码重置和已落库路由定义。
 * @dependencies Spring Boot Test、Liquibase、SQLite JDBC、MyBatis-Plus、Pipker Business API、Pipker Security Starter
 * @index_tags server、test、mybatis-plus、liquibase、sqlite、rbac、role、route、password-reset
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
import com.pipker.business.api.system.authorization.RoleMenuConfiguration;
import com.pipker.business.api.system.authorization.RoleMenuConfigurationService;
import com.pipker.business.api.system.authorization.RoleMenuUpdateResult;
import com.pipker.business.api.system.role.RoleManagementService;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchDelete;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchStatus;
import com.pipker.business.api.system.role.RoleManagementRequest.Create;
import com.pipker.business.api.system.role.RoleManagementRequest.ResetMemberPassword;
import com.pipker.business.api.system.role.RoleManagementRequest.Update;
import com.pipker.business.api.system.role.RoleManagementResponse.PageResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleMember;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleSummary;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteDetail;
import com.pipker.business.api.system.route.RouteManagementResponse.RouteSummary;
import com.pipker.business.api.system.route.RouteManagementService;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

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
    private RoleMenuConfigurationService roleMenuConfigurationService;

    @Autowired
    private RoleManagementService roleManagementService;

    @Autowired
    private RouteManagementService routeManagementService;

    @Autowired
    private SecurityCryptoService securityCryptoService;

    @DynamicPropertySource
    static void configureTemporaryStorage(DynamicPropertyRegistry registry) {
        registry.add("pipker.data.database-root", DATA_ROOT::toString);
        registry.add("pipker.data.log-root", () -> DATA_ROOT.resolve("log").toString());
        registry.add("pipker.file.local.root", () -> DATA_ROOT.resolve("file").toString());
        registry.add("pipker.file.enabled", () -> "false");
    }

    @Test
    void mapsAllSystemTablesWithSnowflakeIdsAndPreservesAuthorizationRules() {
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
        rolePermission.setPermissionId(permission.getId());
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

        SystemAuthorizationSnapshot administratorSnapshot = systemAuthorizationService.findSnapshot(2090000000000000001L);
        assertThat(administratorSnapshot).isNotNull();
        assertThat(administratorSnapshot.roles()).contains(SystemAuthorizationService.SUPER_ADMIN_ROLE);
        assertThat(administratorSnapshot.permissions()).contains("system:auth:me");
        assertThat(administratorSnapshot.menus()).isNotEmpty();
        assertThat(systemApiResourceMapper.findAllEnabledAuthorizationRules())
                .extracting(rule -> rule.permissionCode())
                .contains("system:auth:me");

        RoleMenuUpdateResult menuUpdate = roleMenuConfigurationService.replaceRoleMenuAssignments(
                "2090000000000000102",
                List.of("2090000000000000303")
        );
        assertThat(menuUpdate.menuIds()).containsExactly("2090000000000000303");
        assertThat(roleMenuConfigurationService.getConfiguration().roles())
                .filteredOn(roleConfiguration -> "ADMIN".equals(roleConfiguration.code()))
                .singleElement()
                .satisfies(roleConfiguration -> assertThat(roleConfiguration.menuIds())
                        .containsExactly("2090000000000000303"));
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
    void registersAuthorizedHiddenPageRoutesWithoutPlacingThemInNavigationMenus() {
        String suffix = String.valueOf(System.nanoTime());
        SystemMenu hiddenRoute = new SystemMenu();
        hiddenRoute.setParentId(2090000000000000301L);
        hiddenRoute.setMenuName("隐藏路由 " + suffix);
        hiddenRoute.setMenuType("MENU");
        hiddenRoute.setRoutePath("/system/hidden-route-" + suffix);
        hiddenRoute.setRouteName("HiddenRoute" + suffix);
        hiddenRoute.setComponentKey("system/overview/index");
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
                .doesNotContain(String.valueOf(hiddenRoute.getId()));

        RoleMenuConfiguration configuration = roleMenuConfigurationService.getConfiguration();
        assertThat(configuration.menus().stream()
                .flatMap(menu -> menu.children().stream())
                .filter(menu -> menu.id().equals(String.valueOf(hiddenRoute.getId()))))
                .singleElement()
                .satisfies(menu -> assertThat(menu.visible()).isFalse());

        RoleMenuUpdateResult update = roleMenuConfigurationService.replaceRoleMenuAssignments(
                "2090000000000000102",
                List.of(String.valueOf(hiddenRoute.getId()))
        );
        assertThat(update.menuIds()).containsExactly(String.valueOf(hiddenRoute.getId()));
    }

    private static Path createTemporaryDataRoot() {
        try {
            return Files.createTempDirectory("pipker-mybatis-plus-");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create temporary SQLite directory", exception);
        }
    }
}
