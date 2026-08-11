package com.example.writegrow.domain.writing.dto.response;

import com.example.writegrow.domain.handwriting.dto.response.HandwritingSummaryResponse;
import com.example.writegrow.domain.writing.entity.InputType;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "글 상세 응답. 원문과 최종 수정본, 수정 이력, 손글씨 원본 정보를 함께 제공한다.")
public record WritingDetailResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "작성한 아동 프로필 ID", example = "1")
        Long profileId,

        @Schema(description = "입력 방식", example = "PEN")
        InputType inputType,

        @Schema(description = "글 상태", example = "CONFIRMED")
        WritingStatus status,

        @Schema(description = "글쓰기 주제", example = "오늘 있었던 일")
        String topic,

        @Schema(description = "원문. 키보드 글은 아동이 입력한 문장, 손글씨 글은 OCR 변환 결과.",
                example = "오늘 학교에서 친구랑 놀앗다")
        String originalText,

        @Schema(description = "아동이 확인/수정을 마친 최종본", example = "오늘 학교에서 친구랑 놀았다")
        String finalText,

        @Schema(description = "작성 시각", example = "2026-08-09T20:11:00")
        LocalDateTime createdAt,

        @Schema(description = "제출 시각", example = "2026-08-09T20:14:00")
        LocalDateTime submittedAt,

        @Schema(description = "수정 이력. 수정 전/후 비교의 근거가 된다.")
        List<WritingRevisionResponse> revisions,

        @Schema(description = "손글씨 원본 정보. 키보드 글이면 null.")
        HandwritingSummaryResponse handwriting
) {

    public static WritingDetailResponse of(Writing writing, HandwritingSummaryResponse handwriting) {
        return new WritingDetailResponse(
                writing.getId(),
                writing.getProfileId(),
                writing.getInputType(),
                writing.getStatus(),
                writing.getTopic(),
                writing.getOriginalText(),
                writing.getFinalText(),
                writing.getCreatedAt(),
                writing.getSubmittedAt(),
                writing.getRevisions().stream().map(WritingRevisionResponse::from).toList(),
                handwriting);
    }
}
