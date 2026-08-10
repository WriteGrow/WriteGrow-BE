package com.example.writegrow.domain.account.repository;

import com.example.writegrow.domain.account.entity.Account;
import java.util.Optional;

/**
 * 도메인이 필요로 하는 계정 저장소 계약. Spring Data 인터페이스는 상위 레이어로 노출하지 않는다.
 */
public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findById(Long id);
}
