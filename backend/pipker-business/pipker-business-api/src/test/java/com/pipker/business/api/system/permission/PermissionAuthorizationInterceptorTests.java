package com.pipker.business.api.system.permission;

import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.system.auth.CurrentSystemAuthorizationService;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionAuthorizationInterceptorTests {

    private TestCurrentSystemAuthorizationService currentAuthorizationService;
    private PermissionAuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        currentAuthorizationService = new TestCurrentSystemAuthorizationService();
        interceptor = new PermissionAuthorizationInterceptor(currentAuthorizationService);
    }

    @Test
    void allowsAnAnnotatedHandlerWhenTheCurrentSnapshotHasItsEnumPermission() throws Exception {
        currentAuthorizationService.snapshot = snapshotWith(PermissionEnum.SYSTEM_ROLE_MANAGE.getCode());

        assertThat(interceptor.preHandle(null, null, handler("manageRole"))).isTrue();
        assertThat(currentAuthorizationService.snapshotReads).isEqualTo(1);
    }

    @Test
    void rejectsAnAnnotatedHandlerWhenTheRoleDoesNotHaveItsEnumPermission() throws Exception {
        currentAuthorizationService.snapshot = snapshotWith(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode());

        assertThatThrownBy(() -> interceptor.preHandle(null, null, handler("manageRole")))
                .isInstanceOf(ApiBusinessException.class)
                .satisfies(exception -> assertThat(((ApiBusinessException) exception).getCode())
                        .isEqualTo(CommonApiCode.AUTH_FORBIDDEN));
    }

    @Test
    void leavesAnUnannotatedHandlerWithoutAdditionalPermissionPointRestriction() throws Exception {
        assertThat(interceptor.preHandle(null, null, handler("unrestricted"))).isTrue();
        assertThat(currentAuthorizationService.snapshotReads).isZero();
    }

    @Test
    void recognizesControllerLevelPermissionWhenTheMethodHasNoDirectAnnotation() throws Exception {
        currentAuthorizationService.snapshot = snapshotWith(PermissionEnum.SYSTEM_ROUTE_VIEW.getCode());
        HandlerMethod handlerMethod = new HandlerMethod(
                new RouteControllerFixture(),
                RouteControllerFixture.class.getDeclaredMethod("routePage")
        );

        assertThat(interceptor.preHandle(null, null, handlerMethod)).isTrue();
    }

    @Test
    void rejectsHistoricalDatabaseCodesThatAreNoLongerDefinedByTheCurrentEnum() throws Exception {
        currentAuthorizationService.snapshot = snapshotWith("retired-system-role-manage");

        assertThatThrownBy(() -> interceptor.preHandle(null, null, handler("manageRole")))
                .isInstanceOf(ApiBusinessException.class)
                .satisfies(exception -> assertThat(((ApiBusinessException) exception).getCode())
                        .isEqualTo(CommonApiCode.AUTH_FORBIDDEN));
    }

    private HandlerMethod handler(String name) throws NoSuchMethodException {
        Method method = ControllerFixture.class.getDeclaredMethod(name);
        return new HandlerMethod(new ControllerFixture(), method);
    }

    private SystemAuthorizationSnapshot snapshotWith(String... permissions) {
        return new SystemAuthorizationSnapshot(null, List.of(), List.of(permissions), List.of(), List.of());
    }

    private static final class ControllerFixture {

        @Permission(PermissionEnum.SYSTEM_ROLE_MANAGE)
        void manageRole() {
        }

        void unrestricted() {
        }
    }

    @Permission(PermissionEnum.SYSTEM_ROUTE_VIEW)
    private static final class RouteControllerFixture {

        void routePage() {
        }
    }

    private static final class TestCurrentSystemAuthorizationService extends CurrentSystemAuthorizationService {

        private SystemAuthorizationSnapshot snapshot;
        private int snapshotReads;

        private TestCurrentSystemAuthorizationService() {
            super(null, null);
        }

        @Override
        public SystemAuthorizationSnapshot currentSnapshot() {
            snapshotReads++;
            return snapshot;
        }
    }
}
