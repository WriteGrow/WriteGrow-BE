package com.example.writegrow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 애플리케이션 컨텍스트와 Flyway 마이그레이션, 엔티티 매핑 검증이 정상 동작하는지 확인한다.
 *
 * <p>Postgres 는 Testcontainers 가 띄우고 접속 정보는 {@link ServiceConnection} 이 주입한다.
 * Docker 가 없는 환경에서는 통째로 건너뛰므로, 단위 테스트만 돌릴 때 빌드가 깨지지 않는다.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@EnabledIf("dockerAvailable")
class WritegrowApplicationTests {

	// Testcontainers 2.x 의 PostgreSQLContainer 는 제네릭이 아니다.
	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	static boolean dockerAvailable() {
		try {
			return DockerClientFactory.instance().isDockerAvailable();
		} catch (RuntimeException exception) {
			return false;
		}
	}

	@Test
	void contextLoads() {
	}

}
