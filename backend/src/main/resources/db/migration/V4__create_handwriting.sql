-- 손글씨 "과정" 데이터. 결과 이미지뿐 아니라 획을 그은 순서와 시각을 함께 보관한다. (REQ-02)

CREATE TABLE handwriting_stroke_batch
(
    id           BIGSERIAL PRIMARY KEY,
    writing_id   BIGINT    NOT NULL REFERENCES writing (id) ON DELETE CASCADE,
    -- 클라이언트가 부여하는 배치 순번. 재전송 시 멱등 처리의 기준이 된다.
    batch_seq    INTEGER   NOT NULL,
    payload      JSONB     NOT NULL,
    stroke_count INTEGER   NOT NULL,
    received_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_handwriting_stroke_batch UNIQUE (writing_id, batch_seq)
);

COMMENT ON TABLE handwriting_stroke_batch IS '작성 중 주기적으로 수신한 획 데이터 묶음';
COMMENT ON COLUMN handwriting_stroke_batch.payload IS '{"strokes":[{"index":0,"penDownAt":0,"penUpAt":420,"points":[{"x":10.5,"y":20.1,"t":0,"pressure":0.4}]}]}';

CREATE TABLE handwriting_asset
(
    id                BIGSERIAL PRIMARY KEY,
    writing_id        BIGINT    NOT NULL UNIQUE REFERENCES writing (id) ON DELETE CASCADE,
    image_object_key  VARCHAR(500),
    stroke_object_key VARCHAR(500),
    canvas_width      INTEGER,
    canvas_height     INTEGER,
    total_duration_ms BIGINT,
    stroke_count      INTEGER,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

COMMENT ON TABLE handwriting_asset IS '손글씨 원본 자산(S3 객체 키)과 과정 요약 지표. 분석 실패 시에도 보존한다.';
