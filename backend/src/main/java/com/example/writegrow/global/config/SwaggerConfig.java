package com.example.writegrow.global.config;

import com.example.writegrow.global.exception.ErrorResponse;
import com.example.writegrow.global.resolver.CurrentProfile;
import com.example.writegrow.global.resolver.CurrentProfileArgumentResolver;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.Arrays;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String ERROR_SCHEMA_NAME = "ErrorResponse";
    private static final String ERROR_SCHEMA_REF = "#/components/schemas/" + ERROR_SCHEMA_NAME;
    private static final String APPLICATION_JSON = "application/json";

    static {
        // @CurrentProfile 은 X-Profile-Id 헤더에서 값을 채우는 커스텀 리졸버용 애노테이션이다.
        // 지정하지 않으면 springdoc 이 이 파라미터를 필수 쿼리 파라미터로 오해해 문서에 노출한다.
        SpringDocUtils.getConfig().addAnnotationsToIgnore(CurrentProfile.class);
    }

    @Bean
    public OpenAPI writegrowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WriteGrow API")
                        .description("""
                                초등 저학년 아동 자유 글쓰기 및 AI 성장 지원 서비스 백엔드 API.

                                MVP 범위: REQ-01(자유 글쓰기 작성 및 기록), REQ-02(손글씨 OCR 변환 및 원문 확인).

                                인증은 아직 구현되지 않았습니다. 아동/보호자 식별이 필요한 API 는
                                `X-Profile-Id` 헤더로 프로필 ID 를 전달하세요.
                                프로필은 `POST /api/accounts` 로 계정을 만든 뒤
                                `POST /api/accounts/{accountId}/profiles` 로 생성합니다.
                                """)
                        .version("1.0.0"));
    }

    /**
     * {@link CurrentProfile} 을 사용하는 API 에 {@code X-Profile-Id} 헤더 파라미터를 문서화한다.
     */
    @Bean
    public OperationCustomizer currentProfileHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            boolean usesCurrentProfile = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(CurrentProfile.class));
            if (usesCurrentProfile) {
                operation.addParametersItem(new Parameter()
                        .in("header")
                        .name(CurrentProfileArgumentResolver.PROFILE_HEADER)
                        .description("요청 주체의 프로필 ID (인증 도입 전 임시 식별 수단)")
                        .required(true)
                        .schema(new StringSchema().example("1")));
            }
            return operation;
        };
    }

    /**
     * 4xx/5xx 응답의 스키마를 {@link ErrorResponse} 로 교체한다.
     *
     * <p>이렇게 하지 않으면 springdoc 이 모든 응답에 컨트롤러 반환 타입을 그대로 붙여, 오류 예시에 성공 데이터가 섞인다.
     */
    @Bean
    public OperationCustomizer errorResponseSchemaCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() == null) {
                return operation;
            }
            operation.getResponses().forEach((statusCode, response) -> {
                if (isErrorStatus(statusCode)) {
                    response.setContent(new Content().addMediaType(APPLICATION_JSON,
                            new io.swagger.v3.oas.models.media.MediaType()
                                    .schema(new Schema<>().$ref(ERROR_SCHEMA_REF))));
                }
            });
            return operation;
        };
    }

    /**
     * 위 커스터마이저가 참조하는 {@link ErrorResponse} 스키마를 components 에 등록한다.
     */
    @Bean
    public OpenApiCustomizer errorSchemaRegistrar() {
        return openApi -> {
            ResolvedSchema resolved = ModelConverters.getInstance()
                    .readAllAsResolvedSchema(ErrorResponse.class);
            if (resolved == null || resolved.referencedSchemas == null) {
                return;
            }
            Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
            resolved.referencedSchemas.forEach(components::addSchemas);
            openApi.setComponents(components);
        };
    }

    private static boolean isErrorStatus(String statusCode) {
        try {
            return Integer.parseInt(statusCode) >= 400;
        } catch (NumberFormatException exception) {
            // "default" 같은 비수치 키는 대상이 아니다.
            return false;
        }
    }
}
