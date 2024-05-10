package com.spbtrediskill.secondskill.mapper;

import com.spbtrediskill.secondskill.base.service.GenericMapper;
import com.spbtrediskill.secondskill.pojo.Order;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface OrderMapper extends GenericMapper<Order> {
    void insertOrder(Order order);
}

