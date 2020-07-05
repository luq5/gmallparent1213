package com.atguigu.gmall1213.model.list;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
public class SearchAttr {
    // 平台属性Id
    @Field(type = FieldType.Long)
    private Long attrId;
    // 平台属性值
    @Field(type = FieldType.Keyword)
    private String attrValue;
    // 平台属性名
    @Field(type = FieldType.Keyword)
    private String attrName;


}
