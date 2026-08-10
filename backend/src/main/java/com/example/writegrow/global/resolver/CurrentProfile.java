package com.example.writegrow.global.resolver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 요청을 보낸 프로필 ID 를 컨트롤러 파라미터로 주입한다.
 *
 * <p>MVP 에서는 {@code X-Profile-Id} 헤더에서 값을 읽지만, 인증이 도입되면
 * {@link CurrentProfileArgumentResolver} 내부만 JWT 기반으로 교체하면 되고 컨트롤러는 변경되지 않는다.
 */
@Documented
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentProfile {
}
