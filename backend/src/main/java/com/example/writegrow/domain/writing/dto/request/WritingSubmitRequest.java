package com.example.writegrow.domain.writing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 키보드 글은 {@code content} 가 필요하고, 손글씨(PEN) 글은 사전에 업로드한 획 데이터와 이미지를 사용하므로 비워 둔다.
 */
@Schema(description = "글 제출 요청")
public record WritingSubmitRequest(

        @Schema(description = "키보드 입력 글의 내용. 손글씨 글에서는 사용하지 않는다.",
                example = "오늘 학교에서 친구랑 놀았다")
        String content
) {
}
