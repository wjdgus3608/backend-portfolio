package com.jung.common.box;

import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Vector;

public interface MyBox<T> {
    double getMoney();
    List<T> getCoins();
    String getBoxNow();
}
