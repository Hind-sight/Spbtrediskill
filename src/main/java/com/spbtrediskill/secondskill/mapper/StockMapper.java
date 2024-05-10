package com.spbtrediskill.secondskill.mapper;

import com.spbtrediskill.secondskill.base.service.GenericMapper;
import com.spbtrediskill.secondskill.pojo.Order;
import com.spbtrediskill.secondskill.pojo.Stock;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface StockMapper extends GenericMapper<Stock> {
    void insertStock(Stock stock);
}


