package fun.timu.live.api.controller;

import fun.timu.live.user.interfaces.IUserRpc;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {
    @DubboReference
    private IUserRpc userRpc;

    @GetMapping("/dubbo")
    public String dubboTest() {
        return userRpc.test();
    }
}
