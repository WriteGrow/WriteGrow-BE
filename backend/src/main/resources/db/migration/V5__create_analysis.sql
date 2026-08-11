-- OCR 변환 결과와 작성 과정 분석 지표. (REQ-02, REQ-03 대비)

CREATE TABLE ocr_result
(
    id                 BIGSERIAL PRIMARY KEY,
    writing_id         BIGINT      NOT NULL UNIQUE REFERENCES writing (id) ON DELETE CASCADE,
    status             VARCHAR(20) NOT NULL,
    full_text          TEXT,
    overall_confidence DOUBLE PRECISION,
    provider           VARCHAR(50),
    requested_at       TIMESTAMP,
    completed_at       TIMESTAMP,
    failure_reason     VARCHAR(500),
    created_at         TIMESTAMP   NOT NULL,
    updated_at         TIMESTAMP   NOT NULL
);

COMMENT ON TABLE ocr_result IS '손글씨 OCR 변환 결과';
COMMENT ON COLUMN ocr_result.status IS 'PENDING | PROCESSING | SUCCEEDED | FAILED';

CREATE TABLE ocr_segment
(
    id             BIGSERIAL PRIMARY KEY,
    ocr_result_id  BIGINT           NOT NULL REFERENCES ocr_result (id) ON DELETE CASCADE,
    seq            INTEGER          NOT NULL,
    segment_text   TEXT             NOT NULL,
    confidence     DOUBLE PRECISION NOT NULL,
    start_index    INTEGER,
    end_index      INTEGER,
    -- 운영 임계값 미만이면 true. 아동에게 오류로 확정해 보여주지 않고 보호자/교사 검토 대상으로만 분리한다.
    low_confidence BOOLEAN          NOT NULL,
    created_at     TIMESTAMP        NOT NULL,
    updated_at     TIMESTAMP        NOT NULL
);

COMMENT ON TABLE ocr_segment IS '구절 단위 OCR 결과와 확신도';

CREATE INDEX idx_ocr_segment_result_id ON ocr_segment (ocr_result_id);

CREATE TABLE writing_process_metric
(
    id                     BIGSERIAL PRIMARY KEY,
    writing_id             BIGINT    NOT NULL UNIQUE REFERENCES writing (id) ON DELETE CASCADE,
    total_duration_ms      BIGINT,
    pause_count            INTEGER,
    longest_pause_ms       BIGINT,
    avg_stroke_duration_ms BIGINT,
    hesitation_points      JSONB,
    created_at             TIMESTAMP NOT NULL,
    updated_at             TIMESTAMP NOT NULL
);

COMMENT ON TABLE writing_process_metric IS '"결과가 아니라 과정" 분석 지표. 어떤 글자/자모에서 머뭇거렸는지를 포함한다.';
COMMENT ON COLUMN writing_process_metric.hesitation_points IS '[{"charIndex":12,"character":"놀","jamo":"ㄴ","durationMs":5200,"retryCount":2}]';
