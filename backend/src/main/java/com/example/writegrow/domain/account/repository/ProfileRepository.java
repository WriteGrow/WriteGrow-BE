package com.example.writegrow.domain.account.repository;

import com.example.writegrow.domain.account.entity.Profile;
import com.example.writegrow.domain.account.entity.ProfileRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    /**
     * 한 계정의 프로필은 많아야 몇 개이므로 페이지 없이 전부 반환한다.
     * 생성 시각이 같을 때를 대비해 id 를 보조 정렬 기준으로 둔다.
     */
    List<Profile> findAllByAccountIdOrderByCreatedAtDescIdDesc(Long accountId);

    /**
     * 계정에 속한 특정 역할의 프로필. 보호자 홈이 연결된 자녀를 찾을 때 쓴다.
     * 카드 순서가 매번 바뀌지 않도록 오래된 순으로 고정한다.
     */
    List<Profile> findAllByAccountIdAndRoleOrderByCreatedAtAscIdAsc(Long accountId, ProfileRole role);
}
