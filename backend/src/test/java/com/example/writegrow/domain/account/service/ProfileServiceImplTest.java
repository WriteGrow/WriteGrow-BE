package com.example.writegrow.domain.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.writegrow.domain.account.dto.request.ProfileCreateRequest;
import com.example.writegrow.domain.account.dto.response.ProfileResponse;
import com.example.writegrow.domain.account.entity.Profile;
import com.example.writegrow.domain.account.entity.ProfileRole;
import com.example.writegrow.domain.account.exception.AccountErrorCode;
import com.example.writegrow.domain.account.exception.AccountException;
import com.example.writegrow.domain.account.repository.AccountRepository;
import com.example.writegrow.domain.account.repository.ProfileRepository;
import com.example.writegrow.support.AccountFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileServiceImpl 단위 테스트")
class ProfileServiceImplTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ProfileServiceImpl profileService;

    @Nested
    @DisplayName("프로필 생성")
    class Create {

        @Test
        @DisplayName("계정이 존재하면 프로필을 저장하고 응답으로 변환한다")
        void createsProfile() {
            given(accountRepository.findById(1L)).willReturn(Optional.of(AccountFixtures.account(1L)));
            given(profileRepository.save(any(Profile.class))).willAnswer(invocation -> {
                Profile profile = invocation.getArgument(0);
                AccountFixtures.setId(profile, 10L);
                return profile;
            });

            ProfileResponse response = profileService.create(
                    1L, new ProfileCreateRequest(ProfileRole.CHILD, "민준", 2018));

            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.accountId()).isEqualTo(1L);
            assertThat(response.role()).isEqualTo(ProfileRole.CHILD);
            assertThat(response.nickname()).isEqualTo("민준");
            assertThat(response.consentConfirmed()).isTrue();
        }

        @Test
        @DisplayName("계정이 없으면 ACCOUNT_NOT_FOUND 예외가 발생한다")
        void throwsWhenAccountMissing() {
            given(accountRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.create(
                    99L, new ProfileCreateRequest(ProfileRole.CHILD, "민준", 2018)))
                    .isInstanceOf(AccountException.class)
                    .extracting(exception -> ((AccountException) exception).getErrorCode())
                    .isEqualTo(AccountErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("계정의 프로필 목록 조회")
    class GetProfilesByAccount {

        @Test
        @DisplayName("리포지토리가 준 순서를 유지한 채 전부 반환한다")
        void returnsAllProfiles() {
            given(accountRepository.findById(1L)).willReturn(Optional.of(AccountFixtures.account(1L)));
            given(profileRepository.findAllByAccountIdOrderByCreatedAtDescIdDesc(1L)).willReturn(
                    List.of(AccountFixtures.childProfile(2L), AccountFixtures.parentsProfile(1L)));

            List<ProfileResponse> response = profileService.getProfilesByAccount(1L);

            assertThat(response).extracting(ProfileResponse::id).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("계정이 없으면 ACCOUNT_NOT_FOUND 예외가 발생한다")
        void throwsWhenAccountMissing() {
            given(accountRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfilesByAccount(99L))
                    .isInstanceOf(AccountException.class)
                    .extracting(exception -> ((AccountException) exception).getErrorCode())
                    .isEqualTo(AccountErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("글쓰기 가능한 아동 프로필 조회")
    class GetWritableChild {

        @Test
        @DisplayName("동의가 확인된 아동 프로필이면 그대로 반환한다")
        void returnsChildProfile() {
            given(profileRepository.findById(1L)).willReturn(Optional.of(AccountFixtures.childProfile(1L)));

            Profile profile = profileService.getWritableChild(1L);

            assertThat(profile.getId()).isEqualTo(1L);
            assertThat(profile.isChild()).isTrue();
        }

        @Test
        @DisplayName("프로필이 없으면 PROFILE_NOT_FOUND 예외가 발생한다")
        void throwsWhenProfileMissing() {
            given(profileRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getWritableChild(1L))
                    .isInstanceOf(AccountException.class)
                    .extracting(exception -> ((AccountException) exception).getErrorCode())
                    .isEqualTo(AccountErrorCode.PROFILE_NOT_FOUND);
        }

        @Test
        @DisplayName("보호자 프로필이면 NOT_CHILD_PROFILE 예외가 발생한다")
        void throwsWhenNotChild() {
            given(profileRepository.findById(2L)).willReturn(Optional.of(AccountFixtures.parentsProfile(2L)));

            assertThatThrownBy(() -> profileService.getWritableChild(2L))
                    .isInstanceOf(AccountException.class)
                    .extracting(exception -> ((AccountException) exception).getErrorCode())
                    .isEqualTo(AccountErrorCode.NOT_CHILD_PROFILE);
        }

        @Test
        @DisplayName("보호자 동의가 확인되지 않았다면 CONSENT_REQUIRED 예외가 발생한다")
        void throwsWhenConsentMissing() {
            given(profileRepository.findById(3L))
                    .willReturn(Optional.of(AccountFixtures.childProfileWithoutConsent(3L)));

            assertThatThrownBy(() -> profileService.getWritableChild(3L))
                    .isInstanceOf(AccountException.class)
                    .extracting(exception -> ((AccountException) exception).getErrorCode())
                    .isEqualTo(AccountErrorCode.CONSENT_REQUIRED);
        }
    }
}
