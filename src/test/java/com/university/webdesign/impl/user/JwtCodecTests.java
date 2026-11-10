package com.university.webdesign.impl.user;

import com.university.webdesign.impl.user.support.JwtCodec;
import com.university.webdesign.impl.user.support.JwtPayload;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * JWT 编解码单元测试。
 * <p>
 * 重点回归：载荷里的时间只有秒级精度，如果每次签发不额外带唯一标识，
 * 同一秒内为同一用户签发的令牌会得到**完全相同的字符串**。黑名单按令牌字符串记账，
 * 于是“登出后立刻重新登录”“改密后立刻重新登录”拿到的新令牌会命中旧黑名单，刚登录就 40100。
 */
class JwtCodecTests
{
	/**
	 * 同一秒内签发的令牌必须互不相同
	 */
	@Test
	void tokensSignedInTheSameSecondAreUnique() {
		JwtCodec codec = new JwtCodec("unit-test-secret", 60L);
		String first = codec.sign(5L, "zhangsan", "张三", 5L,
				List.of("EMPLOYEE"), List.of("order:submit"));
		String second = codec.sign(5L, "zhangsan", "张三", 5L,
				List.of("EMPLOYEE"), List.of("order:submit"));

		assertNotEquals(first, second, "同一秒内两次签发的令牌不能是同一个字符串");

		JwtPayload firstPayload = codec.parse(first);
		JwtPayload secondPayload = codec.parse(second);
		assertEquals(5L, firstPayload.userId());
		assertEquals(5L, secondPayload.userId());
		assertEquals(firstPayload.issuedAt(), secondPayload.issuedAt(), "两条令牌应在同一秒签发");
	}

	/**
	 * 签名被篡改时必须拒绝
	 */
	@Test
	void tamperedTokenIsRejected() {
		JwtCodec codec = new JwtCodec("unit-test-secret", 60L);
		String token = codec.sign(5L, "zhangsan", "张三", 5L,
				List.of("EMPLOYEE"), List.of("order:submit"));
		String tampered = token.substring(0, token.length() - 2) + "aa";
		org.junit.jupiter.api.Assertions.assertThrows(
				com.university.webdesign.common.BusinessException.class, () -> codec.parse(tampered));
	}
}
