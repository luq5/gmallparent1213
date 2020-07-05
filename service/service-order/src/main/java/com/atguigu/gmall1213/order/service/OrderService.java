package com.atguigu.gmall1213.order.service;

import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<OrderInfo> {

    //保存订单
    Long saveOrderInfo(OrderInfo orderInfo);
    //获取流水号
    String getTradeNo(String userId);
    //比较流水号
    boolean checkTradeNo(String tradeNo,String userId);
    //删除流水号
    void  deleteTradeNo(String userId);

    /**
     * 验证库存
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

//关闭过期订单
    void execExpiredOrder(Long orderId);

    //更新订单的方法
    void updateOrderStatus (Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单Id 查询订单信息
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);
    //发送消息减库存
    void sendOrderStatus(Long orderId);
    //转换为map集合
    Map initWareOrder(OrderInfo orderInfo);

    List<OrderInfo> orderSplit(Long orderId, String wareSkuMap);

    //关闭过期订单
    void execExpiredOrder(Long orderId, String flag);
}
