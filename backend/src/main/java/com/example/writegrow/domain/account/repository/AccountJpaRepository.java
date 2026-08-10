package com.example.writegrow.domain.account.repository;

import com.example.writegrow.domain.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data 구현체. {@link AccountRepositoryImpl} 내부에서만 사용한다.
 */
interface AccountJpaRepository extends JpaRepository<Account, Long> {
}
