package com.atguigu.gmall1213.cart.controller;

import com.atguigu.gmall1213.cart.service.CartService;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.AuthContextHolder;
import com.atguigu.gmall1213.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request){

        //获取用户ID
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            // 属于未登录时，添加购物车，会产生一个临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId,userId,skuNum);

        return Result.ok();
    }

    // 获取购物车列表
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        // 获取登录的用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取临时用户Id
        String userTempId = AuthContextHolder.getUserTempId(request);

        List<CartInfo> cartList = cartService.getCartList(userId, userTempId);

        // 返回数据
        return Result.ok(cartList);

    }

    //更改选中状态的控制器
    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request){
        //登录和未登录的情况下都可以
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.checkCart(userId,isChecked,skuId);
        return Result.ok();
    }

    //删除购物车的方法
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.deleteCart(skuId,userId);
        return Result.ok();
    }

    //根据用户id 查询送货清单数据
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }
    @GetMapping("loadCartCache/{userId}")
    public Result loadCartCache(@PathVariable String userId){
        cartService.loadCartCache(userId);
        return Result.ok();
    }



}
