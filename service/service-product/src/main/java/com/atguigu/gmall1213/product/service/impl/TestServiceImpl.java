package com.atguigu.gmall1213.product.service.impl;

import com.atguigu.gmall1213.product.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TestServiceImpl implements TestService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public synchronized void testLock() {
     String num = redisTemplate.opsForValue().get("num");
     if (StringUtils.isEmpty(num)) {
         return;
     }
     int number = Integer.parseInt(num);

     redisTemplate.opsForValue().set("num",String.valueOf(++number));
    }
}
