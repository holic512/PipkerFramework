package com.pipker.business.api.system.authorization;

import com.pipker.business.api.common.mapper.SystemApiResourceMapper;
import com.pipker.business.api.common.model.SystemApiResourceRule;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.auth.dto.SystemLoginTypes;
import com.pipker.business.common.auth.LoginIdentity;
import com.pipker.starter.satoken.config.PipkerAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SystemApiAuthorizationServiceTests {

    private TestSystemApiResourceMapper testSystemApiResourceMapper;
    private TestSystemAuthorizationService systemAuthorizationService;
    private SystemApiAuthorizationService systemApiAuthorizationService;

    @BeforeEach
    void setUp() {
        testSystemApiResourceMapper = new TestSystemApiResourceMapper();
        systemAuthorizationService = new TestSystemAuthorizationService();
        PipkerAuthProperties authProperties = new PipkerAuthProperties();
        authProperties.setAuthorizationCacheTtl(Duration.ofSeconds(60));
        authProperties.setAuthorizationCacheMaximumSize(10);
        systemApiAuthorizationService = new SystemApiAuthorizationService(
                testSystemApiResourceMapper.mapper,
                systemAuthorizationService,
                new SystemAuthorizationCache(authProperties)
        );
    }

    @Test
    void grantsOnlyAUniqueMethodAndMvcTemplateMatchWithTheRequiredPermission() {
        testSystemApiResourceMapper.apiResourceRules = List.of(
                new SystemApiResourceRule(1L, "widget:read", "GET", "/api/widgets/{id}")
        );
        systemAuthorizationService.snapshot = snapshotWith("widget:read");

        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/widgets/7"))
                .isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "POST", "/api/widgets/7"))
                .isFalse();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/widgets"))
                .isFalse();

        assertThat(testSystemApiResourceMapper.apiResourceReads).isEqualTo(1);
    }

    @Test
    void deniesOverlappingRulesBeforeReadingTheUserPermissionSnapshot() {
        testSystemApiResourceMapper.apiResourceRules = List.of(
                new SystemApiResourceRule(1L, "widget:read", "GET", "/api/widgets/{id}"),
                new SystemApiResourceRule(2L, "widget:all", "GET", "/api/widgets/*")
        );

        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/widgets/7"))
                .isFalse();

        assertThat(systemAuthorizationService.snapshotReads).isZero();
    }

    @Test
    void grantsEachRoleManagementOperationThroughOneUnambiguousRule() {
        testSystemApiResourceMapper.apiResourceRules = List.of(
                new SystemApiResourceRule(1L, "system:role:manage", "GET", "/api/admin/roles"),
                new SystemApiResourceRule(2L, "system:role:manage", "POST", "/api/admin/roles"),
                new SystemApiResourceRule(3L, "system:role:manage", "GET", "/api/admin/roles/{roleId}"),
                new SystemApiResourceRule(4L, "system:role:manage", "PUT", "/api/admin/roles/{roleId}"),
                new SystemApiResourceRule(5L, "system:role:manage", "DELETE", "/api/admin/roles/{roleId}"),
                new SystemApiResourceRule(6L, "system:role:manage", "PATCH", "/api/admin/roles/batch-status"),
                new SystemApiResourceRule(7L, "system:role:manage", "POST", "/api/admin/roles/batch-delete"),
                new SystemApiResourceRule(8L, "system:role:manage", "GET", "/api/admin/roles/{roleId}/members"),
                new SystemApiResourceRule(9L, "system:role:manage", "PUT", "/api/admin/roles/{roleId}/members/{userId}/password")
        );
        systemAuthorizationService.snapshot = snapshotWith("system:role:manage");

        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/admin/roles")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "POST", "/api/admin/roles")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/admin/roles/42")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "PUT", "/api/admin/roles/42")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "DELETE", "/api/admin/roles/42")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "PATCH", "/api/admin/roles/batch-status")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "POST", "/api/admin/roles/batch-delete")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/admin/roles/42/members")).isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(
                systemIdentity(),
                "PUT",
                "/api/admin/roles/42/members/7/password"
        )).isTrue();
    }

    @Test
    void grantsEachReadOnlyRouteManagementOperationThroughOneUnambiguousRule() {
        testSystemApiResourceMapper.apiResourceRules = List.of(
                new SystemApiResourceRule(1L, "system:route:read", "GET", "/api/admin/routes"),
                new SystemApiResourceRule(2L, "system:route:read", "GET", "/api/admin/routes/{routeId}")
        );
        systemAuthorizationService.snapshot = snapshotWith("system:route:read");

        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/admin/routes"))
                .isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "GET", "/api/admin/routes/42"))
                .isTrue();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "POST", "/api/admin/routes"))
                .isFalse();
        assertThat(systemApiAuthorizationService.isAuthorized(systemIdentity(), "PUT", "/api/admin/routes/42"))
                .isFalse();
    }

    private LoginIdentity systemIdentity() {
        return new LoginIdentity(SystemLoginTypes.SYSTEM, "42");
    }

    private SystemAuthorizationSnapshot snapshotWith(String... permissions) {
        return new SystemAuthorizationSnapshot(null, List.of(), List.of(permissions), List.of(), List.of());
    }

    private static final class TestSystemApiResourceMapper {

        private List<SystemApiResourceRule> apiResourceRules = List.of();
        private int apiResourceReads;

        private final SystemApiResourceMapper mapper = (SystemApiResourceMapper) Proxy.newProxyInstance(
                SystemApiResourceMapper.class.getClassLoader(),
                new Class<?>[]{SystemApiResourceMapper.class},
                (proxy, method, arguments) -> {
                    if ("findAllEnabledAuthorizationRules".equals(method.getName())) {
                        apiResourceReads++;
                        return apiResourceRules;
                    }
                    if ("toString".equals(method.getName())) {
                        return "TestSystemApiResourceMapper";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == arguments[0];
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static final class TestSystemAuthorizationService extends SystemAuthorizationService {

        private SystemAuthorizationSnapshot snapshot;
        private int snapshotReads;

        private TestSystemAuthorizationService() {
            super(null, null, null, null, null);
        }

        @Override
        public SystemAuthorizationSnapshot findSnapshot(long userId) {
            snapshotReads++;
            return snapshot;
        }
    }
}
