package fun.timu.live.user.provider.service.impl;

import com.alibaba.fastjson.JSON;
import fun.timu.live.common.interfaces.topic.UserProviderTopicNames;
import fun.timu.live.common.interfaces.utils.ConvertBeanUtils;
import fun.timu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import fun.timu.live.user.constants.CacheAsyncDeleteCode;
import fun.timu.live.user.constants.UserTagFieldNameConstants;
import fun.timu.live.user.constants.UserTagsEnum;
import fun.timu.live.user.dto.UserCacheAsyncDeleteDTO;
import fun.timu.live.user.dto.UserTagDTO;
import fun.timu.live.user.provider.dao.mapper.IUserTagMapper;
import fun.timu.live.user.provider.dao.po.UserTagPO;
import fun.timu.live.user.provider.service.IUserTagService;
import fun.timu.live.user.utils.TagInfoUtils;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.producer.MQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserTagServiceImpl implements IUserTagService {

    private final IUserTagMapper userTagMapper;
    @Resource
    private RedisTemplate<String, UserTagDTO> redisTemplate;
    @Resource
    private UserProviderCacheKeyBuilder cacheKeyBuilder;
    private final MQProducer mqProducer;

    public UserTagServiceImpl(IUserTagMapper userTagMapper, MQProducer mqProducer) {
        this.userTagMapper = userTagMapper;
        this.mqProducer = mqProducer;
    }

    /**
     * 设置用户标签
     *
     * @param userId       用户ID
     * @param userTagsEnum 用户标签枚举，包含标签字段名和标签值
     * @return 如果标签设置成功返回true，否则返回false
     */
    @Override
    public boolean setTag(Long userId, UserTagsEnum userTagsEnum) {
        // 尝试更新用户标签，如果成功则删除Redis中的缓存并返回true
        boolean updateStatus = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        if (updateStatus) {
            deleteUserTagDTOFromRedis(userId);
            return true;
        }

        // 使用Redis的SETNX命令尝试设置标签锁，以避免并发更新问题
        String setNxKey = cacheKeyBuilder.buildTagLockKey(userId);
        String setNxResult = redisTemplate.execute(new RedisCallback<String>() {
            @Override
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                RedisSerializer keySerializer = redisTemplate.getKeySerializer();
                RedisSerializer valueSerializer = redisTemplate.getValueSerializer();
                // 执行SETNX命令，如果键不存在则设置键并设置过期时间
                return (String) connection.execute("set", keySerializer.serialize(setNxKey), valueSerializer.serialize("-1"), "NX".getBytes(StandardCharsets.UTF_8), "EX".getBytes(StandardCharsets.UTF_8), "3".getBytes(StandardCharsets.UTF_8));
            }
        });

        // 如果SETNX命令返回"OK"，表示成功设置锁，继续后续操作
        if (!"OK".equals(setNxResult)) {
            return false;
        }

        // 检查用户标签是否已存在，如果存在则返回false
        UserTagPO userTagPO = userTagMapper.selectById(userId);
        if (userTagPO != null) {
            return false;
        }

        // 如果用户标签不存在，创建新的用户标签对象并插入数据库
        userTagPO = new UserTagPO();
        userTagPO.setUserId(userId);
        userTagMapper.insert(userTagPO);

        // 再次尝试更新用户标签
        updateStatus = userTagMapper.setTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;

        // 删除之前设置的标签锁
        redisTemplate.delete(setNxKey);

        // 返回标签更新状态
        return updateStatus;
    }

    /**
     * 用于取消用户身上的某个标签
     * 此方法首先尝试在数据库中取消用户的标签，如果取消成功，则进一步在Redis中删除相应的用户标签信息
     * 这是为了保持数据库和Redis中的数据一致性
     *
     * @param userId 用户ID，用于标识需要取消标签的用户
     * @param userTagsEnum 用户标签枚举，提供了标签的详细信息，包括标签名称和字段名
     * @return 如果标签成功取消，返回true；否则，返回false
     */
    @Override
    public boolean cancelTag(Long userId, UserTagsEnum userTagsEnum) {
        // 尝试在数据库中取消用户的标签，判断取消是否成功
        boolean cancelStatus = userTagMapper.cancelTag(userId, userTagsEnum.getFieldName(), userTagsEnum.getTag()) > 0;
        // 如果取消标签失败，直接返回false
        if (!cancelStatus) {
            return false;
        }
        // 如果取消标签成功，则进一步在Redis中删除相应的用户标签信息，以保持数据一致性
        deleteUserTagDTOFromRedis(userId);
        // 标签成功取消，并且Redis中的对应信息也已删除，返回true
        return true;
    }

    /**
     * 检查用户是否包含特定的标签
     *
     * @param userId 用户ID，用于标识特定用户
     * @param userTagsEnum 用户标签枚举，定义了要检查的标签类型
     * @return 如果用户包含指定标签，则返回true；否则返回false
     */
    @Override
    public boolean containTag(Long userId, UserTagsEnum userTagsEnum) {
        // 从Redis中根据用户ID查询用户标签信息
        UserTagDTO userTagDTO = this.queryByUserIdFromRedis(userId);
        // 如果查询结果为空，则返回false，表示不包含指定标签
        if (userTagDTO == null) {
            return false;
        }
        // 获取标签枚举对应的字段名，用于后续的条件判断
        String queryFieldName = userTagsEnum.getFieldName();
        // 需要根据标签枚举中的fieldName来识别需要匹配MySQL表中哪个字段的标签值
        // 根据不同的字段名，调用相应的标签信息处理方法
        if (UserTagFieldNameConstants.TAG_INFO_01.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagDTO.getTagInfo01(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAG_INFO_02.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagDTO.getTagInfo02(), userTagsEnum.getTag());
        } else if (UserTagFieldNameConstants.TAG_INFO_03.equals(queryFieldName)) {
            return TagInfoUtils.isContain(userTagDTO.getTagInfo03(), userTagsEnum.getTag());
        }
        // 如果字段名不匹配任何已知标签字段，则返回false
        return false;
    }


    /**
     * 从Redis中删除用户标签信息
     * 此方法首先构建Redis中的标签键，然后删除该键
     * 接着创建一个异步删除缓存的通知对象，设置其代码和参数
     * 最后，创建一个消息对象，设置其主题和体，用于异步删除缓存
     *
     * @param userId 用户ID，用于构建Redis键和异步删除缓存
     */
    private void deleteUserTagDTOFromRedis(Long userId) {
        //构建Redis中的标签键
        String redisKey = cacheKeyBuilder.buildTagKey(userId);
        //删除Redis中的用户标签信息
        redisTemplate.delete(redisKey);

        //创建异步删除缓存的通知对象
        UserCacheAsyncDeleteDTO userCacheAsyncDeleteDTO = new UserCacheAsyncDeleteDTO();
        //设置异步删除缓存的通知代码
        userCacheAsyncDeleteDTO.setCode(CacheAsyncDeleteCode.USER_TAG_DELETE.getCode());
        //创建参数Map，用于存储用户ID
        Map<String, Object> jsonParam = new HashMap<>();
        jsonParam.put("userId", userId);
        //将参数Map转换为JSON字符串，并设置到通知对象中
        userCacheAsyncDeleteDTO.setJson(JSON.toJSONString(jsonParam));

        //创建消息对象
        Message message = new Message();
        //设置消息的主题
        message.setTopic(UserProviderTopicNames.CACHE_ASYNC_DELETE_TOPIC);
        //将通知对象转换为JSON字符串，并设置为消息的体
        message.setBody(JSON.toJSONString(userCacheAsyncDeleteDTO).getBytes());
        //设置消息的延迟级别，用于异步删除缓存
        message.setDelayTimeLevel(1);
        //发送消息，如果发生异常则抛出运行时异常
        try {
            mqProducer.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 从Redis中查询指定用户的标签信息
     * 如果在Redis中未找到相关信息，则从数据库中查询，并将结果缓存到Redis中
     *
     * @param userId 用户ID，用于查询用户标签信息
     * @return 返回用户标签信息的DTO对象，如果没有找到则返回null
     */
    private UserTagDTO queryByUserIdFromRedis(Long userId) {
        // 构建Redis缓存的键
        String redisKey = cacheKeyBuilder.buildTagKey(userId);

        // 从Redis中获取用户标签信息
        UserTagDTO userTagDTO = redisTemplate.opsForValue().get(redisKey);

        // 如果在Redis中找到了用户标签信息，则直接返回
        if (userTagDTO != null) {
            return userTagDTO;
        }

        // 如果Redis中没有相关信息，则从数据库中查询用户标签信息
        UserTagPO userTagPO = userTagMapper.selectById(userId);

        // 如果数据库中也没有找到相关信息，则返回null
        if (userTagPO == null) {
            return null;
        }

        // 将数据库中的用户标签信息转换为DTO对象
        userTagDTO = ConvertBeanUtils.convert(userTagPO, UserTagDTO.class);

        // 将查询到的用户标签信息缓存到Redis中
        redisTemplate.opsForValue().set(redisKey, userTagDTO);

        // 设置Redis缓存的过期时间
        redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);

        // 返回用户标签信息的DTO对象
        return userTagDTO;
    }
}

