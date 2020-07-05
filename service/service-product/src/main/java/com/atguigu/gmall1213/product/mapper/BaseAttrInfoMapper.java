package com.atguigu.gmall1213.product.mapper;

import com.atguigu.gmall1213.model.product.BaseAttrInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BaseAttrInfoMapper extends BaseMapper<BaseAttrInfo> {

    List<BaseAttrInfo> selectBaseAttrInfoList(@Param("category1Id")long category1Id,
                                              @Param("category2Id") long category2Id,
                                              @Param("category3Id") long category3Id);

    List<BaseAttrInfo> selectAttrInfoList(Long skuId);
}
