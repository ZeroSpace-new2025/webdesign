package com.university.webdesign.config;

import com.university.webdesign.common.enums.PermissionEnum;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式权限点校验注解。
 * <p>
 * 对应《对外方法表》1.5 与第八章：api 层用本注解声明所需权限点，
 * 由 {@link AuthInterceptor} 统一校验，越权抛 40300；service 层仍保留数据行级归属校验
 * （“本人 or 经理”），两者互补——注解拦得住入口，行级校验拦得住越权取数。
 * <p>
 * 用法：{@code @RequiresPerm(PermissionEnum.ORDER_INVALIDATE)}；
 * 取值直接写 {@link PermissionEnum} 常量（注解值必须是编译期常量，
 * 因此不能再写 {@code PermissionEnum.X.getPermCode()}）。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPerm
{
	/**
	 * 所需权限点，满足其一即放行
	 *
	 * @return 权限点数组
	 */
	PermissionEnum[] value();

	/**
	 * 所需角色名称，与 {@link #value()} 是“或”的关系；为空表示只校验权限点
	 *
	 * @return 角色名称数组
	 */
	String[] roles() default {};
}
