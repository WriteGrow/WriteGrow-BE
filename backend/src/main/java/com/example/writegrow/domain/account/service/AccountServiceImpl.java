package com.example.writegrow.domain.account.service;

import com.example.writegrow.domain.account.dto.request.AccountCreateRequest;
import com.example.writegrow.domain.account.dto.response.AccountResponse;
import com.example.writegrow.domain.account.entity.Account;
import com.example.writegrow.domain.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public AccountResponse create(AccountCreateRequest request) {
        Account account = accountRepository.save(Account.create(request.name()));
        return AccountResponse.from(account);
    }
}
