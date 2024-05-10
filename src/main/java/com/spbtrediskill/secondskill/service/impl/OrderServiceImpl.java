package com.spbtrediskill.secondskill.service.impl;

import com.spbtrediskill.secondskill.mapper.OrderMapper;
import com.spbtrediskill.secondskill.pojo.Order;
import com.spbtrediskill.secondskill.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Override
    public void createOrder(Order order) {
        orderMapper.insert(order);
    }
}
