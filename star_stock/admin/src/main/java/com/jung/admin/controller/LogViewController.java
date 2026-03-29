package com.jung.admin.controller;

import com.jung.admin.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LogViewController {

    private final LogService logService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String getLogListPage(Model model){
        ResponseEntity<?> responseEntity = logService.getLogList();
        model.addAttribute("resultList",responseEntity.getBody());
        return "index";
    }

    @RequestMapping(value = "/detail-view.do")
    public String getLogDetailPage(Model model, @RequestParam String searchTime){
        log.info("searchTime : "+searchTime);
        ResponseEntity<?> responseEntity = logService.getLogDetail(searchTime);
        model.addAttribute("searchTime",searchTime);
        model.addAttribute("result",responseEntity.getBody());
        return "detail";
    }
}
