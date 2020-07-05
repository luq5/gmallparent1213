package com.atguigu.gmall1213.list.service;

import com.atguigu.gmall1213.model.list.SearchParam;
import com.atguigu.gmall1213.model.list.SearchResponseVo;

public interface SearchService {

    //商品上架
    void upperGoods(Long skuId);
    void upperGoods();

    //商品下架
    void lowerGoods(Long skuId);

    //热值
    void incrHotScore(Long skuId);

    //检索数据
    SearchResponseVo search(SearchParam searchParam) throws Exception;
}
