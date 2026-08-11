package com.example.writegrow.domain.writing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "키보드 글 임시 저장 요청")
public record WritingDraftUpdateRequest(

        @Schema(description = "작성 중인 내용", example = "오늘 학교에서 친구랑 놀았다")
        @NotNull(message = "내용은 null 일 수 없습니다.")
        String content
) {
}
