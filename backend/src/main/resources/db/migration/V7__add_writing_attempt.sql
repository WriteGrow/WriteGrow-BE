-- 아동이 변환 결과를 받아들이지 않고 다시 쓰는 흐름("다시 쓸게요")을 위한 시도 번호.
--
-- 획 배치는 (writing_id, batch_seq) 로 멱등을 판단해 왔다. 다시 쓰면 클라이언트가 batch_seq=0
-- 부터 다시 보내는데, 그러면 이전 시도의 배치와 충돌해 새 획이 조용히 버려진다.
-- 이전 획을 지우는 대신 시도 번호로 범위를 나눈다. 손글씨 원본과 획 데이터는 삭제하지 않는다.

ALTER TABLE writing
    ADD COLUMN attempt_no INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN writing.attempt_no IS '다시 쓴 횟수. 획 데이터를 시도별로 나누는 기준';

ALTER TABLE handwriting_stroke_batch
    ADD COLUMN attempt_no INTEGER NOT NULL DEFAULT 1;

COMMENT ON COLUMN handwriting_stroke_batch.attempt_no IS '몇 번째 시도의 획인지. 분석에는 현재 시도만 사용한다';

ALTER TABLE handwriting_stroke_batch
    DROP CONSTRAINT uk_handwriting_stroke_batch;

ALTER TABLE handwriting_stroke_batch
    ADD CONSTRAINT uk_handwriting_stroke_batch UNIQUE (writing_id, attempt_no, batch_seq);

CREATE INDEX idx_stroke_batch_writing_attempt ON handwriting_stroke_batch (writing_id, attempt_no);
