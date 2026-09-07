package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.auth.SystemLoginTypes;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.starter.satoken.config.PipkerAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemApiAuthorizationServiceTests {

    private SystemAuthorizationMapper systemAuthorizationMapper;
    private TestSystemAuthorizationService systemAuthorizationService;
    private SystemApiAuthorizationService systemApiAuthorizationService;

    @BeforeEach
    void setUp() {
        systemAuthorizationMapper = new TestSystemAuthorizationMapper();
        systemAuthorizationService = new TestSystemAuthorizationService();
        PipkerAuthProperties authProperties = new PipkerAuthProperties();
        authProperties.setAuthorizationCacheTtl(Duration.ofSeconds(60));
        authProperties.setAuthorizationCacheMaximumSize(10);
        systemApiAuthorizationService = new SystemApiAuthorizationService(
                systemAuthorizationMapper,
                systemAuthorizationService,
                new SystemAuthorizationCache(authProperties)
        );
    }

    @Test
    void grantsOnlyAUniqueMethodAndMvcTemplateMatchWithTheRequiredPermission() {
        TestSystemAuthorizationMapper mapper = (TestSystemAuthorizationMapper) systemAuthorizationMapper;
        mapper.apiResources = List.of(
                new SystemApiResource(1L, "widget:read", "GET", "/api/widgets/{id}")
        );
        systemAuthorizationService.snapshot = snapshotWith("widget:read");

        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/widgets/7"))
                .isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "POST", "/api/widgets/7"))
                .isFalse();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/widgets"))
                .isFalse();

        assertThat(mapper.apiResourceReads).isEqualTo(1);
    }

    @Test
    void deniesOverlappingRulesBeforeReadingTheUserPermissionSnapshot() {
        TestSystemAuthorizationMapper mapper = (TestSystemAuthorizationMapper) systemAuthorizationMapper;
        mapper.apiResources = List.of(
                new SystemApiResource(1L, "widget:read", "GET", "/api/widgets/{id}"),
                new SystemApiResource(2L, "widget:all", "GET", "/api/widgets/*")
        );

        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/widgets/7"))
                .isFalse();

        assertThat(systemAuthorizationService.snapshotReads).isZero();
    }

    private LoginIdentity systemIdentity() {
        return new LoginIdentity(SystemLoginTypes.SYSTEM, "42");
    }

    private SystemAuthorizationSnapshot snapshotWith(String... permissions) {
        return new SystemAuthorizationSnapshot(null, List.of(), List.of(permissions), List.of());
    }

    private static final class TestSystemAuthorizationMapper implements SystemAuthorizationMapper {

        private List<SystemApiResource> apiResources = List.of();
        private int apiResourceReads;

        @Override
        public List<String> findRoleCodesByUserId(long userId) {
            return List.of();
        }

        @Override
        public List<String> findPermissionCodesByUserId(long userId) {
            return List.of();
        }

        @Override
        public List<String> findAllEnabledPermissionCodes() {
            return List.of();
        }

        @Override
        public List<SystemMenu> findAllVisibleMenus() {
            return List.of();
        }

        @Override
        public List<SystemMenu> findVisiblePageMenusByUserId(long userId) {
            return List.of();
        }

        @Override
        public List<SystemRole> findAllEnabledRoles() {
            return List.of();
        }

        @Override
        public SystemRole findRoleById(long roleId) {
            return null;
        }

        @Override
        public List<SystemRoleMenuAssignment> findAllEnabledRoleMenuAssignments() {
            return List.of();
        }

        @Override
        public List<Long> findEnabledVisiblePageMenuIds(List<Long> menuIds) {
            return List.of();
        }

        @Override
        public void deleteRoleMenuAssignments(long roleId) {
        }

        @Override
        public void insertRoleMenuAssignments(long roleId, List<Long> menuIds) {
        }

        @Override
        public List<SystemApiResource> findAllEnabledApiResources() {
            apiResourceReads++;
            return apiResources;
        }
    }

    private static final class TestSystemAuthorizationService extends SystemAuthorizationService {

        private SystemAuthorizationSnapshot snapshot;
        private int snapshotReads;

        private TestSystemAuthorizationService() {
            super(null, null, null);
        }

        @Override
        public SystemAuthorizationSnapshot findSnapshot(long userId) {
            snapshotReads++;
            return snapshot;
        }
    }
}
