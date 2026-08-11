-- 아동이 작성한 글과 그 수정 이력. (REQ-01)

CREATE TABLE writing
(
    id            BIGSERIAL PRIMARY KEY,
    profile_id    BIGINT      NOT NULL REFERENCES profile (id),
    input_type    VARCHAR(20) NOT NULL,
    status        VARCHAR(30) NOT NULL,
    topic         VARCHAR(100),
    -- 키보드 글은 아동이 입력한 문장, 펜 글은 OCR 변환 결과가 들어간다.
    original_text TEXT,
    -- 아동이 확인/수정을 마친 최종본.
    final_text    TEXT,
    submitted_at  TIMESTAMP,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL
);

COMMENT ON TABLE writing IS '아동 자유 글쓰기 기록';
COMMENT ON COLUMN writing.input_type IS 'KEYBOARD | PEN';
COMMENT ON COLUMN writing.status IS 'DRAFT | SUBMITTED | ANALYZED | CONFIRMED | ANALYSIS_FAILED';

CREATE INDEX idx_writing_profile_created_at ON writing (profile_id, created_at DESC);

CREATE TABLE writing_revision
(
    id          BIGSERIAL PRIMARY KEY,
    writing_id  BIGINT      NOT NULL REFERENCES writing (id) ON DELETE CASCADE,
    revision_no INTEGER     NOT NULL,
    content     TEXT        NOT NULL,
    source      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    CONSTRAINT uk_writing_revision_no UNIQUE (writing_id, revision_no)
);

COMMENT ON TABLE writing_revision IS '글 수정 이력. 수정 전/후 비교의 근거 데이터.';
COMMENT ON COLUMN writing_revision.source IS 'INITIAL(최초 작성) | OCR(손글씨 변환) | CHILD_EDIT(아동 수정)';

CREATE INDEX idx_writing_revision_writing_id ON writing_revision (writing_id);
