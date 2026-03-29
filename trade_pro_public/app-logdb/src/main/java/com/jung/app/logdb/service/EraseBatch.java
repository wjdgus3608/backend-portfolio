package com.jung.app.logdb.service;

import com.jung.common.log.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EraseBatch implements Tasklet {
    @Autowired
    private LogDBService logDBService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        LogUtil.printLog("clean all logs!!");
        logDBService.deleteAllLog();
        return null;
    }
}
