package com.example.writegrow.support;

import com.example.writegrow.domain.account.entity.Account;
import com.example.writegrow.domain.account.entity.Profile;
import com.example.writegrow.domain.account.entity.ProfileRole;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 계정·프로필 테스트 픽스처. 영속화 없이 ID 를 가진 엔티티를 만들기 위해 리플렉션을 사용한다.
 */
public final class AccountFixtures {

    private AccountFixtures() {
    }

    public static Account account(Long id) {
        Account account = Account.create("민준이네 가족");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    public static Profile childProfile(Long id) {
        return profile(id, ProfileRole.CHILD, true);
    }

    public static Profile parentsProfile(Long id) {
        return profile(id, ProfileRole.PARENTS, true);
    }

    public static Profile childProfileWithoutConsent(Long id) {
        return profile(id, ProfileRole.CHILD, false);
    }

    /**
     * 다른 계정에 속한 프로필. 계정 경계를 넘는 열람을 검증할 때 쓴다.
     */
    public static Profile profileInAccount(Long id, ProfileRole role, Long accountId) {
        Profile profile = Profile.create(
                account(accountId), role, role == ProfileRole.CHILD ? "민준" : "엄마", 2018);
        ReflectionTestUtils.setField(profile, "id", id);
        return profile;
    }

    public static Profile profile(Long id, ProfileRole role, boolean consentConfirmed) {
        Profile profile = Profile.create(account(1L), role, role == ProfileRole.CHILD ? "민준" : "엄마", 2018);
        ReflectionTestUtils.setField(profile, "id", id);
        ReflectionTestUtils.setField(profile, "consentConfirmed", consentConfirmed);
        return profile;
    }

    public static void setId(Object entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
