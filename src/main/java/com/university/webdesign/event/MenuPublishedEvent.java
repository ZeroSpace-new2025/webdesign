package com.university.webdesign.event;

import java.time.LocalDate;

/**
 * 菜单发布事件（M1 发布，M2 订阅）。
 * <p>
 * 对应《对外方法表》2.3：`MenuService.publish` 成功后发布，载荷
 * `menuId`、`version`、`effectiveDate`、快照明细。订单核心据此刷新当日菜单缓存。
 * 监听方请使用 {@code @TransactionalEventListener(phase = AFTER_COMMIT)}。
 *
 * @param menuId        菜单ID
 * @param version       发布时的菜单版本号
 * @param effectiveDate 生效日期
 * @param snapshotCount 冻结的快照条数
 */
public record MenuPublishedEvent(Long menuId, Integer version, LocalDate effectiveDate, int snapshotCount)
{
}
