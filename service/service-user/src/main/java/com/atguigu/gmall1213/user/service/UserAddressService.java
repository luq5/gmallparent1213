package com.atguigu.gmall1213.user.service;

import com.atguigu.gmall1213.model.user.UserAddress;

import java.util.List;

public interface UserAddressService {

    //业务接口
    List<UserAddress> findUserAddressListByUserId(String userId);

}
