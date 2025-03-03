package fun.timu.live.user.provider.rpc;

import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.interfaces.IUserRpc;
import fun.timu.live.user.provider.service.IUserService;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.Map;

@DubboService
public class UserRpcImpl implements IUserRpc {

    private final IUserService userService;

    public UserRpcImpl(IUserService userService) {
        this.userService = userService;
    }


    /**
     * 根据用户ID获取用户信息
     * <p>
     * 此方法通过调用userService的getByUserId方法来获取用户信息
     * 主要作用是封装对用户服务层的调用，以便在业务逻辑中更方便地获取用户信息
     *
     * @param userId 用户ID，用于指定需要获取信息的用户
     * @return UserDTO 用户数据传输对象，包含用户的相关信息
     */
    @Override
    public UserDTO getByUserId(Long userId) {
        return userService.getByUserId(userId);
    }

    @Override
    public boolean updateUserInfo(UserDTO userDTO) {
        return userService.updateUserInfo(userDTO);
    }

    @Override
    public boolean insertOne(UserDTO userDTO) {
        return userService.insertOne(userDTO);
    }

    /**
     * 批量查询用户信息
     *
     * @param userIdList 用户ID列表，用于指定需要查询的用户
     * @return 返回一个映射，键为用户ID，值为用户信息DTO
     *
     * 此方法通过用户ID列表批量查询用户信息，以Map形式返回，便于高效查找和使用用户数据
     */
    @Override
    public Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList) {
        return userService.batchQueryUserInfo(userIdList);
    }
}
