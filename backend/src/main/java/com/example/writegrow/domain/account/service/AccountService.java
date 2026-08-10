package com.example.writegrow.domain.account.service;

import com.example.writegrow.domain.account.dto.request.AccountCreateRequest;
import com.example.writegrow.domain.account.dto.response.AccountResponse;

public interface AccountService {

    AccountResponse create(AccountCreateRequest request);
}
