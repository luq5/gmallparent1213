package com.atguigu.gmall1213.mq.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author mqx
 * @date 2020/6/29 11:17
 */
@Configuration
public class DelayedMqConfig {
    // 声明变量
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    @Bean
    public Queue delayQueue(){
        // 返回队列
        return  new Queue(queue_delay_1,true);
    }

    // 定义交换机
    @Bean
    public CustomExchange customExchange(){
        // 配置参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("x-delayed-type","direct");
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);

    }
    // 设置绑定关系
    @Bean
    public Binding delayBinding(){
        return BindingBuilder.bind(delayQueue()).to(customExchange()).with(routing_delay).noargs();
    }
}
