package com.jung.domain.message;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class SendMessageReq implements Serializable {
    private String msg;
    private String url;
}
