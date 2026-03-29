package com.jung.logic.controller;

import com.jung.logic.service.log.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final LogService logService;

    @GetMapping("/logs")
    public ResponseEntity<?> getLogList(){
        List<String> logList = logService.retrieveLogList();
        log.info("getLogList : "+logList);
        return ResponseEntity.ok(logList);
    }

    @GetMapping("/log/{searchTime}")
    public ResponseEntity<?> getLogList(@PathVariable String searchTime){
        return ResponseEntity.ok(logService.retrieveDetailLog(searchTime));
    }
}
