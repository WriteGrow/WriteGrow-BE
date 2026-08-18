package com.example.writegrow.domain.writing.event;

/**
 * 글의 최종본이 확정되었음을 알리는 애플리케이션 이벤트. 오류 분석(REQ-03)의 트리거다.
 *
 * <p>키보드 글은 제출 시점에, 손글씨 글은 아동이 OCR 변환 텍스트를 확인한 시점에 발행된다.
 * 두 경우 모두 "분석할 확정 텍스트가 생긴 시점"이라는 점에서 같다.
 *
 * <p>글쓰기 도메인이 분석 도메인을 직접 의존하지 않도록 이벤트로 분리한다.
 */
public record TextConfirmedEvent(Long writingId, Long profileId) {
}
