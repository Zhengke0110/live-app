package fun.timu.live.account.provider.service.impl;


import fun.timu.live.account.provider.service.IAccountTokenService;
import fun.timu.live.framework.redis.starter.key.AccountProviderCacheKeyBuilder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AccountTokenServiceImpl implements IAccountTokenService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private AccountProviderCacheKeyBuilder cacheKeyBuilder;

    /**
     * 创建并保存登录令牌
     * <p>
     * 该方法用于生成一个唯一的登录令牌（token），并将该令牌与用户ID关联，保存到Redis缓存中
     * 使用Redis存储登录令牌，可以实现快速的令牌验证和用户会话管理
     *
     * @param userId 用户ID，用于关联生成的登录令牌
     * @return 返回生成的登录令牌
     */
    @Override
    public String createAndSaveLoginToken(Long userId) {
        // 生成一个全局唯一的登录令牌
        String token = UUID.randomUUID().toString();

        // 将登录令牌与用户ID关联，并保存到Redis缓存中，设置过期时间为30天
        // 这里使用了缓存键构建器buildUserLoginTokenKey来生成缓存键，以保证键的命名规范和唯一性
        redisTemplate.opsForValue().set(cacheKeyBuilder.buildUserLoginTokenKey(token), String.valueOf(userId), 30, TimeUnit.DAYS);

        // 返回生成的登录令牌
        return token;
    }

    /**
     * 根据token获取用户ID
     * 该方法用于解析token并从缓存中获取对应的用户ID
     * 主要用于用户身份验证过程中，通过token反向查找用户信息
     *
     * @param tokenKey token键值，用于生成Redis中的键
     * @return 用户ID，如果找不到则返回null
     */
    @Override
    public Long getUserIdByToken(String tokenKey) {
        // 构建Redis中的键，用于存储用户登录token
        String redisKey = cacheKeyBuilder.buildUserLoginTokenKey(tokenKey);

        // 从Redis中获取与键关联的用户ID值
        Integer userId = (Integer) redisTemplate.opsForValue().get(redisKey);

        // 如果用户ID为空，则直接返回null；否则将其转换为Long类型并返回
        return userId == null ? null : Long.valueOf(userId);
    }
}
