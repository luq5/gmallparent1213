package com.atguigu.gmall1213.cart.client;

import com.atguigu.gmall1213.cart.client.impl.CartDegradeFeignClient;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.cart.CartInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@FeignClient(name = "service-cart",fallback = CartDegradeFeignClient.class)
public interface CartFeignClient {

    //添加购物车
    @PostMapping("/api/cart/addToCart/{skuId}/{skuNum}")
    Result addToCart(@PathVariable Long skuId, @PathVariable Integer skuNum);


    /**
     * 根据用户Id 查询购物车列表
     * @param userId
     * @return
     */
    @GetMapping("/api/cart/getCartCheckedList/{userId}")
    List<CartInfo> getCartCheckedList(@PathVariable("userId") String userId);

    @GetMapping("/api/cart/loadCartCache/{userId}")
    Result loadCartCache(@PathVariable("userId") String userId);



}
