package com.example.writegrow.domain.account.service;

import com.example.writegrow.domain.account.dto.request.ProfileCreateRequest;
import com.example.writegrow.domain.account.dto.response.ProfileResponse;
import com.example.writegrow.domain.account.entity.Account;
import com.example.writegrow.domain.account.entity.Profile;
import com.example.writegrow.domain.account.exception.AccountErrorCode;
import com.example.writegrow.domain.account.exception.AccountException;
import com.example.writegrow.domain.account.repository.AccountRepository;
import com.example.writegrow.domain.account.repository.ProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final ProfileRepository profileRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public ProfileResponse create(Long accountId, ProfileCreateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND));

        Profile profile = profileRepository.save(
                Profile.create(account, request.role(), request.nickname(), request.birthYear()));
        return ProfileResponse.from(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long profileId) {
        return ProfileResponse.from(findProfile(profileId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileResponse> getProfilesByAccount(Long accountId) {
        if (accountRepository.findById(accountId).isEmpty()) {
            throw new AccountException(AccountErrorCode.ACCOUNT_NOT_FOUND);
        }
        return profileRepository.findAllByAccountId(accountId).stream()
                .map(ProfileResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Profile getWritableChild(Long profileId) {
        Profile profile = findProfile(profileId);
        if (!profile.isChild()) {
            throw new AccountException(AccountErrorCode.NOT_CHILD_PROFILE);
        }
        if (!profile.isConsentConfirmed()) {
            throw new AccountException(AccountErrorCode.CONSENT_REQUIRED);
        }
        return profile;
    }

    private Profile findProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.PROFILE_NOT_FOUND));
    }
}
