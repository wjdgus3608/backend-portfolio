package com.jung.app.service.order;

import com.jung.app.domain.vo.Order;
import com.jung.app.domain.vo.em.OrderStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FakeOrderService implements OrderService{

    private List<Order> orderList = new ArrayList<>();


    @Override
    public Order createOrder(Order order) {
        orderList.add(order);
        return null;
    }

    @Override
    public boolean deleteOrder(long orderId) {
        return false;
    }

    @Override
    public boolean deleteOrder(Order order) {
        orderList.remove(order);
        return true;
    }

    @Override
    public Order modifyOrder(long orderId, double newPrice) {
        return null;
    }

    @Override
    public Order getOrderById(long orderId) {
        return null;
    }

    @Override
    public List<Order> getAllOrders() {
        return orderList;
    }

    @Override
    public OrderStatus checkOrderFilled(long orderId) {
        return null;
    }
}
