package com.atguigu.gmall1213.user.controller;

import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.user.service.UserAddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/user")
public class UserController {

    @Autowired
    private UserAddressService userAddressService;

    @GetMapping("inner/findUserAddressListByUserId/{userId}")
    public List<UserAddress> findUserAddressListByUserId(@PathVariable String userId){
       return userAddressService.findUserAddressListByUserId(userId);
    }
}
