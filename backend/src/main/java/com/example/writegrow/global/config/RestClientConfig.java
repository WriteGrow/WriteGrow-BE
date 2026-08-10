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
