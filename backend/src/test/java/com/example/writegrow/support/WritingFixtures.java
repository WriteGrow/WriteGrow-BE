package com.example.writegrow.support;

import com.example.writegrow.domain.writing.entity.InputType;
import com.example.writegrow.domain.writing.entity.Writing;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 글 테스트 픽스처. 영속화 없이 ID 를 가진 엔티티를 만들기 위해 리플렉션을 사용한다.
 */
public final class WritingFixtures {

    private WritingFixtures() {
    }

    public static Writing keyboardWriting(Long id, Long profileId) {
        return writing(id, profileId, InputType.KEYBOARD);
    }

    public static Writing penWriting(Long id, Long profileId) {
        return writing(id, profileId, InputType.PEN);
    }

    /**
     * 손글씨 제출 후 OCR 변환까지 끝나 아동 확인을 기다리는 상태의 글.
     */
    public static Writing analyzedPenWriting(Long id, Long profileId, String ocrText) {
        Writing writing = penWriting(id, profileId);
        writing.submitHandwriting();
        writing.applyOcrText(ocrText);
        return writing;
    }

    public static Writing writing(Long id, Long profileId, InputType inputType) {
        Writing writing = Writing.create(profileId, inputType, "오늘 있었던 일");
        ReflectionTestUtils.setField(writing, "id", id);
        return writing;
    }

    public static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
