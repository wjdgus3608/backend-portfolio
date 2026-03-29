package com.jung.app.service.account;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jung.app.common.TimeUtil;
import com.jung.app.domain.vo.Position;
import com.jung.app.domain.vo.Stock;
import com.jung.app.domain.vo.em.LongShortType;
import com.jung.app.service.api.BinanceSigner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class RealAccountService implements AccountService{

    @Override
    public List<Position> getPositionList() {
        return null;
    }

    @Override
    public double getMoney() {
        return 0;
    }

    /**
     * 총 자산 가치 (평가금 포함)
     */
    @Override
    public double getTotalValue() {
        double valueSum = 0;
        List<Position> positionList = getPositionList();
        for(Position position : positionList){
            valueSum+=(position.getOrderAmount()*position.getOrderPrice());
        }
        return getMoney()+valueSum;
    }

    //아래는 불필요
    @Override
    public void setMoney(double money) {

    }



    @Override
    public boolean createPosition(Position position) {
        return false;
    }

    @Override
    public boolean deletePosition(Position position) {
        return false;
    }


}
