package com.example.writegrow.domain.account.repository;

import com.example.writegrow.domain.account.entity.Profile;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository {

    Profile save(Profile profile);

    Optional<Profile> findById(Long id);

    /**
     * 계정에 속한 프로필을 최신 생성순으로 반환한다.
     */
    List<Profile> findAllByAccountId(Long accountId);
}
