package com.example.writegrow.domain.handwriting.service;

import com.example.writegrow.domain.handwriting.dto.request.StrokeBatchAppendRequest;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingAnalysisSource;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingImageUploadResponse;
import com.example.writegrow.domain.handwriting.dto.response.StrokeBatchAppendResponse;
import org.springframework.web.multipart.MultipartFile;

public interface HandwritingService {

    /**
     * 작성 중 수신한 획 묶음을 저장한다. 같은 배치 순번은 중복 저장하지 않는다.
     */
    StrokeBatchAppendResponse appendStrokeBatch(Long profileId, Long writingId, StrokeBatchAppendRequest request);

    /**
     * 손글씨 렌더 이미지를 S3 에 업로드한다.
     */
    HandwritingImageUploadResponse uploadImage(Long profileId, Long writingId, MultipartFile file,
                                               Integer canvasWidth, Integer canvasHeight);

    /**
     * 수신한 획 배치를 하나의 JSON 으로 병합해 S3 에 올리고 분석에 필요한 자료를 반환한다.
     *
     * <p>분석 파이프라인에서 호출한다. 재시도 시 다시 호출해도 안전하다.
     */
    HandwritingAnalysisSource finalizeForAnalysis(Long writingId);
}
