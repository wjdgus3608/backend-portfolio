package com.jung.message.service;

import com.jung.message.api.MessageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageApi messageApi;

    public ResponseEntity<?> sendMessage(String msg, String url){
        return messageApi.sendMessage(msg, url);
    }


}
