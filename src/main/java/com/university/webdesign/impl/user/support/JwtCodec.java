package com.university.webdesign.impl.user.support;

import com.university.webdesign.common.BusinessException;
import com.university.webdesign.common.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 轻量 JWT 编解码器（HS256）。
 * <p>
 * 对应《重构实施规范》第 5 节 M4：认证采用 JWT（HS256，密钥与有效期可配置），
 * 令牌形如 {@code header.payload.signature}，三部分均为 Base64URL（无填充）。
 * 为了不引入额外依赖，这里只用 JDK 的 {@link Mac} 与 {@link Base64} 手写编解码，
 * 不依赖 jjwt 等第三方库。
 * <p>
 * 载荷字段见 {@link JwtPayload}；解析失败（格式错误、签名不符、已过期）统一抛 40100。
 */
@Component
public class JwtCodec
{
	/**
	 * HMAC-SHA256 算法名
	 */
	private static final String HMAC_ALGORITHM = "HmacSHA256";

	/**
	 * Base64URL 编码器（无填充）
	 */
	private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

	/**
	 * Base64URL 解码器
	 */
	private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

	/**
	 * 固定 header：{"alg":"HS256","typ":"JWT"} 的 Base64URL 形式
	 */
	private static final String HEADER = ENCODER.encodeToString(
			"{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

	/**
	 * 签名密钥，可用 {@code app.jwt.secret} 覆盖
	 */
	private final String secret;

	/**
	 * 令牌有效期（分钟），可用 {@code app.jwt.expire-minutes} 覆盖
	 */
	private final long expireMinutes;

	/**
	 * 构造器注入配置项
	 *
	 * @param secret        签名密钥
	 * @param expireMinutes 有效期（分钟）
	 */
	public JwtCodec(
			@Value("${app.jwt.secret:webdesign-demo-jwt-secret-please-override-in-production-2026}")
			String secret,
			@Value("${app.jwt.expire-minutes:720}") long expireMinutes) {
		this.secret = secret;
		this.expireMinutes = expireMinutes <= 0 ? 720L : expireMinutes;
	}

	/**
	 * 令牌有效期（秒）
	 *
	 * @return 有效秒数
	 */
	public long expireSeconds() {
		return expireMinutes * 60L;
	}

	/**
	 * 签发令牌
	 *
	 * @param userId    用户ID
	 * @param employeeNo 工号
	 * @param name      姓名
	 * @param deptId    部门ID
	 * @param roles     角色编码集合
	 * @param permCodes 权限点编码集合
	 * @return 令牌字符串
	 */
	public String sign(Long userId, String employeeNo, String name, Long deptId,
			List<String> roles, List<String> permCodes) {
		long issuedAt = Instant.now().getEpochSecond();
		long expiresAt = issuedAt + expireSeconds();
		JwtPayload payload = new JwtPayload(userId, employeeNo, name, deptId,
				roles == null ? List.of() : List.copyOf(roles),
				permCodes == null ? List.of() : List.copyOf(permCodes),
				issuedAt, expiresAt);
		String body = ENCODER.encodeToString(toJson(payload).getBytes(StandardCharsets.UTF_8));
		String content = HEADER + "." + body;
		return content + "." + sign(content);
	}

	/**
	 * 解析并校验令牌（签名、有效期）
	 *
	 * @param token 令牌
	 * @return 载荷
	 */
	public JwtPayload parse(String token) {
		if (token == null || token.isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "请先登录");
		}
		String raw = unify(token);
		String[] parts = raw.split("\\.");
		if (parts.length != 3) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "登录令牌格式不正确");
		}
		String expected = sign(parts[0] + "." + parts[1]);
		if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
				parts[2].getBytes(StandardCharsets.UTF_8))) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "登录令牌签名校验失败");
		}
		String json;
		try {
			json = new String(DECODER.decode(parts[1]), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "登录令牌内容损坏", exception);
		}
		JwtPayload payload = fromJson(json);
		if (payload.userId() == null) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "登录令牌缺少用户信息");
		}
		if (payload.expiresAt() <= Instant.now().getEpochSecond()) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "登录已过期，请重新登录");
		}
		return payload;
	}

	/**
	 * 去掉 {@code Bearer } 前缀
	 *
	 * @param token 原始令牌或请求头值
	 * @return 纯令牌
	 */
	public static String unify(String token) {
		if (token == null) {
			return "";
		}
		String normalized = token.trim();
		if (normalized.regionMatches(true, 0, "Bearer ", 0, 7)) {
			normalized = normalized.substring(7).trim();
		}
		return normalized;
	}

	/**
	 * HMAC-SHA256 签名
	 *
	 * @param content 待签内容
	 * @return Base64URL 签名
	 */
	private String sign(String content) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			return ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "登录令牌签名失败", exception);
		}
	}

	/**
	 * 载荷转 JSON（手写，避免引入 JSON 序列化依赖）
	 *
	 * @param payload 载荷
	 * @return JSON 字符串
	 */
	private String toJson(JwtPayload payload) {
		StringBuilder builder = new StringBuilder(256);
		builder.append('{');
		builder.append("\"userId\":").append(payload.userId() == null ? "null" : payload.userId());
		builder.append(",\"employeeNo\":").append(quote(payload.employeeNo()));
		builder.append(",\"name\":").append(quote(payload.name()));
		builder.append(",\"deptId\":").append(payload.deptId() == null ? "null" : payload.deptId());
		builder.append(",\"roles\":").append(quoteArray(payload.roles()));
		builder.append(",\"permCodes\":").append(quoteArray(payload.permCodes()));
		builder.append(",\"iat\":").append(payload.issuedAt());
		builder.append(",\"exp\":").append(payload.expiresAt());
		builder.append('}');
		return builder.toString();
	}

	/**
	 * JSON 转载荷
	 *
	 * @param json JSON 字符串
	 * @return 载荷
	 */
	private JwtPayload fromJson(String json) {
		Map<String, String> values = parseObject(json);
		Long issuedAt = parseLong(values.get("iat"));
		Long expiresAt = parseLong(values.get("exp"));
		return new JwtPayload(
				parseLong(values.get("userId")),
				unquote(values.get("employeeNo")),
				unquote(values.get("name")),
				parseLong(values.get("deptId")),
				parseArray(values.get("roles")),
				parseArray(values.get("permCodes")),
				issuedAt == null ? 0L : issuedAt,
				expiresAt == null ? 0L : expiresAt);
	}

	/**
	 * 去掉字符串字面量两侧的引号并还原转义
	 *
	 * @param literal JSON 字符串字面量
	 * @return 原始字符串；{@code null} 或 {@code null} 字面量返回 null
	 */
	private String unquote(String literal) {
		if (literal == null || literal.isBlank() || "null".equals(literal)) {
			return null;
		}
		if (!literal.startsWith("\"") || literal.length() < 2) {
			return literal;
		}
		String raw = literal.substring(1, literal.length() - 1);
		StringBuilder builder = new StringBuilder(raw.length());
		for (int index = 0; index < raw.length(); index++) {
			char character = raw.charAt(index);
			if (character != '\\' || index + 1 >= raw.length()) {
				builder.append(character);
				continue;
			}
			char next = raw.charAt(++index);
			switch (next) {
				case 'n' -> builder.append('\n');
				case 'r' -> builder.append('\r');
				case 't' -> builder.append('\t');
				case 'u' -> {
					if (index + 4 < raw.length()) {
						builder.append((char) Integer.parseInt(raw.substring(index + 1, index + 5), 16));
						index += 4;
					}
				}
				default -> builder.append(next);
			}
		}
		return builder.toString();
	}

	/**
	 * 解析扁平 JSON 对象（值为字符串、数字、null 或字符串数组）
	 *
	 * @param json JSON 字符串
	 * @return 字段名到原始字面量的映射
	 */
	private Map<String, String> parseObject(String json) {
		Map<String, String> values = new LinkedHashMap<>();
		int index = json.indexOf('{');
		if (index < 0) {
			throw new BusinessException(ErrorCode.UNAUTHENTICATED, "登录令牌内容损坏");
		}
		int position = index + 1;
		while (position < json.length()) {
			int keyStart = json.indexOf('"', position);
			if (keyStart < 0) {
				break;
			}
			int keyEnd = json.indexOf('"', keyStart + 1);
			if (keyEnd < 0) {
				break;
			}
			String key = json.substring(keyStart + 1, keyEnd);
			int colon = json.indexOf(':', keyEnd);
			if (colon < 0) {
				break;
			}
			int valueStart = colon + 1;
			while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
				valueStart++;
			}
			int valueEnd = valueStart;
			String literal;
			if (valueStart < json.length() && json.charAt(valueStart) == '[') {
				valueEnd = json.indexOf(']', valueStart);
				if (valueEnd < 0) {
					break;
				}
				literal = json.substring(valueStart, valueEnd + 1);
			} else if (valueStart < json.length() && json.charAt(valueStart) == '"') {
				valueEnd = json.indexOf('"', valueStart + 1);
				if (valueEnd < 0) {
					break;
				}
				literal = json.substring(valueStart, valueEnd + 1);
			} else {
				valueEnd = valueStart;
				while (valueEnd < json.length() && json.charAt(valueEnd) != ','
						&& json.charAt(valueEnd) != '}') {
					valueEnd++;
				}
				literal = json.substring(valueStart, valueEnd).trim();
			}
			values.put(key, literal);
			int next = json.indexOf(',', valueEnd);
			if (next < 0) {
				break;
			}
			position = next + 1;
		}
		return values;
	}

	/**
	 * 解析字符串数组字面量
	 *
	 * @param literal 形如 {@code ["A","B"]} 的字面量
	 * @return 字符串列表
	 */
	private List<String> parseArray(String literal) {
		List<String> values = new ArrayList<>();
		if (literal == null || literal.isBlank() || "null".equals(literal)) {
			return values;
		}
		int position = 0;
		while (position < literal.length()) {
			int start = literal.indexOf('"', position);
			if (start < 0) {
				break;
			}
			int end = literal.indexOf('"', start + 1);
			if (end < 0) {
				break;
			}
			values.add(literal.substring(start + 1, end));
			position = end + 1;
		}
		return values;
	}

	/**
	 * 解析长整数字面量
	 *
	 * @param literal 字面量
	 * @return 数值；{@code null} 或非数字返回 null
	 */
	private Long parseLong(String literal) {
		if (literal == null || literal.isBlank() || "null".equals(literal) || literal.startsWith("\"")) {
			return null;
		}
		try {
			return Long.valueOf(literal.trim());
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	/**
	 * JSON 字符串转义
	 *
	 * @param value 原始字符串
	 * @return 带引号的 JSON 字符串
	 */
	private String quote(String value) {
		if (value == null) {
			return "null";
		}
		StringBuilder builder = new StringBuilder(value.length() + 8);
		builder.append('"');
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			switch (character) {
				case '"' -> builder.append("\\\"");
				case '\\' -> builder.append("\\\\");
				case '\n' -> builder.append("\\n");
				case '\r' -> builder.append("\\r");
				case '\t' -> builder.append("\\t");
				default -> {
					if (character < 0x20) {
						builder.append(String.format("\\u%04x", (int) character));
					} else {
						builder.append(character);
					}
				}
			}
		}
		return builder.append('"').toString();
	}

	/**
	 * 字符串数组转 JSON
	 *
	 * @param values 字符串集合
	 * @return JSON 数组
	 */
	private String quoteArray(List<String> values) {
		if (values == null || values.isEmpty()) {
			return "[]";
		}
		StringBuilder builder = new StringBuilder(values.size() * 16);
		builder.append('[');
		for (int index = 0; index < values.size(); index++) {
			if (index > 0) {
				builder.append(',');
			}
			builder.append(quote(values.get(index)));
		}
		return builder.append(']').toString();
	}
}
