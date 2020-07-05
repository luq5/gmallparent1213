package com.atguigu.gmall1213.cart.service.impl;

import com.atguigu.gmall1213.cart.mapper.CartInfoMapper;
import com.atguigu.gmall1213.cart.service.CartAsyncService;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncServiceImpl implements CartAsyncService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Override
    @Async//异步注解
    public void updateCartInfo(CartInfo cartInfo) {
        cartInfoMapper.updateById(cartInfo);
    }

    @Override
    @Async
    public void saveCartInfo(CartInfo cartInfo) {
        cartInfoMapper.insert(cartInfo);
    }

    @Override
    @Async
    public void deleteCartInfo(String userId) {
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userId));

    }

    @Override
    @Async
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        CartInfo cartInfo = new CartInfo();
        cartInfo.setIsChecked(isChecked);
        QueryWrapper<CartInfo> cartInfoQueryWrapper = new QueryWrapper<>();
        cartInfoQueryWrapper.eq("user_id",userId).eq("sku_id",skuId);
        cartInfoMapper.update(cartInfo,cartInfoQueryWrapper);

    }

    @Override
    @Async
    public void deleteCartInfo(String userId, Long skuId) {
        cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userId).eq("sku_id",skuId));


    }
}
