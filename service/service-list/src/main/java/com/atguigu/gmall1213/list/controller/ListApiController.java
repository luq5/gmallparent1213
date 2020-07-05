package com.atguigu.gmall1213.list.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.list.service.SearchService;
import com.atguigu.gmall1213.model.list.Goods;
import com.atguigu.gmall1213.model.list.SearchParam;
import com.atguigu.gmall1213.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/list")
public class ListApiController {


    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    @GetMapping("inner/createIndex")
    public Result createIndex(){        //创建index,type
        restTemplate.createIndex(Goods.class);
        restTemplate.putMapping(Goods.class);
        return  Result.ok();
    }
    //上架
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }
    //下架
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }
    //热度排名
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }
    @PostMapping
    public Result getList(@RequestBody SearchParam searchParam){
        SearchResponseVo search= null;
        try {
            search = searchService.search(searchParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.ok(search);
    }

}
