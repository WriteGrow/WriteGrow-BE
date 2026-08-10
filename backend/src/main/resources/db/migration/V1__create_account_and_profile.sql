-- 가족 계정(Account)과 그 아래 여러 사용자 프로필(Profile).
-- 넷플릭스형 구조로, 하나의 계정에 아동/보호자 프로필이 함께 속한다.

CREATE TABLE account
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   NOT NULL,
    updated_at TIMESTAMP   NOT NULL
);

COMMENT ON TABLE account IS '가족 계정';

CREATE TABLE profile
(
    id                BIGSERIAL PRIMARY KEY,
    account_id        BIGINT      NOT NULL REFERENCES account (id),
    role              VARCHAR(20) NOT NULL,
    nickname          VARCHAR(30) NOT NULL,
    birth_year        INTEGER,
    -- REQ-08(보호자 동의)이 구현되기 전까지는 항상 true 로 생성된다.
    consent_confirmed BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP   NOT NULL,
    updated_at        TIMESTAMP   NOT NULL
);

COMMENT ON TABLE profile IS '계정에 속한 사용자 프로필(아동/보호자)';
COMMENT ON COLUMN profile.role IS 'CHILD | PARENTS';
COMMENT ON COLUMN profile.consent_confirmed IS '보호자 동의 확인 여부. 미확인 시 글 저장 및 분석이 차단된다.';

CREATE INDEX idx_profile_account_id ON profile (account_id);
