package fun.timu.live.api.controller;

import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.interfaces.IUserRpc;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    @GetMapping("/batchQueryUserInfo")
    public Map<Long, UserDTO> batchQueryUserInfo(String userIdStr) {
        String[] idStr = userIdStr.split(",");
        List<Long> userIdList = new ArrayList<>();
        for (String userId : idStr) {
            userIdList.add(Long.valueOf(userId));
        }
        return userRpc.batchQueryUserInfo(userIdList);
    }
}
