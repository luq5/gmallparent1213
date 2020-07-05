package com.atguigu.gmall1213.user.client;

import com.atguigu.gmall1213.model.user.UserAddress;
import com.atguigu.gmall1213.user.client.impl.UserDegradeFeignClient;
import net.bytebuddy.description.NamedElement;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "service-user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {

    @GetMapping("api/user/inner/findUserAddressListByUserId/{userId}")
    List<UserAddress> findUserAddressListByUserId(@PathVariable String userId);

}
