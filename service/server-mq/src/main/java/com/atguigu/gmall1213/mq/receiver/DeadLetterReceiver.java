package com.atguigu.gmall1213.mq.receiver;


import com.atguigu.gmall1213.mq.config.DeadLetterMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Configuration
public class DeadLetterReceiver {

    // 监听 队列二，
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void getMsg(String msg){
        System.out.println("接收数据：\t"+msg);

        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("接收队列二 queue_dead_2" + sf.format(new Date()) + msg);
    }
}
