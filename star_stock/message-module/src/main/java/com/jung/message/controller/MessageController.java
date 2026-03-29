package com.jung.message.controller;

import com.jung.domain.message.SendMessageReq;
import com.jung.message.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MessageController {
    private final MessageService messageService;

    @PostMapping("/message")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestBody SendMessageReq req){
        String msg = req.getMsg();
        String url = req.getUrl();

        return messageService.sendMessage(msg, url);
    }

}
