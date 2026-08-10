package com.example.writegrow.domain.account.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 계정에 속한 사용자 프로필. 글쓰기 주체는 항상 {@link ProfileRole#CHILD} 프로필이다.
 */
@Getter
@Entity
@Table(name = "profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ProfileRole role;

    @Column(name = "nickname", nullable = false, length = 30)
    private String nickname;

    @Column(name = "birth_year")
    private Integer birthYear;

    /**
     * 보호자 동의 확인 여부. REQ-08 구현 전까지는 항상 true 로 생성된다.
     */
    @Column(name = "consent_confirmed", nullable = false)
    private boolean consentConfirmed;

    private Profile(Account account, ProfileRole role, String nickname, Integer birthYear) {
        this.account = account;
        this.role = role;
        this.nickname = nickname;
        this.birthYear = birthYear;
        this.consentConfirmed = true;
    }

    public static Profile create(Account account, ProfileRole role, String nickname, Integer birthYear) {
        return new Profile(account, role, nickname, birthYear);
    }

    public boolean isChild() {
        return role == ProfileRole.CHILD;
    }
}
