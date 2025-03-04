package fun.timu.live.user.provider.service.impl;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import fun.timu.live.common.interfaces.topic.UserProviderTopicNames;
import fun.timu.live.common.interfaces.utils.ConvertBeanUtils;
import fun.timu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import fun.timu.live.user.constants.CacheAsyncDeleteCode;
import fun.timu.live.user.dto.UserCacheAsyncDeleteDTO;
import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.provider.dao.mapper.IUserMapper;
import fun.timu.live.user.provider.dao.po.UserPO;
import fun.timu.live.user.provider.service.IUserService;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
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

    private final MQProducer mqProducer;

    @Autowired
    public UserServiceImpl(IUserMapper userMapper, MQProducer mqProducer) {
        this.userMapper = userMapper;
        this.mqProducer = mqProducer;
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
        if (userId == null) {
            return null;
        }
        String key = cacheKeyBuilder.buildUserInfoKey(userId);
        UserDTO userDTO = redisTemplate.opsForValue().get(key);
        if (userDTO != null) {
            return userDTO;
        }
        userDTO = ConvertBeanUtils.convert(userMapper.selectById(userId), UserDTO.class);
        if (userDTO != null) {
            redisTemplate.opsForValue().set(key, userDTO, 30, TimeUnit.MINUTES);
        }
        return userDTO;
    }

    /**
     * 更新用户信息的方法
     *
     * @param userDTO 用户数据传输对象，包含要更新的用户信息
     * @return 如果更新失败或参数无效，则返回false；否则返回true
     */
    @Override
    public boolean updateUserInfo(UserDTO userDTO) {
        // 检查传入的用户对象是否为空或用户ID是否为空，如果任一条件满足，则返回false
        if (userDTO == null || userDTO.getUserId() == null) {
            return false;
        }
        // 将用户DTO转换为用户PO并更新数据库中的用户信息
        int updateStatus = userMapper.updateById(ConvertBeanUtils.convert(userDTO, UserPO.class));
        // 如果数据库更新操作成功（更新状态大于-1），则进行缓存删除操作
        if (updateStatus > -1) {
            // 构建缓存键并删除Redis中的用户信息缓存
            String key = cacheKeyBuilder.buildUserInfoKey(userDTO.getUserId());
            redisTemplate.delete(key);
            // 准备异步删除缓存的消息体
            UserCacheAsyncDeleteDTO userCacheAsyncDeleteDTO = new UserCacheAsyncDeleteDTO();
            userCacheAsyncDeleteDTO.setCode(CacheAsyncDeleteCode.USER_INFO_DELETE.getCode());
            Map<String, Object> jsonParam = new HashMap<>();
            jsonParam.put("userId", userDTO.getUserId());
            userCacheAsyncDeleteDTO.setJson(JSON.toJSONString(jsonParam));
            // 创建消息对象，用于发送异步缓存删除指令
            Message message = new Message();
            message.setTopic(UserProviderTopicNames.CACHE_ASYNC_DELETE_TOPIC);
            message.setBody(JSON.toJSONString(userCacheAsyncDeleteDTO).getBytes());
            // 设置消息延迟级别，以实现缓存的二次删除
            message.setDelayTimeLevel(1);
            // 发送消息到消息队列，如果发送失败，则抛出运行时异常
            try {
                mqProducer.send(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        // 返回true表示用户信息更新操作执行成功
        return true;
    }

    /**
     * 插入单个用户记录
     * 此方法用于将用户信息插入数据库，确保用户ID不为空
     *
     * @param userDTO 用户数据传输对象，包含用户信息
     * @return 插入操作的成功与否，成功返回true，失败返回false
     */
    @Override
    public boolean insertOne(UserDTO userDTO) {
        // 检查用户对象和用户ID是否为空，为空则返回false
        if (userDTO == null || userDTO.getUserId() == null) return false;

        // 转换用户DTO为用户PO并调用Mapper方法插入数据库
        userMapper.insert(ConvertBeanUtils.convert(userDTO, UserPO.class));

        // 插入成功后返回true
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
