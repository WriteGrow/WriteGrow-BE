package com.example.writegrow.domain.handwriting.service;

import com.example.writegrow.domain.handwriting.dto.response.HandwritingSummaryResponse;
import java.util.Optional;

/**
 * 다른 도메인이 손글씨 원본 정보를 조회할 때 사용하는 읽기 전용 창구.
 */
public interface HandwritingQueryService {

    Optional<HandwritingSummaryResponse> findByWritingId(Long writingId);
}
