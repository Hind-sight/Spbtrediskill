package com.spbtrediskill.secondskill.controller;

import com.spbtrediskill.secondskill.config.MyRabbitMQConfig;
import com.spbtrediskill.secondskill.pojo.Order;
import com.spbtrediskill.secondskill.service.OrderService;
import com.spbtrediskill.secondskill.service.RedisService;
import com.spbtrediskill.secondskill.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
@Controller
@Slf4j
public class SecController {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisService redisService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private StockService stockService;
    /**
     * 使用redis+消息队列进行秒杀实现
     *
     * @param username
     * @param stockName
     * @return
     */
    @PostMapping( value = "/sec",produces = "application/json;charset=utf-8")
    @ResponseBody
    public String sec(@RequestParam(value = "username") String username, @RequestParam(value = "stockName") String stockName) {
        log.info("参加秒杀的用户是：{}，秒杀的商品是：{}", username, stockName);
        String message = null;
        //调用redis给相应商品库存量减一
        Long decrByResult = redisService.decrBy(stockName);
        if (decrByResult >= 0) {
            /**
             * 说明该商品的库存量有剩余，可以进行下订单操作
             */
            log.info("用户：{}秒杀该商品：{}库存有余，可以进行下订单操作", username, stockName);
            //发消息给库存消息队列，将库存数据减一
            rabbitTemplate.convertAndSend(MyRabbitMQConfig.STORY_EXCHANGE, MyRabbitMQConfig.STORY_ROUTING_KEY, stockName);

            //发消息给订单消息队列，创建订单
            Order order = new Order();
            order.setOrder_name(stockName);
            order.setOrder_user(username);
            rabbitTemplate.convertAndSend(MyRabbitMQConfig.ORDER_EXCHANGE, MyRabbitMQConfig.ORDER_ROUTING_KEY, order);
            message = "用户" + username + "秒杀" + stockName + "成功";
        } else {
            /**
             * 说明该商品的库存量没有剩余，直接返回秒杀失败的消息给用户
             */
            log.info("用户：{}秒杀时商品的库存量没有剩余,秒杀结束", username);
            message = "用户："+ username + "商品的库存量没有剩余,秒杀结束";
        }
        return message;
    }
}


