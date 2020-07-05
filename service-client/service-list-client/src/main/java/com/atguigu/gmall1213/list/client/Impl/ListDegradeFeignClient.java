package com.atguigu.gmall1213.list.client.Impl;


import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.list.client.ListFeignClient;
import com.atguigu.gmall1213.model.list.SearchParam;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

@Component
public class ListDegradeFeignClient implements ListFeignClient {

    @Override
    public Result incrHotScore(Long skuId) {
        return null;
    }

    @Override
    public Result list(SearchParam listParam) {
        return null;
    }

    @Override
    public Result upperGoods(Long skuId) {
        return null;
    }

    @Override
    public Result lowerGoods(Long skuId) {
        return null;
    }
}
