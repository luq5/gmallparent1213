package com.atguigu.gmall1213.cart.service;

import com.atguigu.gmall1213.model.cart.CartInfo;

import java.util.List;

public interface CartService {
    //添加购物车接口
    void addToCart(Long skuId, String userId, Integer skuNum);

    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);

    //删除购物车
    void deleteCart(Long skuId, String userId);

    //根据用户ID 查询购物车列表
    List<CartInfo> getCartCheckedList(String userId);

    //根据userId 查询购物车列表
    List<CartInfo> loadCartCache(String userId);

}
