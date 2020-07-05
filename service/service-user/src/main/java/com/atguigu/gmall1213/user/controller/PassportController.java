package com.atguigu.gmall1213.user.controller;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall1213.common.constant.RedisConst;
import com.atguigu.gmall1213.common.result.Result;
import com.atguigu.gmall1213.common.util.IpUtil;
import com.atguigu.gmall1213.model.user.UserInfo;
import com.atguigu.gmall1213.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user/passport")
public class PassportController {
    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo, HttpServletRequest request){
        // login.login(this.user)
        UserInfo info = userService.login(userInfo);
        // 判断查询出来的数据是否为空！
        if (null!=info){
            // 登录成功之后，返回一个token ，token由 一个UUID 组成
            String token = UUID.randomUUID().toString();
            // 页面中 auth.setToken(response.data.data.token) 将token 放入cookie 中!
            // 声明一个map
            HashMap<String, Object> map = new HashMap<>();
            map.put("token",token);
            // 还需要做一件事 ：登录成功之后，页面上方需要显示一个用户昵称的！
            map.put("nickName",info.getNickName());

            // 如果登录成功，我们需要将用户信息存储缓存！ 只需要通过一个 userId 就可以了！
            // 将此时登录的用户IP 地址放入缓存！
            // 声明一个对象
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userId",info.getId().toString());
            // 工具类
            jsonObject.put("ip", IpUtil.getIpAddress(request));

            // 将数据放入缓存
            // 定义key
            String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            redisTemplate.opsForValue().set(userKey,jsonObject.toJSONString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);
            // 将map 返回
            return Result.ok(map);
        }else {
            // 如果没用户信息
            return Result.fail().message("用户名密码不匹配！");
        }
    }

    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        // 删除缓存中的数据 userKey = user:login:
        // token 跟用户缓存key 有直接的关系，在登录的时候，将token 放入了cookie！
        // 但是，在登录的时候，token 不止放入了cookie中，还放入了其他的位置！
        // login.login(this.user).then(response => {  }
        /*
        login(userInfo) {
            return request({ })
          }
          又套了一层request request.interceptors.request.use(function(config){ 定义了一个拦截器
          这个拦截器做了一件让人不太理解的事情！ 将token 放入了 header 中！
          因为：考虑到这个登录可以扩展为移动端使用！

        总结：
            登录时，将token 放入了cookie 中，同时放入了header 中！
         */
        String token = request.getHeader("token");
        String userKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
        redisTemplate.delete(userKey);

        return Result.ok();
    }

}
