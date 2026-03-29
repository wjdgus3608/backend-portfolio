package com.jung.app.realapp.service.strategy;

import com.jung.app.realapp.service.bot.GlobalBotControll;
import com.jung.app.realapp.service.bot.buy.CommonBuyBot;
import com.jung.app.realapp.service.bot.init.InitBot;
import com.jung.app.realapp.service.bot.sell.EarnSellBot;
import com.jung.app.realapp.service.bot.sell.ExitSellBot;
import com.jung.app.realapp.service.bot.sell.LostSellBot;
import com.jung.common.log.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class ShortTermStrategy implements CommonStrategy{

    private final CommonBuyBot commonBuyBot;
    private final EarnSellBot earnSellBot;
    private final LostSellBot lostSellBot;
    private final ExitSellBot exitSellBot;
    private final InitBot initBot;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
//        LogUtil.offSaveMode();
        LogUtil.printLog(this.getClass()+" is active");
        LogUtil.saveLog("ShortTermStrategy is active");
        if(GlobalBotControll.isOnceAtStartFlag()){
            initBot.initCoinMap();
            initBot.initAvoidMap();
            GlobalBotControll.setOnceAtStartFlag(false);
        }

        LocalTime time = LocalTime.now();
        //init 1시간마다 돌리기
        if(time.getMinute()>=00 &&  time.getMinute()<5){
            if(GlobalBotControll.isHourFlag()){
                initBot.initCoinMap();
                GlobalBotControll.setHourFlag(false);
            }
        }
        if(time.getMinute()>10 && time.getMinute()<15){
            GlobalBotControll.setHourFlag(true);
        }

        //오전,오후8시에 정리
        if((time.getHour()==23 || time.getHour()==11) && time.getMinute()>=00 &&  time.getMinute()<5){
            if(GlobalBotControll.isExitOnceFlag()){
                exitSellBot.sell();
                GlobalBotControll.setExitOnceFlag(false);
            }
        }
        if((time.getHour()==00 || time.getHour()==12) && time.getMinute()<10){
            GlobalBotControll.setExitOnceFlag(true);
        }

        GlobalBotControll.upCheckTimeCnt();

        commonBuyBot.buy();

        earnSellBot.sell();
        lostSellBot.sell();

        LogUtil.printLog(this.getClass()+" is end");
        LogUtil.saveLog("ShortTermStrategy is end");
        return null;
    }
}

////        LogUtil.offSaveMode();
//        if(GlobalBotControll.isOnceAtStartFlag()){
//                initBot.initCoinMap();
//                initBot.initAvoidMap();
//                GlobalBotControll.setOnceAtStartFlag(false);
//                }
//                GlobalBotControll.upCheckTimeCnt();
//                LogUtil.printLog(this.getClass()+" is active");
//                LogUtil.saveLog("ShortTermStrategy is active");
//        /*LocalTime time = LocalTime.now();
//        if(time.getHour()==23 && time.getMinute()==00){
//            exitSellBot.sell();
//        }*/
//                String[] time = GlobalBotControll.getNowTime();
//                if(!time[0].equals("00") || (Integer.parseInt(time[1])>=30))
//                commonBuyBot.buy();
//                else{
//                LogUtil.printLog("before 09:30 time");
//                LogUtil.saveLog("before 09:30 time");
//                }
//
//                earnSellBot.sell();
//                lostSellBot.sell();
//
//                LogUtil.printLog(this.getClass()+" is end");
//                LogUtil.saveLog("ShortTermStrategy is end");

