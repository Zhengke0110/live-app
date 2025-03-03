package fun.timu.live.user.provider.service.impl;

import com.google.common.collect.Maps;
import fun.timu.live.common.interfaces.utils.ConvertBeanUtils;
import fun.timu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.provider.dao.mapper.IUserMapper;
import fun.timu.live.user.provider.dao.po.UserPO;
import fun.timu.live.user.provider.service.IUserService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements IUserService {

    private final IUserMapper userMapper;

    @Resource
    private RedisTemplate<String, UserDTO> redisTemplate;

    @Resource
    private UserProviderCacheKeyBuilder cacheKeyBuilder;

    @Autowired
    public UserServiceImpl(IUserMapper userMapper) {
        this.userMapper = userMapper;
    }


    /**
     * 根据用户ID获取用户信息
     * 首先尝试从Redis缓存中获取用户信息，如果未命中，则从数据库中查询
     * 并将结果缓存到Redis中，以提高后续相同查询的性能
     *
     * @param userId 用户ID，用于查询用户信息
     * @return UserDTO 如果找到用户信息，则返回UserDTO对象，否则返回null
     */
    @Override
    public UserDTO getByUserId(Long userId) {
        // 参数校验，如果userId为null，则直接返回null
        if (userId == null) {
            return null;
        }

        // 构建缓存键
        String key = cacheKeyBuilder.buildUserInfoKey(userId);

        // 尝试从Redis缓存中获取用户信息
        UserDTO userDTO = redisTemplate.opsForValue().get(key);

        // 如果从缓存中获取到用户信息，则直接返回
        if (userDTO != null) {
            return userDTO;
        }

        // 如果缓存中未命中，则从数据库中查询用户信息
        userDTO = ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);

        // 如果查询到用户信息，则将其缓存到Redis中，并设置过期时间
        if (userDTO != null) {
            redisTemplate.opsForValue().set(key, userDTO, 30, TimeUnit.MINUTES);
        }

        // 返回查询到的用户信息
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

    /**
     * 批量查询用户信息
     *
     * @param userIdList 用户ID列表
     * @return 返回一个映射，键为用户ID，值为用户信息DTO
     */
    @Override
    public Map<Long, UserDTO> batchQueryUserInfo(List<Long> userIdList) {
        // 判断用户ID列表是否为空，如果为空则返回空映射
        if (CollectionUtils.isEmpty(userIdList)) {
            return Maps.newHashMap();
        }

        // 过滤用户ID列表，仅保留ID大于10000的用户
        userIdList = userIdList.stream().filter(id -> id > 10000).collect(Collectors.toList());
        // 再次检查过滤后的用户ID列表是否为空，如果为空则返回空映射
        if (CollectionUtils.isEmpty(userIdList)) {
            return Maps.newHashMap();
        }

        // redis
        // 根据用户ID列表生成对应的Redis键列表
        List<String> keyList = new ArrayList<>();
        userIdList.forEach(userId -> {
            keyList.add(cacheKeyBuilder.buildUserInfoKey(userId));
        });

        // 从Redis中批量获取用户信息
        List<UserDTO> userDTOList = redisTemplate.opsForValue().multiGet(keyList).stream().filter(x -> x != null).collect(Collectors.toList());
        // 如果从Redis中获取的用户信息完整且与用户ID列表大小一致，则直接返回用户信息映射
        if (!CollectionUtils.isEmpty(userDTOList) && userDTOList.size() == userIdList.size()) {
            return userDTOList.stream().collect(Collectors.toMap(UserDTO::getUserId, x -> x));
        }

        // 获取Redis中已缓存的用户ID列表
        List<Long> userIdInCacheList = userDTOList.stream().map(UserDTO::getUserId).collect(Collectors.toList());
        // 计算需要从数据库查询的用户ID列表
        List<Long> userIdNotInCacheList = userIdList.stream().filter(x -> !userIdInCacheList.contains(x)).collect(Collectors.toList());

        // 根据用户ID的哈希值对用户ID进行分组，以实现分片查询
        Map<Long, List<Long>> userIdMap = userIdNotInCacheList.stream().collect(Collectors.groupingBy(userId -> userId % 100));
        // 使用多线程并行查询数据库中未缓存的用户信息
        List<UserDTO> dbQueryResult = new CopyOnWriteArrayList<>();
        userIdMap.values().parallelStream().forEach(queryUserIdList -> {
            dbQueryResult.addAll(ConvertBeanUtils.convertList(userMapper.selectBatchIds(queryUserIdList), UserDTO.class));
        });

        // Redis 补偿
        // 如果从数据库查询到了用户信息，则更新Redis缓存，并设置过期时间
        if (!CollectionUtils.isEmpty(dbQueryResult)) {
            Map<String, UserDTO> saveCacheMap = dbQueryResult.stream().collect(Collectors.toMap(userDto -> cacheKeyBuilder.buildUserInfoKey(userDto.getUserId()), x -> x));
            redisTemplate.opsForValue().multiSet(saveCacheMap);

            // 对命令执行批量过期设置操作
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
                    for (String redisKey : saveCacheMap.keySet()) {
                        operations.expire((K) redisKey, createRandomTime(), TimeUnit.SECONDS);
                    }
                    return null;
                }
            });

            // 合并从Redis和数据库查询到的用户信息
            userDTOList.addAll(dbQueryResult);
        }

        // 返回最终的用户信息映射
        return userDTOList.stream().collect(Collectors.toMap(UserDTO::getUserId, x -> x));
    }

    private int createRandomTime() {
        int randomNumSecond = ThreadLocalRandom.current().nextInt(10000);
        return randomNumSecond + 30 * 60;
    }
}
