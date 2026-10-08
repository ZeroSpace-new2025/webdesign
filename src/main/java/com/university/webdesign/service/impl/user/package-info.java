package com.university.webdesign.service.impl.user;

/**
 * 用户与报表中心（M4）——用户/角色/认证服务的实现包。
 * <p>
 * 实现类：{@link UserServiceImpl}、{@link RoleServiceImpl}、{@link AuthServiceImpl}；
 * {@link UserImportParser} 是员工批量导入的 CSV/XLSX 解析器；
 * {@code support} 子包放 JWT 编解码等实现细节。
 * <p>
 * 事务统一为普通 {@code @Transactional}（查询也不加 {@code readOnly = true}）。
 */
