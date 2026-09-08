/**
 * @file MybatisPlusSystemPersistenceIntegrationTests.java
 * @project Pipker Framework
 * @module Pipker Server
 * @description 在隔离 SQLite 数据库中验证 MyBatis-Plus 系统表映射、雪花主键和授权初始化数据。
 * @logic 由完整 Spring Boot 上下文执行 Liquibase 基线，随后通过八个 Mapper 写入并回读实体、校验关联去重和管理员授权投影。
 * @dependencies Spring Boot Test、Liquibase、SQLite JDBC、MyBatis-Plus、Pipker Business API
 * @index_tags server、test、mybatis-plus、liquibase、sqlite、rbac
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
import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.common.model.SystemUserRole;
import com.pipker.business.api.system.authorization.SystemAuthorizationService;
import com.pipker.business.api.system.authorization.RoleMenuConfigurationService;
import com.pipker.business.api.system.authorization.RoleMenuUpdateResult;
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
    private RoleMenuConfigurationService roleMenuConfigurationService;

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
                2090000000000000102L,
                List.of(2090000000000000303L)
        );
        assertThat(menuUpdate.menuIds()).containsExactly(2090000000000000303L);
        assertThat(roleMenuConfigurationService.getConfiguration().roles())
                .filteredOn(roleConfiguration -> "ADMIN".equals(roleConfiguration.code()))
                .singleElement()
                .satisfies(roleConfiguration -> assertThat(roleConfiguration.menuIds())
                        .containsExactly(2090000000000000303L));
    }

    private static Path createTemporaryDataRoot() {
        try {
            return Files.createTempDirectory("pipker-mybatis-plus-");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create temporary SQLite directory", exception);
        }
    }
}
