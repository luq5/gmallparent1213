package com.atguigu.gmall1213.all.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("trade.html")
    public String trade(Model model){
        Result<Map<String, Object>> result = orderFeignClient.trade();
        model.addAllAttributes(result.getData());
        return "order/trade";
    }
}
