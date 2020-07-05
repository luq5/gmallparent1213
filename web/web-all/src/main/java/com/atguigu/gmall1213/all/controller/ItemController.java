package com.atguigu.gmall1213.all.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.item.client.ItemFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
public class ItemController {

    @Autowired
    private ItemFeignClient itemFeignClient;

    @RequestMapping("{skuId}.html")
    public String getItem(@PathVariable Long skuId,Model model){
        Result<Map> result = itemFeignClient.getItem(skuId);
        model.addAllAttributes(result.getData());
        //返回商品详情页
        return "item/index";
    }

}
