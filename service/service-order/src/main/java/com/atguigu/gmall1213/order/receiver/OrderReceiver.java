package com.atguigu.gmall1213.order.receiver;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.model.enums.PaymentStatus;
import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.model.payment.PaymentInfo;
import com.atguigu.gmall1213.order.service.OrderService;
import com.atguigu.gmall1213.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;

    @Autowired
    private RabbitService rabbitService;

    // 监听消息时获取订单Id
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId, Message message , Channel channel){
        // 判断订单Id 是否为空！
        if (null!=orderId){
            // 为了防止重复消息这个消息。判断订单状态
            // 通过订单Id 来获取订单对象 select * from orderInfo where id = orderId
            OrderInfo orderInfo = orderService.getById(orderId);
            // 订单状态是未支付
            if (null!= orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.name())){
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                if (null!=paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                    // 先查看是否有交易记录 {用户是否扫了二维码}
                    Boolean aBoolean = paymentFeignClient.checkPayment(orderId);
                    if(aBoolean){
                        // 有交易记录 ，关闭支付宝 防止用户在过期时间到的哪一个瞬间，付款。
                        Boolean flag = paymentFeignClient.closePay(orderId);
                        if (flag){
                            // 用户未付款 ，开始关闭订单，关闭交易记录 2:表示要关闭交易记录paymentInfo 中有数据
                            orderService.execExpiredOrder(orderId,"2");
                        }else{
                            // 用户已经付款
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,orderId);
                        }
                    }else{
                        // 在支付宝中没有交易记录，但是在电商中有交易记录
                        orderService.execExpiredOrder(orderId,"2");
                    }
                }else {
                    // 也就是说在paymentInfo 中根本没有交易记录。
                    orderService.execExpiredOrder(orderId,"1");
                }
            }
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void updOrder(Long orderId ,Message message, Channel channel){
        // 判断orderId 不为空
        if (null!=orderId){
            // 更新订单的状态，还有进度的状态
            OrderInfo orderInfo = orderService.getById(orderId);
            // 判断状态
            if (null!= orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                // 才准备更新数据
                orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                // 发送消息通知库存，准备减库存！
                orderService.sendOrderStatus(orderId);

            }
        }
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    //准备减库存状态
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updOrderStatus(String msgJson,Message message,Channel channel){
        if (StringUtils.isEmpty(msgJson)){
            Map map= JSON.parseObject(msgJson,Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");
            // 根据status 判断减库存结果
            if ("DEDUCTED".equals(status)){
                // 减库存成功！更新订单的状态
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);
            }else {
                // ‘OUT_OF_STOCK’  (库存超卖)
                // 库存超卖了，那么如何处理？
                // 第一种：调用其他仓库货物进行补货。 想办法补货，补库存。
                // 发送消息重新更新一下减库存的结果！
                // 第二种：人工客服介入，给你退款{昨天用的功能}
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }
        }
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
