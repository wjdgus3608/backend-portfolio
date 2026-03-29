package com.jung.app.monitoringapp.service.strategy;

import com.jung.app.monitoringapp.service.bot.GlobalBotControll;
import com.jung.app.monitoringapp.service.bot.buy.CommonBuyBot;
import com.jung.app.monitoringapp.service.bot.buy.MACDBuyBot;
import com.jung.app.monitoringapp.service.bot.sell.EarnSellBot;
import com.jung.app.monitoringapp.service.bot.sell.ExitSellBot;
import com.jung.app.monitoringapp.service.bot.sell.LostSellBot;
import com.jung.common.log.LogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class ShortTermStrategy implements CommonStrategy{

    private final CommonBuyBot commonBuyBot;
    private final EarnSellBot earnSellBot;
    private final LostSellBot lostSellBot;
    private final ExitSellBot exitSellBot;
//    private final MACDBuyBot macdBuyBot;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

//        //test1
        LogUtil.printLog(this.getClass()+" is active");
        LogUtil.saveLog("ShortTermStrategy is active");


        LocalTime time = LocalTime.now();
        //8시에 정리
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


        //test2
//         LogUtil.printLog(this.getClass()+" is active");
//         LogUtil.saveLog("ShortTermStrategy is active");
//        //8시에 정리
//        LocalTime time = LocalTime.now();
//        if(time.getHour()==23 && time.getMinute()>=00 &&  time.getMinute()<5){
//            if(GlobalBotControll.isExitOnceFlag()){
//                exitSellBot.sell();
//                GlobalBotControll.setExitOnceFlag(false);
//            }
//        }
//        if(time.getHour()==00 && time.getMinute()<10){
//            GlobalBotControll.setExitOnceFlag(true);
//        }
//        //9시 30분 이후 매수
//        String[] timeStr = GlobalBotControll.getNowTime();
//        if(!timeStr[0].equals("00") || (Integer.parseInt(timeStr[1])>=30)){
//            //하락장 체크시 넣어줘야함
//            GlobalBotControll.upCheckTimeCnt();
//            commonBuyBot.buy();
//        }
//        else{
//        LogUtil.printLog("before 09:30 time");
//        LogUtil.saveLog("before 09:30 time MACD buy active");
//            macdBuyBot.buy();
//        }
//
//        earnSellBot.sell();
//        lostSellBot.sell();
//
//        LogUtil.printLog(this.getClass()+" is end");
//        LogUtil.saveLog("ShortTermStrategy is end");

        //test3

//        GlobalBotControll.upCheckTimeCnt();
//        LogUtil.printLog(this.getClass()+" is active");
//        LogUtil.saveLog("ShortTermStrategy is active");
//        String[] time = GlobalBotControll.getNowTime();
//        if(!time[0].equals("00") || (Integer.parseInt(time[1])>=30))
//            commonBuyBot.buy();
//        else{
//            LogUtil.printLog("before 09:30 time");
//            LogUtil.saveLog("before 09:30 time");
//        }
//
//        earnSellBot.sell();
//        lostSellBot.sell();
//
//        LogUtil.printLog(this.getClass()+" is end");
//        LogUtil.saveLog("ShortTermStrategy is end");

        return null;
    }
}
