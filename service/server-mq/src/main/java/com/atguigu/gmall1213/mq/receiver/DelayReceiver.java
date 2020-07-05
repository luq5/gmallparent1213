package com.atguigu.gmall1213.mq.receiver;

import com.atguigu.gmall1213.mq.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mqx
 * @date 2020/6/29 11:29
 */
@Component
@Configuration
public class DelayReceiver {

    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void getMsg(String msg){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("监听到的消息：queue_delay_1"+msg+ "接收的时间：\t"+sf.format(new Date()));
    }

}
