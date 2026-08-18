-- 확신도 기반 오류 분석과 아동별 반복 오류 프로필. (REQ-03)

CREATE TABLE error_analysis
(
    id             BIGSERIAL PRIMARY KEY,
    writing_id     BIGINT      NOT NULL UNIQUE REFERENCES writing (id) ON DELETE CASCADE,
    profile_id     BIGINT      NOT NULL,
    status         VARCHAR(20) NOT NULL,
    analyzed_text  TEXT,
    provider       VARCHAR(50),
    requested_at   TIMESTAMP,
    completed_at   TIMESTAMP,
    failure_reason VARCHAR(500),
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP   NOT NULL
);

COMMENT ON TABLE error_analysis IS '확정된 글에 대한 오류 분석. OCR 분석과는 별개의 두 번째 분석이다.';
COMMENT ON COLUMN error_analysis.status IS 'PENDING | PROCESSING | SUCCEEDED | FAILED. 실패는 재처리를 위해 남긴다.';
COMMENT ON COLUMN error_analysis.analyzed_text IS '분석 대상이 된 확정 텍스트. 이후 글이 수정되어도 분석 근거를 보존한다.';

CREATE INDEX idx_error_analysis_profile_id ON error_analysis (profile_id);

CREATE TABLE error_candidate
(
    id                BIGSERIAL PRIMARY KEY,
    error_analysis_id BIGINT           NOT NULL REFERENCES error_analysis (id) ON DELETE CASCADE,
    seq               INTEGER          NOT NULL,
    error_type        VARCHAR(30)      NOT NULL,
    start_index       INTEGER,
    end_index         INTEGER,
    original_text     TEXT,
    suggestion        TEXT,
    confidence        DOUBLE PRECISION NOT NULL,
    reason            VARCHAR(300),
    -- 운영 기준 미만이면 true. 아동에게 노출하지 않고 보호자/교사 검토 대상으로만 분리하며,
    -- 반복 오류 프로필 집계에도 반영하지 않는다. (REQ-03 비즈니스 규칙 4)
    low_confidence    BOOLEAN          NOT NULL,
    created_at        TIMESTAMP        NOT NULL,
    updated_at        TIMESTAMP        NOT NULL
);

COMMENT ON TABLE error_candidate IS '오류 후보. 유형·원문 위치·제안 표현·판단 확신도를 가진다.';
COMMENT ON COLUMN error_candidate.error_type IS 'SPELLING | SPACING | FINAL_CONSONANT | PARTICLE_ENDING | SENTENCE_STRUCTURE | VOCABULARY';
COMMENT ON COLUMN error_candidate.reason IS '판단 근거. 보호자가 낮은 확신도 후보를 검토할 때 쓴다.';

CREATE INDEX idx_error_candidate_analysis_id ON error_candidate (error_analysis_id);

CREATE TABLE error_profile
(
    id                       BIGSERIAL PRIMARY KEY,
    profile_id               BIGINT      NOT NULL,
    error_type               VARCHAR(30) NOT NULL,
    occurrence_count         INTEGER     NOT NULL,
    correction_success_count INTEGER     NOT NULL,
    last_occurred_on         DATE,
    created_at               TIMESTAMP   NOT NULL,
    updated_at               TIMESTAMP   NOT NULL,
    CONSTRAINT uk_error_profile_profile_type UNIQUE (profile_id, error_type)
);

COMMENT ON TABLE error_profile IS '아동별·오류 유형별 누적 지표. 확정 오류만 쌓인다.';
COMMENT ON COLUMN error_profile.correction_success_count IS '아동이 스스로 고쳐낸 횟수. REQ-04 에서 갱신된다.';
