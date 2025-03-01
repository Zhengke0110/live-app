package fun.timu.live.api.controller;

import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.interfaces.IUserRpc;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @DubboReference
    private IUserRpc userRpc;

    @GetMapping("/getUserInfo")
    public UserDTO getUserInfo(Long userId) {
        return userRpc.getByUserId(userId);
    }

    @PostMapping("/updateUserInfo")
    public boolean updateUserInfo(Long userId, String nikename) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName(nikename);
        return userRpc.updateUserInfo(userDTO);
    }

    @GetMapping("/insertOne")
    public boolean insertOne(Long userId) {
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(userId);
        userDTO.setNickName("test");
        userDTO.setSex(1);
        return userRpc.insertOne(userDTO);
    }
}
