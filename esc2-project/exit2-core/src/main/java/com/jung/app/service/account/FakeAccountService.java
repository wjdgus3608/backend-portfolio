package com.jung.app.service.account;

import com.jung.app.domain.vo.Position;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FakeAccountService implements AccountService{

    private double money = 100000;
    private List<Position> positionList = new ArrayList<>();


    @Override
    public List<Position> getPositionList() {
        return positionList;
    }

    @Override
    public boolean createPosition(Position position) {
        positionList.add(position);
        return true;
    }

    @Override
    public boolean deletePosition(Position position) {
        positionList.remove(position);
        return true;
    }

    @Override
    public double getMoney() {
        return money;
    }

    @Override
    public void setMoney(double money) {
        this.money = money;
    }

    @Override
    public double getTotalValue() {
        double value = 0;
        for(Position position : positionList){
            value+= (position.getOrderPrice()* position.getOrderAmount());
        }
        return value+money;
    }


}
