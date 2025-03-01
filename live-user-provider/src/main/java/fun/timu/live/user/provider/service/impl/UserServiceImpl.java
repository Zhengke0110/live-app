package fun.timu.live.user.provider.service.impl;

import fun.timu.live.common.interfaces.utils.ConvertBeanUtils;
import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.provider.dao.mapper.IUserMapper;
import fun.timu.live.user.provider.dao.po.UserPO;
import fun.timu.live.user.provider.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserMapper userMapper;

    @Autowired
    public UserServiceImpl(IUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param userId 用户ID，用于查询用户信息如果为null，则返回null，表示未找到用户
     * @return UserDTO 用户信息的DTO对象，如果找不到指定的用户，则返回null
     */
    @Override
    public UserDTO getByUserId(Long userId) {
        // 检查传入的用户ID是否为null
        if (userId == null) return null;
        // 使用MyBatis Plus的userMapper根据用户ID查询用户信息，并转换为UserDTO对象返回
        return ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);
    }

    @Override
    public boolean updateUserInfo(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUserId() == null) return false;
        userMapper.updateById(ConvertBeanUtils.convert(userDTO, UserPO.class));
        return true;
    }

    @Override
    public boolean insertOne(UserDTO userDTO) {
        if (userDTO == null || userDTO.getUserId() == null) return false;
        userMapper.insert(ConvertBeanUtils.convert(userDTO, UserPO.class));
        return true;
    }

    @Override
    public Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList) {
        return null;
    }
}
