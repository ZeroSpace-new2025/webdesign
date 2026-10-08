package com.university.webdesign.config;

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
 * 用法：{@code @RequiresPerm(PermCodes.ORDER_INVALIDATE)}
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPerm
{
	/**
	 * 所需权限点编码，满足其一即放行（见 {@code PermCodes}）
	 *
	 * @return 权限点编码数组
	 */
	String[] value();

	/**
	 * 所需角色编码，与 {@link #value()} 是“或”的关系；为空表示只校验权限点
	 *
	 * @return 角色编码数组
	 */
	String[] roles() default {};
}
