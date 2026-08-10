package com.example.writegrow.domain.account.exception;

import com.example.writegrow.global.exception.BaseException;

public class AccountException extends BaseException {

    public AccountException(AccountErrorCode errorCode) {
        super(errorCode);
    }
}
