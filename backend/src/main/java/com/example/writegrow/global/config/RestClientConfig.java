package com.example.writegrow.global.config;

import com.example.writegrow.global.config.properties.AiProperties;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 분석 서버 호출 전용 {@link RestClient}.
 */
@Configuration
public class RestClientConfig {

    public static final String AI_REST_CLIENT = "aiRestClient";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

    @Bean(name = AI_REST_CLIENT)
    public RestClient aiRestClient(AiProperties properties, RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
                // 반드시 HTTP/1.1 로 고정한다. 자바 HttpClient 는 기본이 HTTP/2 라, 평문 http 로
                // 보낼 때 Upgrade: h2c 로 승격을 시도한다. AI 서버(uvicorn)는 h2c 를 지원하지
                // 않아 승격이 거부되고, 그 과정에서 요청 본문이 전달되지 않아 422 가 돌아온다.
                // 컨테이너 사이 통신이라 HTTP/2 로 얻을 이점도 없다.
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(orDefault(properties.connectTimeout(), DEFAULT_CONNECT_TIMEOUT))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(orDefault(properties.readTimeout(), DEFAULT_READ_TIMEOUT));

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private static Duration orDefault(Duration value, Duration fallback) {
        return value == null ? fallback : value;
    }
}
