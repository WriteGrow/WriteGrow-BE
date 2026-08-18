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
import java.util.Objects;
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
        return profileRepository.findAllByAccountIdOrderByCreatedAtDescIdDesc(accountId).stream()
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

    @Override
    @Transactional(readOnly = true)
    public Profile getViewableChild(Long viewerProfileId, Long childProfileId) {
        Profile child = findProfile(childProfileId);
        if (!child.isChild()) {
            throw new AccountException(AccountErrorCode.NOT_CHILD_PROFILE);
        }
        if (Objects.equals(viewerProfileId, childProfileId)) {
            return child;
        }

        Profile viewer = findProfile(viewerProfileId);
        // 아동은 자기 기록만 볼 수 있다. 계정만 확인하면 남매가 서로의 오류 분석을 들여다볼 수 있다.
        if (viewer.isChild()) {
            throw new AccountException(AccountErrorCode.NOT_LINKED_CHILD);
        }
        // 보호자·교사는 같은 계정이면 연결된 것으로 본다. 계정 밖에서는 열람할 수 없다.
        if (!Objects.equals(viewer.getAccount().getId(), child.getAccount().getId())) {
            throw new AccountException(AccountErrorCode.NOT_LINKED_CHILD);
        }
        return child;
    }

    private Profile findProfile(Long profileId) {
        return profileRepository.findById(profileId)
                .orElseThrow(() -> new AccountException(AccountErrorCode.PROFILE_NOT_FOUND));
    }
}
