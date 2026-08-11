package com.example.writegrow.domain.analysis.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분석 상태 (PENDING: 대기, PROCESSING: 분석 중, SUCCEEDED: 완료, FAILED: 실패)")
public enum AnalysisStatus {

    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED
}
