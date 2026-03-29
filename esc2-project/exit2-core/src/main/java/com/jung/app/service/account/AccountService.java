package com.jung.app.service.account;

import com.jung.app.domain.vo.Order;
import com.jung.app.domain.vo.Position;

import java.util.List;

public interface AccountService {
    List<Position> getPositionList();
    boolean createPosition(Position position);
    boolean deletePosition(Position position);
    double getMoney();
    void setMoney(double money);
    double getTotalValue();
}
