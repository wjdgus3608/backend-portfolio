package com.jung.app.webapp.controller;

import com.jung.common.log.LogUtil;
 import com.jung.domain.log.LogVO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class WebPageController {
    @GetMapping("/{page}")
    public String doWatch(Model model, @PathVariable("page") int page){
        String strLogs = LogUtil.readLog(page,100);
        List<LogVO> logs = LogVO.stringToLogList(strLogs);
        model.addAttribute("logs",logs);
        model.addAttribute("totalCnt",Long.parseLong(LogUtil.getTotalLogCnt()));
        return "index";
    }

    @GetMapping("/lastPage")
    public String doWatchLast(Model model){
        String strLogs = LogUtil.readLastLog();
        List<LogVO> logs = LogVO.stringToLogList(strLogs);
        model.addAttribute("logs",logs);
        model.addAttribute("totalCnt",Long.parseLong(LogUtil.getTotalLogCnt()));
        return "index";
    }
}
