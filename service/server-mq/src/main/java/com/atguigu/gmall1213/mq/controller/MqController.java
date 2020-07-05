package com.atguigu.gmall1213.mq.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.mq.config.DeadLetterMqConfig;
import com.atguigu.gmall1213.mq.config.DelayedMqConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/mq")
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private  RabbitTemplate rabbitTemplate;

    // 发送消息的方法
    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 来自rabbit-util 中 RabbitService的对象
        rabbitService.sendMessage("exchange.confirm","routing.confirm",
                simpleDateFormat.format(new Date()));
        return Result.ok();
    }

    //测试死信队列
    @GetMapping("sendDeadLettle")
    public   Result sendDeadLettle(){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,
                "ok",message -> {
            //设置发送队列消息的时间
                    message.getMessageProperties().setExpiration(1000*10+"");
                    System.out.println(new Date() + "Delay sent.");
                    return message;
                });
        return Result.ok();
    }

    @GetMapping("sendDelay")
    public Result sendDelay(){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 发送消息
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay,
                sf.format(new Date()), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 设置延迟时间 10 秒钟
                        message.getMessageProperties().setDelay(10*1000);
                        System.out.println(sf.format(new Date()) + "  \t Delay sent....");
                        return message;
                    }
                });

        return Result.ok();
    }

}
