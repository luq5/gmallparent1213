package com.atguigu.gmall1213.order.receiver;

import com.atguigu.gmall1213.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class OrderCanelMqConfig {

    // 创建一个队列
    @Bean
    public Queue delayQueueOrder(){
        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true);
    }

    // 创建自定义交换机
    @Bean
    public CustomExchange customExchange(){
        // 配置参数
        HashMap<String, Object> map = new HashMap<>();
        map.put("x-delayed-type","direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",true,false,map);
    }

    // 设置绑定关系
    @Bean
    public Binding delayBinding(){
        return BindingBuilder.bind(delayQueueOrder()).to(customExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
