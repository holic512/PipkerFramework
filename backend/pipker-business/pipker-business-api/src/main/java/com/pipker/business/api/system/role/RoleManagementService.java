/**
 * @file RoleManagementService.java
 * @project Pipker Framework
 * @module Pipker Business API
 * @description 提供系统角色生命周期、批量状态处理、成员查询与成员密码重置能力。
 * @logic 角色创建后编码不可变，SUPER_ADMIN 永远不能被改动或删除；所有影响授权的角色操作均清除本机授权快照。
 * @dependencies SystemRoleMapper、SystemUserRoleMapper、SystemRolePermissionMapper、SystemRoleMenuMapper、SystemAccountService、SecurityCryptoService、SystemAuthorizationCache、Spring Transaction
 * @index_tags rbac、role、user、password-reset、pagination、administration
 * @author holic512
 */
package com.pipker.business.api.system.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pipker.business.api.common.mapper.SystemRoleMapper;
import com.pipker.business.api.common.mapper.SystemRoleMenuMapper;
import com.pipker.business.api.common.mapper.SystemRolePermissionMapper;
import com.pipker.business.api.common.mapper.SystemUserRoleMapper;
import com.pipker.business.api.common.model.SystemAuthorizationSnapshot;
import com.pipker.business.api.common.model.SystemRole;
import com.pipker.business.api.common.model.SystemUser;
import com.pipker.business.api.common.model.SystemUserRole;
import com.pipker.business.api.common.model.SystemRoleMenu;
import com.pipker.business.api.common.model.SystemRolePermission;
import com.pipker.business.api.system.auth.CurrentSystemAuthorizationService;
import com.pipker.business.api.system.authorization.SystemAuthorizationCache;
import com.pipker.business.api.system.authorization.SystemAuthorizationService;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchDelete;
import com.pipker.business.api.system.role.RoleManagementRequest.BatchStatus;
import com.pipker.business.api.system.role.RoleManagementRequest.Create;
import com.pipker.business.api.system.role.RoleManagementRequest.ResetMemberPassword;
import com.pipker.business.api.system.role.RoleManagementRequest.Update;
import com.pipker.business.api.system.role.RoleManagementResponse.OperationResult;
import com.pipker.business.api.system.role.RoleManagementResponse.PageResult;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleDetail;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleMember;
import com.pipker.business.api.system.role.RoleManagementResponse.RoleSummary;
import com.pipker.business.api.system.user.SystemAccountService;
import com.pipker.business.common.api.CommonApiCode;
import com.pipker.business.common.exception.ApiBusinessException;
import com.pipker.starter.security.service.SecurityCryptoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** 系统角色管理应用服务。 */
@Service
public class RoleManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_STATUSES = Set.of("ENABLED", "DISABLED");

    private final SystemRoleMapper systemRoleMapper;
    private final SystemUserRoleMapper systemUserRoleMapper;
    private final SystemRolePermissionMapper systemRolePermissionMapper;
    private final SystemRoleMenuMapper systemRoleMenuMapper;
    private final SystemAccountService systemAccountService;
    private final SecurityCryptoService securityCryptoService;
    private final CurrentSystemAuthorizationService currentSystemAuthorizationService;
    private final SystemAuthorizationCache systemAuthorizationCache;

    /** 创建角色管理服务。 */
    public RoleManagementService(
            SystemRoleMapper systemRoleMapper,
            SystemUserRoleMapper systemUserRoleMapper,
            SystemRolePermissionMapper systemRolePermissionMapper,
            SystemRoleMenuMapper systemRoleMenuMapper,
            SystemAccountService systemAccountService,
            SecurityCryptoService securityCryptoService,
            CurrentSystemAuthorizationService currentSystemAuthorizationService,
            SystemAuthorizationCache systemAuthorizationCache
    ) {
        this.systemRoleMapper = systemRoleMapper;
        this.systemUserRoleMapper = systemUserRoleMapper;
        this.systemRolePermissionMapper = systemRolePermissionMapper;
        this.systemRoleMenuMapper = systemRoleMenuMapper;
        this.systemAccountService = systemAccountService;
        this.securityCryptoService = securityCryptoService;
        this.currentSystemAuthorizationService = currentSystemAuthorizationService;
        this.systemAuthorizationCache = systemAuthorizationCache;
    }

    /** 分页筛选角色。 */
    public PageResult<RoleSummary> findRolePage(int page, int pageSize, String keyword, String status) {
        validatePage(page, pageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        String normalizedStatus = normalizeOptionalStatus(status);

        LambdaQueryWrapper<SystemRole> query = new LambdaQueryWrapper<SystemRole>()
                .eq(normalizedStatus != null, SystemRole::getStatus, normalizedStatus)
                .and(normalizedKeyword != null, wrapper -> wrapper
                        .like(SystemRole::getRoleCode, normalizedKeyword)
                        .or()
                        .like(SystemRole::getRoleName, normalizedKeyword))
                .orderByAsc(SystemRole::getSort)
                .orderByAsc(SystemRole::getRoleCode);
        Page<SystemRole> rolePage = systemRoleMapper.selectPage(new Page<>(page, pageSize), query);
        return new PageResult<>(
                rolePage.getCurrent(),
                rolePage.getSize(),
                rolePage.getTotal(),
                rolePage.getRecords().stream().map(this::toSummary).toList()
        );
    }

    /** 返回指定角色和它关联的成员数量。 */
    public RoleDetail findRoleDetail(String roleId) {
        SystemRole role = requireRole(roleId);
        long memberCount = systemUserRoleMapper.selectCount(new LambdaQueryWrapper<SystemUserRole>()
                .eq(SystemUserRole::getRoleId, role.getId()));
        return new RoleDetail(
                stringifyId(role.getId()),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                role.getStatus(),
                role.getSort(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                memberCount
        );
    }

    /** 创建普通系统角色。 */
    @Transactional
    public RoleSummary createRole(Create request) {
        String roleCode = request.roleCode().trim().toUpperCase(Locale.ROOT);
        if (SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(roleCode)) {
            throw validationError("SUPER_ADMIN is a framework-reserved role code");
        }
        if (systemRoleMapper.selectCount(new LambdaQueryWrapper<SystemRole>()
                .eq(SystemRole::getRoleCode, roleCode)) > 0) {
            throw validationError("角色编码已存在");
        }

        SystemRole role = new SystemRole();
        role.setRoleCode(roleCode);
        role.setRoleName(request.roleName().trim());
        role.setDescription(normalizeOptional(request.description()));
        role.setStatus(normalizeRequiredStatus(request.status()));
        role.setSort(normalizeSort(request.sort()));
        systemRoleMapper.insert(role);
        systemAuthorizationCache.invalidateAllSnapshots();
        return toSummary(role);
    }

    /** 编辑角色名称、描述、状态与排序；角色编码始终不可编辑。 */
    @Transactional
    public RoleSummary updateRole(String roleId, Update request) {
        SystemRole role = requireRole(roleId);
        rejectProtectedRole(role);

        role.setRoleName(request.roleName().trim());
        role.setDescription(normalizeOptional(request.description()));
        role.setStatus(normalizeRequiredStatus(request.status()));
        role.setSort(normalizeSort(request.sort()));
        role.setUpdatedAt(LocalDateTime.now());
        systemRoleMapper.updateById(role);
        systemAuthorizationCache.invalidateAllSnapshots();
        return toSummary(role);
    }

    /** 批量启用或停用普通角色。 */
    @Transactional
    public OperationResult updateRoleStatus(BatchStatus request) {
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        assertEditableRoles(roleIds);
        systemRoleMapper.update(null, new LambdaUpdateWrapper<SystemRole>()
                .in(SystemRole::getId, roleIds)
                .set(SystemRole::getStatus, normalizeRequiredStatus(request.status()))
                .set(SystemRole::getUpdatedAt, LocalDateTime.now()));
        systemAuthorizationCache.invalidateAllSnapshots();
        return new OperationResult(stringifyIds(roleIds));
    }

    /** 批量删除普通角色及其授权关联记录。 */
    @Transactional
    public OperationResult deleteRoles(BatchDelete request) {
        List<Long> roleIds = normalizeRoleIds(request.roleIds());
        assertEditableRoles(roleIds);
        // SQLite 驱动可在未启用 foreign_keys 的连接上运行；显式清理使三种数据库语义一致。
        systemUserRoleMapper.delete(new LambdaQueryWrapper<SystemUserRole>()
                .in(SystemUserRole::getRoleId, roleIds));
        systemRolePermissionMapper.delete(new LambdaQueryWrapper<SystemRolePermission>()
                .in(SystemRolePermission::getRoleId, roleIds));
        systemRoleMenuMapper.delete(new LambdaQueryWrapper<SystemRoleMenu>()
                .in(SystemRoleMenu::getRoleId, roleIds));
        systemRoleMapper.deleteByIds(roleIds);
        systemAuthorizationCache.invalidateAllSnapshots();
        return new OperationResult(stringifyIds(roleIds));
    }

    /** 分页读取一个角色下可操作的成员账户。 */
    public PageResult<RoleMember> findMemberPage(
            String roleId,
            int page,
            int pageSize,
            String keyword
    ) {
        SystemRole role = requireRole(roleId);
        validatePage(page, pageSize);
        String normalizedKeyword = normalizeOptional(keyword);
        Page<SystemUser> memberPage = systemUserRoleMapper.findMemberPageByRoleId(
                new Page<>(page, pageSize),
                role.getId(),
                normalizedKeyword == null ? null : "%" + normalizedKeyword + "%"
        );
        return new PageResult<>(
                memberPage.getCurrent(),
                memberPage.getSize(),
                memberPage.getTotal(),
                memberPage.getRecords().stream().map(this::toMember).toList()
        );
    }

    /** 重置指定角色成员的密码，且非超级管理员不能重置超级管理员账户。 */
    @Transactional
    public OperationResult resetMemberPassword(
            String roleId,
            String userId,
            ResetMemberPassword request
    ) {
        SystemRole role = requireRole(roleId);
        long parsedUserId = parsePositiveId(userId, "userId");
        long membershipCount = systemUserRoleMapper.selectCount(new LambdaQueryWrapper<SystemUserRole>()
                .eq(SystemUserRole::getRoleId, role.getId())
                .eq(SystemUserRole::getUserId, parsedUserId));
        if (membershipCount == 0) {
            throw validationError("账户不属于当前角色");
        }

        SystemUser user = systemAccountService.findById(parsedUserId);
        if (user == null) {
            throw validationError("账户不存在");
        }
        rejectSuperAdminAccountReset(parsedUserId);
        systemAccountService.updatePasswordHash(
                parsedUserId,
                securityCryptoService.hashPassword(request.newPassword()),
                LocalDateTime.now()
        );
        return new OperationResult(List.of(stringifyId(parsedUserId)));
    }

    private void rejectSuperAdminAccountReset(long userId) {
        boolean targetIsSuperAdmin = systemUserRoleMapper.findEnabledRoleCodesByUserId(userId)
                .contains(SystemAuthorizationService.SUPER_ADMIN_ROLE);
        if (!targetIsSuperAdmin) {
            return;
        }
        SystemAuthorizationSnapshot currentSnapshot = currentSystemAuthorizationService.currentSnapshot();
        if (!currentSnapshot.roles().contains(SystemAuthorizationService.SUPER_ADMIN_ROLE)) {
            throw validationError("只有 SUPER_ADMIN 可以重置超级管理员账户密码");
        }
    }

    private SystemRole requireRole(String rawRoleId) {
        long roleId = parsePositiveId(rawRoleId, "roleId");
        SystemRole role = systemRoleMapper.selectById(roleId);
        if (role == null) {
            throw validationError("角色不存在");
        }
        return role;
    }

    private void assertEditableRoles(List<Long> roleIds) {
        List<SystemRole> roles = systemRoleMapper.selectList(new LambdaQueryWrapper<SystemRole>()
                .in(SystemRole::getId, roleIds));
        if (roles.size() != roleIds.size()) {
            throw validationError("包含不存在的角色");
        }
        if (roles.stream().anyMatch(this::isProtectedRole)) {
            throw validationError("SUPER_ADMIN 是框架保护角色，不能批量处理");
        }
    }

    private void rejectProtectedRole(SystemRole role) {
        if (isProtectedRole(role)) {
            throw validationError("SUPER_ADMIN 是框架保护角色，不能修改");
        }
    }

    private boolean isProtectedRole(SystemRole role) {
        return SystemAuthorizationService.SUPER_ADMIN_ROLE.equals(role.getRoleCode());
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
        if (normalized == null) {
            return null;
        }
        return normalizeRequiredStatus(normalized);
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

    private int normalizeSort(Integer sort) {
        if (sort == null || sort < 0) {
            throw validationError("sort must be zero or greater");
        }
        return sort;
    }

    private RoleSummary toSummary(SystemRole role) {
        return new RoleSummary(
                stringifyId(role.getId()),
                role.getRoleCode(),
                role.getRoleName(),
                role.getDescription(),
                role.getStatus(),
                role.getSort(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }

    private RoleMember toMember(SystemUser user) {
        return new RoleMember(
                stringifyId(user.getId()),
                user.getUsername(),
                user.getNickname(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getCreatedAt()
        );
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
