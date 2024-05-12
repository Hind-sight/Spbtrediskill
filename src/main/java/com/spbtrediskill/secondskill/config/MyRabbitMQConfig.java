package com.spbtrediskill.secondskill.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;
@Configuration
public class MyRabbitMQConfig {

    //库存交换机
    public static final String STORY_EXCHANGE = "STORY_EXCHANGE";

    //订单交换机
    public static final String ORDER_EXCHANGE = "ORDER_EXCHANGE";

    //请求消息交换机
    public static final String REQUEST_EXCHANGE = "REQUEST_EXCHANGE";

    //库存队列
    public static final String STORY_QUEUE = "STORY_QUEUE";

    //订单队列
    public static final String ORDER_QUEUE = "ORDER_QUEUE";

    //请求队列
    public static final String REQUEST_QUEUE = "REQUEST_QUEUE";

    //库存路由键
    public static final String STORY_ROUTING_KEY = "STORY_ROUTING_KEY";

    //订单路由键
    public static final String ORDER_ROUTING_KEY = "ORDER_ROUTING_KEY";

    //请求路由键
    public static final String REQUEST_ROUTING_KEY = "REQUEST_ROUTING_KEY";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    //创建库存交换机
    @Bean
    public Exchange getStoryExchange() {
        return ExchangeBuilder.directExchange(STORY_EXCHANGE).durable(true).build();
    }
    //创建库存队列
    @Bean
    public Queue getStoryQueue() {
        return new Queue(STORY_QUEUE);
    }
    //库存交换机和库存队列绑定
    @Bean
    public Binding bindStory() {
        return BindingBuilder.bind(getStoryQueue()).to(getStoryExchange()).with(STORY_ROUTING_KEY).noargs();
    }
    //创建订单队列
    @Bean
    public Queue getOrderQueue() {
        return new Queue(ORDER_QUEUE);
    }
    //创建订单交换机
    @Bean
    public Exchange getOrderExchange() {
        return ExchangeBuilder.directExchange(ORDER_EXCHANGE).durable(true).build();
    }
    //订单队列与订单交换机进行绑定
    @Bean
    public Binding bindOrder() {
        return BindingBuilder.bind(getOrderQueue()).to(getOrderExchange()).with(ORDER_ROUTING_KEY).noargs();
    }
    //创建请求队列
    @Bean
    public Queue getRequestQueue() {
        // 创建一个具有最大长度的队列
        Map<String, Object> args = new HashMap<>();
        args.put("x-max-length", 20);
        args.put("x-overflow", "reject-publish");
        return QueueBuilder.durable(REQUEST_QUEUE).withArguments(args).build();
    }
    //创建请求交换机
    @Bean
    public Exchange getRequestExchange() {
        return ExchangeBuilder.directExchange(REQUEST_EXCHANGE).durable(true).build();
    }
    //请求队列与请求交换机进行绑定
    @Bean
    public Binding bindRequest() {
        return BindingBuilder.bind(getRequestQueue()).to(getRequestExchange()).with(REQUEST_EXCHANGE).noargs();
    }

}


