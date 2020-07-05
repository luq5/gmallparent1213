package com.atguigu.gmall1213.order.service.impl;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.common.constant.MqConst;
import com.atguigu.gmall1213.common.service.RabbitService;
import com.atguigu.gmall1213.common.util.HttpClientUtil;
import com.atguigu.gmall1213.model.enums.OrderStatus;
import com.atguigu.gmall1213.model.enums.ProcessStatus;
import com.atguigu.gmall1213.model.order.OrderDetail;
import com.atguigu.gmall1213.model.order.OrderInfo;
import com.atguigu.gmall1213.order.mapper.OrderDetailMapper;
import com.atguigu.gmall1213.order.mapper.OrderInfoMapper;
import com.atguigu.gmall1213.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${ware.url}")
    private String WareUrl;

    @Autowired
    private RabbitService rabbitService;


    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        orderInfo.sumTotalAmount();

        String outTradNo = "ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradNo);

        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        StringBuffer sb = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList) {
              sb.append(orderDetail.getSkuName());
        }
        if (sb.toString().length()>100){
            orderInfo.setTradeBody(sb.toString().substring(0,100));
        }else {
            orderInfo.setTradeBody(sb.toString());
        }

        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //订单进度状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());

        orderInfoMapper.insert(orderInfo);

        if (!CollectionUtils.isEmpty(orderDetailList)){
            for (OrderDetail orderDetail : orderDetailList) {
                orderDetail.setOrderId(orderInfo.getId());
                orderDetailMapper.insert(orderDetail);
            }
        }
        //保存完成之后发送消息
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,MqConst.ROUTING_ORDER_CANCEL,
                orderInfo.getId(),MqConst.DELAY_TIME);
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        //定义一个key
        String tradeNoKey = "user:"+userId+":tradeNo";
        //生产流水号
        String tradeNo = UUID.randomUUID().toString();
        //放入缓存
        redisTemplate.opsForValue().set(tradeNoKey,tradeNo);
        return tradeNo;
    }

    @Override
    public boolean checkTradeNo(String tradeNo, String userId) {
        String tradeNoKey = "user:"+userId+":tradeNo";
        String tradeNoRedis = (String)  redisTemplate.opsForValue().get(tradeNoKey);
        return tradeNo.equals(tradeNoRedis);
    }

    @Override
    public void deleteTradeNo(String userId) {
        String tradeNoKey = "user:"+userId+":tradeNo";
        redisTemplate.delete(tradeNoKey);

    }

    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        String result = HttpClientUtil.doGet(WareUrl + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }

    @Override
    public void execExpiredOrder(Long orderId) {
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
    }

    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        // 创建对象
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        // 订单的状态，可以通过进度状态来获取
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfo.setProcessStatus(processStatus.name());
        orderInfoMapper.updateById(orderInfo);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", orderId));
        orderInfo.setOrderDetailList(orderDetailList);
        return null;
    }

    @Override
    public void sendOrderStatus(Long orderId) {
        // 更改订单的状态，变成通知仓库准备发货
        updateOrderStatus(orderId,ProcessStatus.NOTIFIED_WARE);
        // 需要参考库存管理文档 根据管理手册。
        // 发送的数据 是 orderInfo 中的部分属性数据，并非全部属性数据！
        // 获取发送的字符串：
        String wareJson = initWareOrder(orderId);
        // 准备发送消息
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_WARE_STOCK,MqConst.ROUTING_WARE_STOCK,wareJson);
    }

    public String initWareOrder(Long orderId) {
        // 首先查询到orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将orderInfo 中的部分属性，放入一个map 集合中。
        Map map = initWareOrder(orderInfo);
        // 返回json 字符串
        return JSON.toJSONString(map);
    }


    // 将orderInfo 部分数据组成map
    public Map initWareOrder(OrderInfo orderInfo) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
        map.put("paymentWay", "2");
        // map.put("wareId", orderInfo.getWareId());// 仓库Id ，减库存拆单时需要使用！
        /*
            details 对应的是订单明细
            details:[{skuId:101,skuNum:1,skuName:’小米手64G’},
                       {skuId:201,skuNum:1,skuName:’索尼耳机’}]
         */
        // 声明一个list 集合 来存储map
        List<Map> maps = new ArrayList<>();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 先声明一个map 集合
            HashMap<String, Object> orderDetailMap = new HashMap<>();
            orderDetailMap.put("skuId",orderDetail.getSkuId());
            orderDetailMap.put("skuNum",orderDetail.getSkuNum());
            orderDetailMap.put("skuName",orderDetail.getSkuName());
            maps.add(orderDetailMap);
        }
        map.put("details", maps);
        // 返回构成好的map集合。
        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(Long orderId, String wareSkuMap) {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // wareSkuMap 编程集合map
        List<Map> mapList = JSON.parseArray(wareSkuMap, Map.class);
        // 子订单根据什么来创建
        for (Map map : mapList) {
            // 获取map 中的仓库Id
            String wareId = (String) map.get("wareId");
            // 获取仓库Id 对应的商品 Id
            List<String> skuIdList = (List<String>) map.get("skuIds");
            OrderInfo subOrderInfo = new OrderInfo();
            // 属性拷贝，原始订单的基本数据，都可以给子订单使用
            BeanUtils.copyProperties(orderInfoOrigin,subOrderInfo);
            // id 不能拷贝，发送主键冲突
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            // 赋值一个仓库Id
            subOrderInfo.setWareId(wareId);
            // 计算总金额 在订单的实体类中有sumTotalAmount() 方法。
            // 声明一个子订单明细集合
            List<OrderDetail> orderDetails = new ArrayList<>();

            // 需要将子订单的名单明细准备好,添加到子订单中
            // 子订单明细应该来自于原始订单明细。
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            if (!CollectionUtils.isEmpty(orderDetailList)){
                // 遍历原始的订单明细
                for (OrderDetail orderDetail : orderDetailList) {
                    // 再去遍历仓库中所对应的商品Id
                    for (String skuId : skuIdList) {
                        // 比较两个商品skuId ，如果相同，则这个商品就是子订单明细需要的商品
                        if (Long.parseLong(skuId)==orderDetail.getSkuId()){
                            orderDetails.add(orderDetail);
                        }
                    }
                }
            }
            // 需要将子订单的名单明细准备好,添加到子订单中
            subOrderInfo.setOrderDetailList(orderDetails);
            // 获取到总金额
            subOrderInfo.sumTotalAmount();
            // 保存子订单
            saveOrderInfo(subOrderInfo);

            // 将新的子订单放入集合中
            subOrderInfoList.add(subOrderInfo);
        }
        // 更新原始订单的状态
        updateOrderStatus(orderId,ProcessStatus.SPLIT);
        return subOrderInfoList;
    }

    @Override
    public void execExpiredOrder(Long orderId, String flag) {
        updateOrderStatus(orderId,ProcessStatus.CLOSED);
        if ("2".equals(flag)){
            // 发送信息关闭支付宝交易
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_CLOSE,MqConst.ROUTING_PAYMENT_CLOSE,orderId);
        }
    }
}
