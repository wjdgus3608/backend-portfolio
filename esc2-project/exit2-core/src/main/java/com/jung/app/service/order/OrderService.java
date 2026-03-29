package com.jung.app.service.order;

import com.jung.app.domain.vo.Order;
import com.jung.app.domain.vo.em.OrderStatus;

import java.util.List;

public interface OrderService {
    Order createOrder(Order order);
    boolean deleteOrder(long orderId);
    boolean deleteOrder(Order order);
    Order modifyOrder(long orderId, double newPrice);
    Order getOrderById(long orderId);
    List<Order> getAllOrders();
    OrderStatus checkOrderFilled(long orderId);
}
