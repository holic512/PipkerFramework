/**
 * @file UserManagementService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供系统用户生命周期、角色分配、状态维护、批量删除和密码重置能力。
 * @logic 用户名创建后不可变，创建和更新都要求至少一个启用普通角色；带有 SUPER_ADMIN 角色的保护账户不能由此服务修改、停用、删除或重置密码，全部会影响授权的操作会清除本机授权快照。
 * @dependencies SystemUserMapper、SystemRoleMapper、SystemUserRoleMapper、SecurityCryptoService、SystemAuthorizationCache、Spring Transaction
 * @index_tags user、rbac、role-assignment、password-reset、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipker.business.api.common.mapper.SystemRoleMapper;
import com.pipker.business.api.common.mapper.SystemUserMapper;
import com.pipker.business.api.common.mapper.SystemUserRoleMapper;
import com.pipker.business.api.common.model.SystemRole;
import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.common.model.SystemUserRole;
import com.pipker.business.api.system.authorization.SystemAuthorizationCache;
import com.pipker.business.api.system.authorization.SystemAuthorizationService;
import com.pipker.business.api.system.user.UserManagementRequest.BatchDelete;
import com.pipker.business.api.system.user.UserManagementRequest.BatchStatus;
import com.pipker.business.api.system.user.UserManagementRequest.Create;
import com.pipker.business.api.system.user.UserManagementRequest.ResetPassword;
import com.pipker.business.api.system.user.UserManagementRequest.Update;
import com.pipker.business.api.system.user.UserManagementResponse.AssignableRole;
import com.pipker.business.api.system.user.UserManagementResponse.OperationResult;
import com.pipker.business.api.system.user.UserManagementResponse.PageResult;
import com.pipker.business.api.system.user.UserManagementResponse.UserDetail;
import com.pipker.business.api.system.user.UserManagementResponse.UserRole;
import com.pipker.business.api.system.user.UserManagementResponse.UserSummary;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 系统用户管理服务。 */
@Service
public class UserManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_STATUSES = Set.of("ENABLED", "DISABLED");

    private final SystemUserMapper systemUserMapper;
    private final SystemRoleMapper systemRoleMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SecurityCryptoService securityCryptoService;
    private final SystemAuthorizationCache systemAuthorizationCache;

    /** 创建用户管理服务。 */
    public UserManagementService(
            SystemUserMapper systemUserMapper,
            SystemRoleMapper systemRoleMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SecurityCryptoService securityCryptoService,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemUserMapper = systemUserMapper;
        this.systemRoleMapper = systemRoleMapper;
        this.systemUserRoleMapper = systemUserRoleMapper;
        this.securityCryptoService = securityCryptoService;
        this.systemAuthorizationCache = systemAuthorizationCache;
    }

    /** 分页筛选用户并投影其角色分配。 */
    public PageResult<UserSummary> findUserPage(int page, int pageSize, String keyword, String status) {
        validatePage(page, pageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedStatus = normalizeOptionalStatus(status);
        LambdaQueryWrapper<SystemUser> query = new LambdaQueryWrapper<SystemUser>()
                .eq(normalizedStatus != null, SystemUser::getStatus, normalizedStatus)
                .and(normalizedKeyword != null, wrapper -> wrapper
                        .like(SystemUser::getUsername, normalizedKeyword)
                        .or().like(SystemUser::getNickname, normalizedKeyword)
                        .or().like(SystemUser::getPhone, normalizedKeyword)
                        .or().like(SystemUser::getEmail, normalizedKeyword))
                .orderByDesc(SystemUser::getCreatedAt)
                .orderByDesc(SystemUser::getId);
        Page<SystemUser> userPage = systemUserMapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResult<>(
                userPage.getCurrent(),
                userPage.getSize(),
                userPage.getTotal(),
                userPage.getRecords().stream().map(this::toSummary).toList()
        );
    }

    /** 查询单个用户的安全详情与角色分配。 */
    public UserDetail findUserDetail(String userId) {
        SystemUser user = requireUser(userId);
        List<SystemRole> roles = systemUserRoleMapper.findRolesByUserId(user.getId());
        return new UserDetail(
                stringifyId(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                toRoleViews(roles),
                isProtectedAccount(roles)
        );
    }

    /** 返回可分配给普通用户的启用角色，显式排除框架超级管理员角色。 */
    public List<AssignableRole> findAssignableRoles() {
        return systemRoleMapper.selectList(new LambdaQueryWrapper<SystemRole>()
                        .eq(SystemRole::getStatus, "ENABLED")
                        .ne(SystemRole::getRoleCode, SystemAuthorizationService.SUPER_ADMIN_ROLE)
                        .orderByAsc(SystemRole::getSort)
                        .orderByAsc(SystemRole::getRoleCode))
                .stream()
                .map(role -> new AssignableRole(
                        stringifyId(role.getId()),
                        role.getRoleCode(),
                        role.getRoleName()
                ))
                .toList();
    }

    /** 创建用户、加密初始密码并保存完整角色分配。 */
    @Transactional
    public UserSummary createUser(Create request) {
        String username = request.username().trim();
        if (systemUserMapper.selectCount(new LambdaQueryWrapper<SystemUser>()
                .eq(SystemUser::getUsername, username)) > 0) {
            throw validationError("用户名已存在");
        }
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        List<SystemRole> roles = requireAssignableRoles(roleIds);

        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setPasswordHash(securityCryptoService.hashPassword(request.password()));
        user.setNickname(normalizeOptional(request.nickname()));
        user.setPhone(normalizeOptional(request.phone()));
        user.setEmail(normalizeOptional(request.email()));
        user.setStatus(normalizeRequiredStatus(request.status()));
        systemUserMapper.insert(user);
        replaceUserRoles(user.getId(), roleIds);
        systemAuthorizationCache.invalidateAllSnapshots();
        return toSummary(user, roles);
    }

    /** 更新用户资料与完整角色分配；用户名不可修改。 */
    @Transactional
    public UserSummary updateUser(String userId, Update request) {
        SystemUser user = requireMutableUser(userId);
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        List<SystemRole> roles = requireAssignableRoles(roleIds);

        user.setNickname(normalizeOptional(request.nickname()));
        user.setPhone(normalizeOptional(request.phone()));
        user.setEmail(normalizeOptional(request.email()));
        user.setStatus(normalizeRequiredStatus(request.status()));
        user.setUpdatedAt(LocalDateTime.now());
        systemUserMapper.updateById(user);
        replaceUserRoles(user.getId(), roleIds);
        systemAuthorizationCache.invalidateAllSnapshots();
        return toSummary(user, roles);
    }

    /** 批量启用或停用普通用户。 */
    @Transactional
    public OperationResult updateUserStatus(BatchStatus request) {
        List<Long> userIds = normalizeUserIds(request.userIds());
        assertMutableUsers(userIds);
        systemUserMapper.update(null, new LambdaUpdateWrapper<SystemUser>()
                .in(SystemUser::getId, userIds)
                .set(SystemUser::getStatus, normalizeRequiredStatus(request.status()))
                .set(SystemUser::getUpdatedAt, LocalDateTime.now()));
        systemAuthorizationCache.invalidateAllSnapshots();
        return new OperationResult(stringifyIds(userIds));
    }

    /** 删除普通用户与其角色关联。 */
    @Transactional
    public OperationResult deleteUsers(BatchDelete request) {
        List<Long> userIds = normalizeUserIds(request.userIds());
        assertMutableUsers(userIds);
        // SQLite 连接可能未启用 foreign_keys；显式清理保持三种数据库的结果一致。
        systemUserRoleMapper.delete(new LambdaQueryWrapper<SystemUserRole>()
                .in(SystemUserRole::getUserId, userIds));
        systemUserMapper.deleteByIds(userIds);
        systemAuthorizationCache.invalidateAllSnapshots();
        return new OperationResult(stringifyIds(userIds));
    }

    /** 重新加密并覆盖普通用户的登录密码。 */
    @Transactional
    public OperationResult resetPassword(String userId, ResetPassword request) {
        SystemUser user = requireMutableUser(userId);
        user.setPasswordHash(securityCryptoService.hashPassword(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        systemUserMapper.updateById(user);
        return new OperationResult(List.of(stringifyId(user.getId())));
    }

    private SystemUser requireUser(String rawUserId) {
        long userId = parsePositiveId(rawUserId, "userId");
        SystemUser user = systemUserMapper.selectById(userId);
        if (user == null) {
            throw validationError("用户不存在");
        }
        return user;
    }

    private SystemUser requireMutableUser(String rawUserId) {
        SystemUser user = requireUser(rawUserId);
        if (isProtectedAccount(systemUserRoleMapper.findRolesByUserId(user.getId()))) {
            throw validationError("SUPER_ADMIN 账户受框架保护，不能在用户管理中修改");
        }
        return user;
    }

    private void assertMutableUsers(List<Long> userIds) {
        List<SystemUser> users = systemUserMapper.selectByIds(userIds);
        if (users.size() != userIds.size()) {
            throw validationError("包含不存在的用户");
        }
        if (users.stream().anyMatch(user -> isProtectedAccount(
                systemUserRoleMapper.findRolesByUserId(user.getId())))) {
            throw validationError("SUPER_ADMIN 账户受框架保护，不能批量处理");
        }
    }

    private List<SystemRole> requireAssignableRoles(List<Long> roleIds) {
        List<SystemRole> roles = systemRoleMapper.selectByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw validationError("角色分配中包含不存在的角色");
        }
        if (roles.stream().anyMatch(role -> !role.isEnabled())) {
            throw validationError("只能分配已启用的角色");
        }
        if (roles.stream().anyMatch(this::isProtectedRole)) {
            throw validationError("SUPER_ADMIN 是框架保护角色，不能通过用户管理分配");
        }
        return roles;
    }

    private void replaceUserRoles(long userId, List<Long> roleIds) {
        systemUserRoleMapper.delete(new LambdaQueryWrapper<SystemUserRole>()
                .eq(SystemUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SystemUserRole assignment = new SystemUserRole();
            assignment.setUserId(userId);
            assignment.setRoleId(roleId);
            systemUserRoleMapper.insert(assignment);
        }
    }

    private boolean isProtectedAccount(List<SystemRole> roles) {
        return roles.stream().anyMatch(this::isProtectedRole);
    }

    private boolean isProtectedRole(SystemRole role) {
        return SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(role.getRoleCode());
    }

    private List<Long> normalizeUserIds(List<String> rawUserIds) {
        if (rawUserIds == null || rawUserIds.isEmpty()) {
            throw validationError("userIds must not be empty");
        }
        LinkedHashSet<Long> userIds = new LinkedHashSet<>();
        for (String rawUserId : rawUserIds) {
            userIds.add(parsePositiveId(rawUserId, "userIds"));
        }
        return List.copyOf(userIds);
    }

    private List<Long> normalizeRoleIds(List<String> rawRoleIds) {
        if (rawRoleIds == null || rawRoleIds.isEmpty()) {
            throw validationError("roleIds must not be empty");
        }
        LinkedHashSet<Long> roleIds = new LinkedHashSet<>();
        for (String rawRoleId : rawRoleIds) {
            roleIds.add(parsePositiveId(rawRoleId, "roleIds"));
        }
        return List.copyOf(roleIds);
    }

    private long parsePositiveId(String rawId, String fieldName) {
        try {
            long id = Long.parseLong(rawId);
            if (id <= 0) {
                throw validationError(fieldName + " must be a positive Long ID");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw validationError(fieldName + " must be a positive Long ID");
        }
    }

    private void validatePage(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw validationError("page must be positive and pageSize must be between 1 and 100");
        }
    }

    private String normalizeOptionalStatus(String status) {
        String normalized = normalizeOptional(status);
        return normalized == null ? null : normalizeRequiredStatus(normalized);
    }

    private String normalizeRequiredStatus(String status) {
        String normalized = normalizeOptional(status);
        if (normalized == null || !VALID_STATUSES.contains(normalized)) {
            throw validationError("status must be ENABLED or DISABLED");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private UserSummary toSummary(SystemUser user) {
        return toSummary(user, systemUserRoleMapper.findRolesByUserId(user.getId()));
    }

    private UserSummary toSummary(SystemUser user, List<SystemRole> roles) {
        return new UserSummary(
                stringifyId(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getCreatedAt(),
                toRoleViews(roles),
                isProtectedAccount(roles)
        );
    }

    private List<UserRole> toRoleViews(List<SystemRole> roles) {
        return roles.stream()
                .map(role -> new UserRole(
                        stringifyId(role.getId()),
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getStatus()
                ))
                .toList();
    }

    private List<String> stringifyIds(List<Long> ids) {
        return ids.stream().map(this::stringifyId).collect(Collectors.toList());
    }

    private String stringifyId(long id) {
        return String.valueOf(id);
    }

    private ApiBusinessException validationError(String message) {
        return new ApiBusinessException(CommonApiCode.VALIDATION_FAILED, message);
    }
}
