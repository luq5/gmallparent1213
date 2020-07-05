package com.atguigu.gmall1213.user.service.impl;

import com.atguigu.gmall1213.model.user.UserInfo;
import com.atguigu.gmall1213.user.mapper.UserInfoMapper;
import com.atguigu.gmall1213.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;



    @Override
    public UserInfo login(UserInfo userInfo) {
        String newPwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("login_name",userInfo.getLoginName()).eq("passwd",newPwd);
        UserInfo info = userInfoMapper.selectOne(userInfoQueryWrapper);
        if (null!=info){
            return info;
        }
         return null;
    }
}
