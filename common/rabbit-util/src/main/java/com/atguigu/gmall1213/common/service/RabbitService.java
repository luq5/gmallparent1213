package com.atguigu.gmall1213.common.service;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送消息方法
     * @param exchange 交换机
     * @param routingKey 路由键
     * @param msg 消息
     * @return
     */
    public boolean sendMessage(String exchange,String routingKey,Object msg){
        rabbitTemplate.convertAndSend(exchange,routingKey,msg);
        return true;
    }

    public boolean sendDelayMessage(String exchange,String routingKey,Object msg,int delayTime){
        //发送延迟消息
        rabbitTemplate.convertAndSend(exchange, routingKey, msg, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                message.getMessageProperties().setDelay(delayTime*1000);
                return message;
            }
        });
        return true;
    }




}
