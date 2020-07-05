package com.atguigu.gmall1213.mq.receiver;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Configuration
public class ConfirmReceiver {

    // 消息接收者
//    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "true"),
            key = {"routing.confirm"}
    ))
    public void process(Message message, Channel channel){
        // 获取消息
        System.out.println("msg:\t"+new String(message.getBody()));
        // 确认消息 第二个参数表示每次确认一个消息。
        try {
            int i = 1/0; // 出现异常
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            System.out.println("有异常了！"+e.getMessage());

            // 判断是否已经处理过一次消息
            if (message.getMessageProperties().getRedelivered()){
                System.out.println("消息已经被处理过！");

                // 给一个拒绝消息
                try {
                    channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }else {
                System.out.println("消息积极返回队列！");
                // 第三个表示 true 表示重回队列！
                try {
                    channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,false);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
