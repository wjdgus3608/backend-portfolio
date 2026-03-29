package com.jung.domain.common;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StatusEnum {
    OK(200,"성공");

    public int code;
    public String message;
}
