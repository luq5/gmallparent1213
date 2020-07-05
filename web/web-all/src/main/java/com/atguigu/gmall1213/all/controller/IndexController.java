package com.atguigu.gmall1213.all.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class IndexController {
    @Autowired
    private ProductFeignClient productFeignClient;
    @GetMapping({"/","index.html"})
    public String index(HttpServletRequest request){
        Result result = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",result.getData());
        return "index/index";
    }


}
