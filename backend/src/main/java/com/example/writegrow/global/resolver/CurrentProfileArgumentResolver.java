package com.example.writegrow.global.resolver;

import com.example.writegrow.global.exception.GlobalErrorCode;
import com.example.writegrow.global.exception.GlobalException;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link CurrentProfile} 파라미터를 {@code X-Profile-Id} 헤더 값으로 채운다.
 */
@Component
public class CurrentProfileArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String PROFILE_HEADER = "X-Profile-Id";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentProfile.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String headerValue = webRequest.getHeader(PROFILE_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            throw new GlobalException(GlobalErrorCode.MISSING_PROFILE_HEADER);
        }
        try {
            return Long.parseLong(headerValue.trim());
        } catch (NumberFormatException exception) {
            throw new GlobalException(GlobalErrorCode.MISSING_PROFILE_HEADER);
        }
    }
}
