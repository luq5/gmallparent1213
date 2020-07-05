package com.atguigu.gmall1213.cart.service.impl;

import com.atguigu.gmall1213.cart.mapper.CartInfoMapper;
import com.atguigu.gmall1213.cart.service.CartAsyncService;
import com.atguigu.gmall1213.cart.service.CartService;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.model.cart.CartInfo;
import com.atguigu.gmall1213.model.product.SkuInfo;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private CartAsyncService cartAsyncService;

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public void addToCart(Long skuId, String userId, Integer skuNum) {
        String cartKey = getCartKey(userId);
        //添加购物车之前判断
        if(!redisTemplate.hasKey(userId)){
            loadCartCache(userId);
        }

        CartInfo cartInfoExist = cartInfoMapper.selectOne(new QueryWrapper<CartInfo>().eq("user_id", userId).eq("sku_id", skuId));
        if (null!=cartInfoExist){
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            cartInfoExist.setSkuPrice(productFeignClient.getSkuPrice(skuId));
            // 更新
            // cartInfoMapper.updateById(cartInfoExist);
            cartAsyncService.updateCartInfo(cartInfoExist);
            // 放入缓存
            // redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);
        }else {
            // 没有商品，第一次添加
            // 购物车中的数据，都是来自于商品详情，商品详情的数据是来自于servce-product.
            SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
            // 声明一个cartInfo 对象
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);

            // 新增数据
            // cartInfoMapper.insert(cartInfo);
            cartAsyncService.saveCartInfo(cartInfo);
            // 如果代码走到了这，说明cartInfoExist 是空。cartInfoExist 可能会被GC吃了。废物再利用
            cartInfoExist=cartInfo;
        }
        // 在缓存存储数据
        redisTemplate.boundHashOps(cartKey).put(skuId.toString(),cartInfoExist);

        // 要给缓存设置过期时间
        setCartKeyExpire(cartKey);
    }

    @Override
    public List<CartInfo> getCartList(String userId, String userTempId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 判断你是登录，还是未登录
        if (StringUtils.isEmpty(userId)){
            // 说明未登录
            cartInfoList = getCartList(userTempId);
        }
        if (!StringUtils.isEmpty(userId)){
            List<CartInfo> cartInfoNoLoginList = getCartList(userTempId);
            if (!CollectionUtils.isEmpty(cartInfoNoLoginList)){
                //开始合并购物车
              cartInfoList=  mergeToCartList(cartInfoNoLoginList,userId);
              //合并之后删除
                deleteCartList(userTempId);
            }
            //如果未登录的购物车为空，那么就直接返回登录购物车集合
            if (CollectionUtils.isEmpty(cartInfoNoLoginList) || StringUtils.isEmpty(userTempId)){
                cartInfoList = getCartList(userId);
            }
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, Integer isChecked, Long skuId) {
        //调用异步
        cartAsyncService.checkCart(userId,isChecked,skuId);
        //更新缓存
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if(boundHashOperations.hasKey(skuId.toString())){
            CartInfo cartInfo = (CartInfo) boundHashOperations.get(skuId.toString());
            //对应的状态
            boundHashOperations.put(skuId.toString(),cartInfo);

            //修改过期时间
            setCartKeyExpire(cartKey);
        }

    }

    @Override
    public void deleteCart(Long skuId, String userId) {
        //数据库删除
        cartAsyncService.deleteCartInfo(userId,skuId);

        //删除缓存
        String cartKey = getCartKey(userId);
        BoundHashOperations boundHashOperations = redisTemplate.boundHashOps(cartKey);
        if (boundHashOperations.hasKey(skuId.toString())){
            boundHashOperations.delete(skuId.toString());
        }

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfos = new ArrayList<>();
        
        String cartKey = getCartKey(userId);
        
        List<CartInfo> cartInfoList = redisTemplate.opsForHash().values(cartKey);
        
        if (!CollectionUtils.isEmpty(cartInfoList)){
            for (CartInfo cartInfo : cartInfoList) {
                if (cartInfo.getIsChecked().intValue()==1){
                    cartInfos.add(cartInfo);
                }
            }
        }
        return cartInfos;
    }

    //删除购物车数据
    private void deleteCartList(String userTempId) {
        //删除缓存 ，一个是删除数据库
        //cartInfoMapper.delete(new QueryWrapper<CartInfo>().eq("user_id",userTempId));
        cartAsyncService.deleteCartInfo(userTempId);

        String cartKey = getCartKey(userTempId);
        Boolean flag = redisTemplate.hasKey(cartKey);
        if (flag){
            redisTemplate.delete(cartKey);
        }

    }
    //合并购物车方法
    private List<CartInfo> mergeToCartList(List<CartInfo> cartInfoNoLoginList, String userId) {
        List<CartInfo> cartListLogin = getCartList(userId);
        Map<Long, CartInfo> cartInfoMap = cartListLogin.stream().collect(Collectors.toMap(CartInfo::getSkuId, cartInfo -> cartInfo));
        //判断
        for (CartInfo cartInfoNoLogin : cartInfoNoLoginList) {
            //获取到未登录的skuid
            Long skuId = cartInfoNoLogin.getSkuId();
            if (cartInfoMap.containsKey(skuId)){
                CartInfo cartInfoLogin = cartInfoMap.get(skuId);
                cartInfoLogin.setSkuNum(cartInfoLogin.getSkuNum()+cartInfoNoLogin.getSkuNum());
                //添加未登录的选中商品
                if (cartInfoLogin.getIsChecked().intValue()==1){
                        cartInfoLogin.setIsChecked(1);
                }
                //更新数据库
                cartAsyncService.updateCartInfo(cartInfoLogin);
            }else {
                cartInfoNoLogin.setUserId(userId);
                //cartInfoMapper.insert(cartInfoNoLogin);
                cartAsyncService.saveCartInfo(cartInfoNoLogin);
            }
        }
        //最终合并结果
        List<CartInfo> cartInfoList = loadCartCache(userId);
        return cartInfoList;
    }

    // 获取购物车列表
    private List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 先看缓存，再看数据库并将数据放入缓存。
        if (StringUtils.isEmpty(userId)) {
            return null;
        }
        String cartKey = getCartKey(userId);
        // 获取缓存中的数据
        cartInfoList = redisTemplate.opsForHash().values(cartKey);
        // 判断集合中的数据是否存在
        if (!CollectionUtils.isEmpty(cartInfoList)){
            // cartInfoList 说明缓存中有数据 数据展示的时候，应该是有规则排序的，那么这个规则应该是什么？ 更新时间
            // 按照id进行排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                // Comparator 比较器 - 自定义 内名内部类
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });

            return cartInfoList;
        }else {
            // 说明缓存中没用数据
            cartInfoList= loadCartCache(userId);
            // 返回数据
            return cartInfoList;
        }
    }

    // 根据用户Id 查询数据库并将数据放入缓存。
    public List<CartInfo> loadCartCache(String userId) {
        // select * from cartInfo where user_id = userId;
        // 数据库中的数据
        List<CartInfo> cartInfoList = cartInfoMapper.selectList(new QueryWrapper<CartInfo>().eq("user_id", userId));
        // 数据库中一定会有这样的数据么？
        if (CollectionUtils.isEmpty(cartInfoList)){
            return cartInfoList;
        }
        // 如果不为空，那么将数据放入缓存
        // 声明一个map 集合来存储数据
        HashMap<String, CartInfo> map = new HashMap<>();
        // 获取到缓存key
        String cartKey = getCartKey(userId);
        // 循环遍历集合将map 中填入数据
        for (CartInfo cartInfo : cartInfoList) {
            // 之所以查询数据库是因为缓存中没用数据！是不是有可能会发生价格变动。
            cartInfo.setSkuPrice(productFeignClient.getSkuPrice(cartInfo.getSkuId()));
            // map.put(field,value);
            map.put(cartInfo.getSkuId().toString(),cartInfo);
        }
        //  redisTemplate.opsForHash().putAll(key,map); map.put(field,value);
        redisTemplate.opsForHash().putAll(cartKey,map);
        // 设置过期时间
        setCartKeyExpire(cartKey);
        return cartInfoList;
    }

    // 设置过期时间 ctrl+alt+m
    private void setCartKeyExpire(String cartKey) {
        redisTemplate.expire(cartKey, RedisConst.USER_CART_EXPIRE, TimeUnit.SECONDS);
    }
    // 获取用户购物车key
    private String getCartKey(String userId){
        return  RedisConst.USER_KEY_PREFIX+userId+RedisConst.USER_CART_KEY_SUFFIX;
    }
}
