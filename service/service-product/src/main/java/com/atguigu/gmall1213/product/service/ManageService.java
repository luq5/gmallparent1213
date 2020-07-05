package com.atguigu.gmall1213.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ManageService {
    //查询一级分类的数据
    List<BaseCategory1> getCategory1();

    //根据一级分类ID 查询二级分类数据
    List<BaseCategory2> getCategory2(long category1Id);


    //根据二级分类ID 查询三级分类数据
    List<BaseCategory3> getCategory3(long category2Id);

    //根据分类ID 查询平台属性数据
    List<BaseAttrInfo> getAttrInfoList(long category1Id,long category2Id,long category3Id);

    //保存平台属性和平台属性值
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    BaseAttrInfo getAttInfo(Long attrId);

    //分页查询
    IPage<SpuInfo> selectPage (Page<SpuInfo> spuInfoPageParam , SpuInfo spuInfo);

    //获取所有的销售属性数据
    List<BaseSaleAttr> getBaseSaleAttrList();
    //保存spuinfo(商品表)数据
    void saveSpuInfo(SpuInfo spuInfo);
    //查询返回图片
    List<SpuImage> getSpuImageList(Long spuId);
    //查询销售属性
    List<SpuSaleAttr> spuSaleAttrList(Long spuId);
    //保存sku数据
    void saveSkuInfo(SkuInfo skuInfo);

    //查询skuInfo列表
    IPage<SkuInfo> selectPage(Page<SkuInfo> skuInfoPage);
     //上架
    void onSale(Long skuId);
    //下架
    void cancelSale(Long skuId);
    //根据sku查询数据
    SkuInfo getSkuInfo(Long skuId);

    //根据三级分类ID获取分类名称
    BaseCategoryView getBaseCategoryViewBycategory3Id(Long category3Id);

    //通过skuid查询价格
    BigDecimal getSkuPriceBySkuId(Long skuId);

    //查询销售属性数据
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId,long spuId);

    //
    Map getSkuValueIdsMap(Long spuId);

    //获取所有的分类数据
    List<JSONObject> getBaseCategoryList();
    //根据品牌id查询品牌
    BaseTrademark getTrademarkByTmId(Long tmId);

    //根据skuid查询平台属性值
    List<BaseAttrInfo> getAttrInfoList(Long skuId);
}
