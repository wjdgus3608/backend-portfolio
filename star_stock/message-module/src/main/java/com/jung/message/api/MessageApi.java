package com.jung.message.api;

import org.springframework.http.ResponseEntity;

public interface MessageApi {
    boolean healthCheck();
    ResponseEntity<?> sendMessage(String msg, String linkUrl);
}
