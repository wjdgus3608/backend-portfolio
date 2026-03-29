package com.jung.app.logdb.controller;

import com.jung.app.logdb.service.LogDBService;
import com.jung.domain.common.CommonHeader;
import com.jung.domain.common.StatusEnum;
import com.jung.app.logdb.entity.Log;
import com.jung.domain.log.LogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class LogDBController {
    private final LogDBService logDBService;

    @GetMapping("/{page}/{size}")
    public ResponseEntity<LogDto> readLog(@PathVariable("page") int page,
                                          @PathVariable("size") int size,
                                          Model model){
        List<Log> log = logDBService.readLog(page,size);
        LogDto logDto = LogDto.builder()
                .status(StatusEnum.OK)
                .data(log)
                .message(StatusEnum.OK.message)
                .build();
        HttpHeaders headers = CommonHeader.getHeader();
        return ResponseEntity.ok()
                .headers(headers)
                .body(logDto);
    }

    @GetMapping("/lastPage")
    public ResponseEntity<LogDto> readLastPage(Model model){
        List<Log> log = logDBService.readLastLogPage();
        LogDto logDto = LogDto.builder()
                .status(StatusEnum.OK)
                .data(log)
                .message(StatusEnum.OK.message)
                .build();
        HttpHeaders headers = CommonHeader.getHeader();
        return ResponseEntity.ok()
                .headers(headers)
                .body(logDto);
    }

    @GetMapping("/totalCnt")
    public ResponseEntity<Long> getTotalCnt(Model model){
        long totalCnt = logDBService.getTotalCnt();
        HttpHeaders headers = CommonHeader.getHeader();
        return ResponseEntity.ok()
                .headers(headers)
                .body(totalCnt);
    }

    @DeleteMapping("/")
    public ResponseEntity<LogDto> deleteAllLog(){
        logDBService.deleteAllLog();
        HttpHeaders headers = CommonHeader.getHeader();
        return ResponseEntity.ok()
                .headers(headers)
                .body(null);
    }

    @PostMapping("/log")
    public ResponseEntity<LogDto> insertLog(@RequestBody Map<String,String> log){
        logDBService.insertLog(log.get("msg"));
        HttpHeaders headers = CommonHeader.getHeader();
        return ResponseEntity.ok()
                .headers(headers)
                .body(null);
    }
}
