package com.example.writegrow.domain.account.repository;

import com.example.writegrow.domain.account.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
