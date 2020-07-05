package com.atguigu.gmall1213.list.service.Impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall1213.list.repository.GoodsRepository;
import com.atguigu.gmall1213.list.service.SearchService;
import com.atguigu.gmall1213.model.list.*;
import com.atguigu.gmall1213.model.product.*;
import com.atguigu.gmall1213.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void upperGoods(Long skuId) {
        Goods goods = new Goods();
        //获取商品基本信息
        SkuInfo skuInfo = productFeignClient.getSkuInfoById(skuId);
        if (null != skuInfo) {
            goods.setId(skuId);
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setTitle(skuInfo.getSkuName());
            goods.setCreateTime(new Date());
//商品分类信息
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            if (null != categoryView) {
                goods.setCategory1Id(categoryView.getCategory1Id());
                goods.setCategory1Name(categoryView.getCategory1Name());
                goods.setCategory2Id(categoryView.getCategory2Id());
                goods.setCategory2Name(categoryView.getCategory2Name());
                goods.setCategory3Id(categoryView.getCategory3Id());
                goods.setCategory3Name(categoryView.getCategory3Name());
            }

            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuInfo.getId());
            List<SearchAttr> searchAttrList = attrList.stream().map(baseAttrInfo -> {
                // 通过baseAttrInfo 获取平台属性Id
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                // 赋值平台属性值名称
                // 获取了平台属性值的集合
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                searchAttr.setAttrValue(attrValueList.get(0).getValueName());

                // 将每个平台属性对象searchAttr 返回去
                return searchAttr;
            }).collect(Collectors.toList());
            // 存储平台属性
            if (null != searchAttrList) {
                goods.setAttrs(searchAttrList);
            }
            // 品牌信息
            BaseTrademark trademark = productFeignClient.getTrademarkByTmId(skuInfo.getTmId());
            if (null != trademark) {
                goods.setTmId(trademark.getId());
                goods.setTmName(trademark.getTmName());
                goods.setTmLogoUrl(trademark.getLogoUrl());
            }
        }
        goodsRepository.save(goods);
    }

    @Override
    public void upperGoods() {
        // 读一个一个excel 表格 ,所有要上传的skuId.
    }

    @Override
    public void lowerGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
        // 需要借助redis
        String key = "hotScore";
        // 用户每访问一次，那么这个数据应该+1,成员以商品Id 为单位
        Double hotScore = redisTemplate.opsForZSet().incrementScore(key, "skuId:" + skuId, 1);
        // 按照规定来更新es 中的数据 30
        if (hotScore % 10 == 0) {
            // 更新一次es 中hotScore
            // 获取到es 中的对象
            Optional<Goods> optional = goodsRepository.findById(skuId);
            // 获取到了当前对象
            Goods goods = optional.get();
            goods.setHotScore(Math.round(hotScore));
            goodsRepository.save(goods);
        }
    }


    @Override
    public SearchResponseVo search(SearchParam searchParam) throws Exception {
        //dsl语句：利用JAVA代码来实现动态dsl语句
        SearchRequest searchRequest =buildQueryDsl(searchParam);
        //执行dsl语句
        SearchResponse searchResponse= restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //将查询之后的数据集
        SearchResponseVo responseVo = parseSearchResult(searchResponse);
        responseVo.setPageSize(searchParam.getPageSize());
        responseVo.setPageNo(searchParam.getPageNo());
        //根据总条数显示多少来计算   计算总页数
        long totalPages =(responseVo.getTotal()+searchParam.getPageSize()-1)/searchParam.getPageSize();
        responseVo.setTotalPages(totalPages);
        return responseVo;
    }

    //获取返回结果集
    private SearchResponseVo parseSearchResult(SearchResponse searchResponse) {
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        Map<String, Aggregation> aggregationMap = searchResponse.getAggregations().asMap();
        // 获取到了品牌Id 的Agg 获取到桶信息，Aggregation 对象中并没有此方法 ParsedLongTerms
        ParsedLongTerms tmIdAgg = (ParsedLongTerms) aggregationMap.get("tmIdAgg");
        // Aggregation; ctrl + h
        // Function R apply(T t)
        List<SearchResponseTmVo> responseTmVoList = tmIdAgg.getBuckets().stream().map(bucket -> {
            // 声明一个品牌对象
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            // 获取到品牌Id
            String tmId = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.parseLong(tmId));

            // 赋值品牌的名称
            Map<String, Aggregation> tmIdAggregationMap = ((Terms.Bucket) bucket).getAggregations().asMap();
            // Aggregation -- ParsedStringTerms
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            // 赋值品牌的logoUrl
            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) tmIdAggregationMap.get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);
            // 返回品牌对象
            return searchResponseTmVo;
        }).collect(Collectors.toList());
        // 赋值品牌整个集合数据
        searchResponseVo.setTrademarkList(responseTmVoList);

        // 赋值商品 goodsList
        SearchHits hits = searchResponse.getHits();
        SearchHit[] subHits = hits.getHits();
        // 声明一个商品对象集合
        List<Goods> goodsList = new ArrayList<>();
        if (null!=subHits && subHits.length>0){
            // 循环遍历集合
            for (SearchHit subHit : subHits) {
                // json 字符串
                String sourceAsString = subHit.getSourceAsString();
                // 将json 字符串转化为 goods
                Goods goods = JSON.parseObject(sourceAsString, Goods.class);
                if (null!=subHit.getHighlightFields().get("title")){
                    Text title= subHit.getHighlightFields().get("title").getFragments()[0];
                    goods.setTitle(title.toString());
                }
                // 将对象添加到集合
                goodsList.add(goods);
            }
        }
        searchResponseVo.setGoodsList(goodsList);

        // 平台属性
        ParsedNested attrAgg = (ParsedNested) aggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        // 判断集合中是否有数据
        if (!CollectionUtils.isEmpty(buckets)){
            List<SearchResponseAttrVo> responseAttrVoList = buckets.stream().map(bucket -> {
                // 声明一个对象
                SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
                // 赋值属性Id
                searchResponseAttrVo.setAttrId(((Terms.Bucket) bucket).getKeyAsNumber().longValue());
                // 赋值属性名称
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
                String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
                searchResponseAttrVo.setAttrName(attrName);

                // 赋值属性值名称
                ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
                // 属性值可能有多个，循环遍历
                List<? extends Terms.Bucket> valueAggBucketsList = attrValueAgg.getBuckets();
                // 获取到集合中的每个数据
                // Terms.Bucket::getKeyAsString 通过key 来获取value
                List<String> valueList = valueAggBucketsList.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                // 将获取到的属性值放入集合中
                searchResponseAttrVo.setAttrValueList(valueList);

                return searchResponseAttrVo;
            }).collect(Collectors.toList());
            // 将属性，属性值数据放入返回对象
            searchResponseVo.setAttrsList(responseAttrVoList);
        }
        // 赋值总条数
        searchResponseVo.setTotal(hits.totalHits);
        // 返回对象
        return searchResponseVo;
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam) {
        //定义查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //QueryBuilders
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            MatchQueryBuilder title = QueryBuilders.matchQuery("title", searchParam.getKeyword()).operator(Operator.AND);
            boolQueryBuilder.must(title);
        }
        //根据ID分类查询
        if (null!= searchParam.getCategory1Id()){
            TermQueryBuilder category1Id = QueryBuilders.termQuery("category1Id", searchParam.getCategory1Id());
            boolQueryBuilder.filter(category1Id);
        }
        if (null!= searchParam.getCategory2Id()){
            TermQueryBuilder category2Id = QueryBuilders.termQuery("category2Id", searchParam.getCategory2Id());
            boolQueryBuilder.filter(category2Id);
        }
        if (null!= searchParam.getCategory3Id()){
            TermQueryBuilder category3Id = QueryBuilders.termQuery("category3Id", searchParam.getCategory3Id());
            boolQueryBuilder.filter(category3Id);
        }
        //查询品牌
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)){
            String[] split = trademark.split(":");
            if (null!=split && split.length==2){
                TermQueryBuilder tmId = QueryBuilders.termQuery("tmId",split[0]);
                boolQueryBuilder.filter(tmId);
            }
        }
        //根据平台属性值进行查询
        String[] props = searchParam.getProps();
        if (null!=props && props.length>0){
            for (String prop : props) {
                String[] split = prop.split(":");
                if (null!=split && split.length==3){
                    BoolQueryBuilder boolQuery =  QueryBuilders.boolQuery();
                    BoolQueryBuilder subBoolQuery =  QueryBuilders.boolQuery();
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue", split[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));

                    //整合查询
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);
        //分页数据
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());

        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.postTags("</span>");
        highlightBuilder.preTags("<span style=color:red>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //做排序
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)){
            String[] split = order.split(":");
            if (null!=split && split.length==2){
                String field = null;
                switch (split[0]){
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                searchSourceBuilder.sort(field,"asc".equals(split[1])?SortOrder.ASC: SortOrder.DESC);
            }else {
                searchSourceBuilder.sort("hotScore",SortOrder.DESC);
            }
        }
        TermsAggregationBuilder termsAggregationBuilder = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));

        //将品牌的agg 放入查询器
        searchSourceBuilder.aggregation(termsAggregationBuilder);
        //设置平台属性聚合
        searchSourceBuilder.aggregation(AggregationBuilders.nested("attrAgg","attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))));

        searchSourceBuilder.fetchSource(new String[]{"id","defaultImg","title","price"},null);
        SearchRequest searchRequest = new SearchRequest("goods");
        searchRequest.types("info");
        searchRequest.source(searchSourceBuilder);
        System.out.println("dsl:"+searchSourceBuilder.toString());
        return  searchRequest;
    }
}
