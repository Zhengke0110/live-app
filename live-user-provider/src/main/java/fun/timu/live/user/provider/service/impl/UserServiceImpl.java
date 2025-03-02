package fun.timu.live.user.provider.service.impl;

import fun.timu.live.common.interfaces.utils.ConvertBeanUtils;
import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.provider.dao.mapper.IUserMapper;
import fun.timu.live.user.provider.dao.po.UserPO;
import fun.timu.live.user.provider.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserMapper userMapper;

    @Resource
    private RedisTemplate<String, UserDTO> redisTemplate;

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
        String key = "UserInfo:" + userId;
        UserDTO userDTO = redisTemplate.opsForValue().get(key);
        if (userDTO != null) return userDTO;

        userDTO = ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);

        if (userDTO != null) redisTemplate.opsForValue().set(key, userDTO);
        return userDTO;
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
