package com.example.writegrow.domain.account.repository;

import com.example.writegrow.domain.account.entity.Profile;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepository {

    private final ProfileJpaRepository profileJpaRepository;

    @Override
    public Profile save(Profile profile) {
        return profileJpaRepository.save(profile);
    }

    @Override
    public Optional<Profile> findById(Long id) {
        return profileJpaRepository.findById(id);
    }

    @Override
    public List<Profile> findAllByAccountId(Long accountId) {
        return profileJpaRepository.findAllByAccountIdOrderByCreatedAtDescIdDesc(accountId);
    }
}
