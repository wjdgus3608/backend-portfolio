package com.jung.app.boxlogapp.service.strategy;

import com.jung.app.boxlogapp.service.box.GlobalBotControll;
import com.jung.common.box.MyBox;
import com.jung.common.log.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShortTermStrategy implements CommonStrategy{

    private final MyBox myBox;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        initAvoidMap();
        LogUtil.saveLog(myBox.getBoxNow());
        return null;
    }

    public void initAvoidMap(){
        for(String cName: GlobalBotControll.avoidCoinArray)
            GlobalBotControll.addCoinToAvoidSet(cName);
    }
}
