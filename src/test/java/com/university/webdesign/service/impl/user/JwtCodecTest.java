package com.university.webdesign.service.impl.user;

import com.university.webdesign.common.UserContext;
import com.university.webdesign.service.impl.user.support.JwtCodec;
import com.university.webdesign.service.impl.user.support.JwtPayload;
import com.university.webdesign.service.user.dto.CurrentUserVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 编解码器单元测试。
 * <p>
 * 覆盖 M4 的关键约定：HS256 手写编解码、payload 含
 * {@code userId / employeeNo / name / deptId / roles / permCodes / exp}、
 * 签名或格式错误一律抛 40100。
 */
class JwtCodecTest
{
	private static final String SECRET = "unit-test-secret-key-for-jwt-codec-0123456789";

	/**
	 * 构造被测编解码器
	 *
	 * @param expireMinutes 有效期（分钟）
	 * @return 编解码器
	 */
	private JwtCodec codec(long expireMinutes) {
		return new JwtCodec(SECRET, expireMinutes);
	}

	@Test
	@DisplayName("签发后再解析：payload 各字段原样保留")
	void shouldRoundTripPayload() {
		JwtCodec codec = codec(60);

		String token = codec.sign(7L, "E1001", "张三", 10L,
				List.of("MANAGER", "FINANCE"), List.of("report:view"));

		assertThat(token).contains(".");
		assertThat(token.split("\\.")).hasSize(3);
		JwtPayload payload = codec.parse(token);
		assertThat(payload.userId()).isEqualTo(7L);
		assertThat(payload.employeeNo()).isEqualTo("E1001");
		assertThat(payload.name()).isEqualTo("张三");
		assertThat(payload.deptId()).isEqualTo(10L);
		assertThat(payload.roles()).containsExactly("MANAGER", "FINANCE");
		assertThat(payload.permCodes()).containsExactly("report:view");
		assertThat(payload.expiresAt()).isGreaterThan(payload.issuedAt());
	}

	@Test
	@DisplayName("兼容 Bearer 前缀")
	void shouldUnifyBearerPrefix() {
		JwtCodec codec = codec(60);
		String token = codec.sign(1L, "E1", "李四", 2L, List.of(), List.of());

		assertThat(codec.parse("Bearer " + token).userId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("签名被篡改：抛 40100")
	void shouldRejectTamperedToken() {
		JwtCodec codec = codec(60);
		String token = codec.sign(1L, "E1", "李四", 2L, List.of(), List.of());
		String tampered = token.substring(0, token.length() - 2) + "xx";

		assertThatThrownBy(() -> codec.parse(tampered))
				.isInstanceOf(com.university.webdesign.common.BusinessException.class)
				.hasMessageContaining("签名");
	}

	@Test
	@DisplayName("令牌带未来到期时间，且有效期可配置")
	void shouldCarryFutureExpiry() {
		JwtCodec codec = codec(30);
		String token = codec.sign(1L, "E1", "李四", 2L, List.of(), List.of());

		JwtPayload payload = codec.parse(token);
		assertThat(codec.expireSeconds()).isEqualTo(30 * 60L);
		assertThat(payload.expiresAt()).isGreaterThan(java.time.Instant.now().getEpochSecond());
		// 有效期配置为 0 或负数时回退为默认 720 分钟，避免出现“签发即过期”的令牌
		assertThat(new JwtCodec(SECRET, 0L).expireSeconds()).isEqualTo(720 * 60L);
	}

	@Test
	@DisplayName("过期令牌：抛 40100")
	void shouldRejectExpiredToken() {
		// JwtCodec 不接受负的有效期（会回退默认值），因此用手写 body + HMAC 签名构造一个真实可见的过期令牌
		String expired = expiredToken();

		assertThatThrownBy(() -> codec(60).parse(expired))
				.isInstanceOf(com.university.webdesign.common.BusinessException.class)
				.hasMessageContaining("已过期");
	}

	/**
	 * 用同一密钥手工签发一个 payload 为 {@code {"userId":1,"exp":1}} 的令牌，
	 * 用于覆盖“有效期已过”的分支。
	 *
	 * @return 已过期的令牌
	 */
	private String expiredToken() {
		try {
			java.util.Base64.Encoder encoder = java.util.Base64.getUrlEncoder().withoutPadding();
			String header = encoder.encodeToString(
					"{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			String body = encoder.encodeToString(
					"{\"userId\":1,\"exp\":1}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
			String content = header + "." + body;
			javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
			mac.init(new javax.crypto.spec.SecretKeySpec(
					SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
			String signature = encoder.encodeToString(
					mac.doFinal(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
			return content + "." + signature;
		} catch (Exception exception) {
			throw new IllegalStateException("构造过期令牌失败", exception);
		}
	}

	@Test
	@DisplayName("格式错误的令牌：抛 40100")
	void shouldRejectMalformedToken() {
		JwtCodec codec = codec(60);

		assertThatThrownBy(() -> codec.parse("not-a-jwt"))
				.isInstanceOf(com.university.webdesign.common.BusinessException.class);
		assertThatThrownBy(() -> codec.parse("  "))
				.isInstanceOf(com.university.webdesign.common.BusinessException.class);
	}

	@Test
	@DisplayName("UserContext 可直接由载荷构造并判定角色与权限")
	void shouldBuildUserContext() {
		JwtCodec codec = codec(60);
		String token = codec.sign(9L, "E9", "王五", 3L,
				List.of("FINANCE"), List.of("report:view"));
		JwtPayload payload = codec.parse(token);

		UserContext context = new UserContext(payload.userId(), payload.employeeNo(), payload.name(),
				payload.deptId(), null, null, payload.roles(), payload.permCodes());

		assertThat(context.hasAnyRole("MANAGER", "finance")).isTrue();
		assertThat(context.hasPermission("report:view")).isTrue();
		assertThat(context.hasPermission("order:invalidate")).isFalse();
	}

	@Test
	@DisplayName("currentUser 视图字段可承载角色与权限集合")
	void shouldExposeCurrentUserCollections() {
		CurrentUserVO vo = new CurrentUserVO();
		vo.setUserId(1L);
		vo.setRoles(List.of("EMPLOYEE"));
		vo.setPermCodes(List.of("order:submit"));

		assertThat(vo.getRoles()).containsExactly("EMPLOYEE");
		assertThat(vo.getPermCodes()).containsExactly("order:submit");
	}
}
