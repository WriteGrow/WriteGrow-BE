package com.example.writegrow.domain.writing.event;

/**
 * 손글씨 글이 제출되었음을 알리는 애플리케이션 이벤트.
 *
 * <p>글쓰기 도메인이 분석 도메인을 직접 의존하지 않도록 이벤트로 분리한다.
 * 분석 파이프라인은 트랜잭션 커밋 이후 비동기로 시작된다.
 */
public record HandwritingSubmittedEvent(Long writingId, Long profileId) {
}
