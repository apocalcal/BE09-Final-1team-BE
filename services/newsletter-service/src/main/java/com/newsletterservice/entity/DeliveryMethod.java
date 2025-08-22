package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeliveryMethod {
    EMAIL("이메일"),
    PUSH("푸시 알림"),
    SMS("문자 메시지");

    private final String description;
}