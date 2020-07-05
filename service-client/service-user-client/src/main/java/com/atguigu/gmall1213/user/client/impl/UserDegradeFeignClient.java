package com.atguigu.gmall1213.user.client.impl;

import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.user.client.UserFeignClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserDegradeFeignClient implements UserFeignClient {

    @Override
    public List<UserAddress> findUserAddressListByUserId(String userId) {
        return null;
    }
}
