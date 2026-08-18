package com.example.writegrow.domain.account.service;

import com.example.writegrow.domain.account.dto.request.ProfileCreateRequest;
import com.example.writegrow.domain.account.dto.response.ProfileResponse;
import com.example.writegrow.domain.account.entity.Profile;
import java.util.List;

public interface ProfileService {

    ProfileResponse create(Long accountId, ProfileCreateRequest request);

    ProfileResponse getProfile(Long profileId);

    /**
     * 한 계정의 프로필은 개수가 적고 늘어날 여지가 없으므로 페이지 없이 전부 반환한다.
     */
    List<ProfileResponse> getProfilesByAccount(Long accountId);

    /**
     * 글쓰기가 허용된 아동 프로필을 조회한다.
     *
     * <p>REQ-01 선행조건(보호자 동의 확인)을 검사하는 단일 지점이다. REQ-08 이 구현되면 이 메서드만 바뀐다.
     *
     * @throws com.example.writegrow.domain.account.exception.AccountException 프로필이 없거나, 아동이 아니거나, 동의가 미확인인 경우
     */
    Profile getWritableChild(Long profileId);

    /**
     * 조회자가 열람할 수 있는 아동 프로필을 반환한다.
     *
     * <p>보호자·교사는 "연결된 아동"의 분석 정보만 볼 수 있다(REQ-03 권한 규칙). 인증이 없는
     * 지금은 <b>같은 계정에 속하는지</b>로 연결을 판단한다. 아동이 자기 자신을 조회하는 것도 허용한다.
     *
     * <p>REQ-08 로 인증이 붙으면 조회자 식별 방법만 바뀌고 이 검증 규칙은 그대로 쓴다.
     *
     * @throws com.example.writegrow.domain.account.exception.AccountException 프로필이 없거나,
     *                                                                         대상이 아동이 아니거나, 같은 계정이 아닌 경우
     */
    Profile getViewableChild(Long viewerProfileId, Long childProfileId);
}
