package com.atguigu.gmall1213.list.repository;

import com.atguigu.gmall1213.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {

}
