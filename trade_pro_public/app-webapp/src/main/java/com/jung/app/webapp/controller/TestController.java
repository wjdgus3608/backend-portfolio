package com.jung.app.webapp.controller;

import com.jung.common.log.LogUtil;
import com.jung.domain.common.CommonHeader;
import com.jung.domain.log.LogDto;
import com.jung.domain.log.LogVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class TestController {
    @GetMapping("/{page}/{size}")
    public ResponseEntity<LogDto> readTest(@PathVariable("page") int page, @PathVariable("size") int size){
        List<LogVO> logs = LogVO.stringToLogList(LogUtil.readLog(page,size));
        LogDto logDto = LogDto.logListToLogDto(logs);
        return ResponseEntity.ok()
                .headers(CommonHeader.getHeader())
                .body(logDto);
    }

    @PostMapping("/post")
    public ResponseEntity<LogDto> saveTest(@RequestBody Map<String,String> json){
        LogUtil.saveLog(json.get("log"));
        return ResponseEntity.ok()
                .headers(CommonHeader.getHeader())
                .body(null);
    }
}
