package com.spbtrediskill.secondskill.service;

public interface StockService {
    // 秒杀商品后减少库存
    void decrByStock(String stockName);
    // 秒杀商品前判断是否有库存
    Integer selectByExample(String stockName);
}
