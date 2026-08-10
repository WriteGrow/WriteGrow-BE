package com.example.writegrow.domain.account.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 역할 (CHILD: 아동, PARENTS: 보호자)")
public enum ProfileRole {

    CHILD,
    PARENTS
}
