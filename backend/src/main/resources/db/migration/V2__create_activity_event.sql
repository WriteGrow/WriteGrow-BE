-- 기능명세서의 "~ 이벤트를 기록한다" 요구사항을 한 테이블로 수집한다.

CREATE TABLE activity_event
(
    id          BIGSERIAL PRIMARY KEY,
    profile_id  BIGINT      NOT NULL,
    writing_id  BIGINT,
    type        VARCHAR(40) NOT NULL,
    payload     JSONB,
    occurred_at TIMESTAMP   NOT NULL
);

COMMENT ON TABLE activity_event IS '아동 학습 활동 이벤트 로그';
COMMENT ON COLUMN activity_event.type IS 'WRITING_SUBMITTED | WRITING_CONFIRMED | OCR_REQUESTED | OCR_COMPLETED | OCR_FAILED | OCR_TEXT_EDITED';

CREATE INDEX idx_activity_event_profile_occurred_at ON activity_event (profile_id, occurred_at DESC);
CREATE INDEX idx_activity_event_writing_id ON activity_event (writing_id);
