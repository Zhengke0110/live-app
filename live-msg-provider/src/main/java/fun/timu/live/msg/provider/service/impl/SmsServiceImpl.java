package fun.timu.live.msg.provider.service.impl;

import fun.timu.live.common.interfaces.utils.DESUtils;
import fun.timu.live.framework.redis.starter.key.MsgProviderCacheKeyBuilder;
import fun.timu.live.msg.dto.MsgCheckDTO;
import fun.timu.live.msg.enums.MsgSendResultEnum;
import fun.timu.live.msg.provider.config.ApplicationProperties;
import fun.timu.live.msg.provider.config.ThreadPoolManager;
import fun.timu.live.msg.provider.dao.mapper.SmsMapper;
import fun.timu.live.msg.provider.dao.po.SmsPO;
import fun.timu.live.msg.provider.service.ISmsService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements ISmsService {
    private static Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);

    private final SmsMapper smsMapper;
    @Resource
    private RedisTemplate<String, Integer> redisTemplate;

    private final MsgProviderCacheKeyBuilder msgProviderCacheKeyBuilder;

    public SmsServiceImpl(SmsMapper smsMapper, MsgProviderCacheKeyBuilder msgProviderCacheKeyBuilder) {
        this.smsMapper = smsMapper;
        this.msgProviderCacheKeyBuilder = msgProviderCacheKeyBuilder;
    }

    /**
     * 发送登录验证码
     *
     * @param phone 手机号
     * @return 发送结果枚举
     */
    @Override
    public MsgSendResultEnum sendLoginCode(String phone) {
        // 检查手机号是否为空
        if (StringUtils.isEmpty(phone)) {
            return MsgSendResultEnum.MSG_PARAM_ERROR;
        }
        // 1.生成验证码，4位，6位（取它），有效期（30s，60s），同一个手机号不能重发，redis去存储验证码
        String codeCacheKey = msgProviderCacheKeyBuilder.buildSmsLoginCodeKey(phone);
        // 检查验证码是否已存在，防止重复发送
        if (redisTemplate.hasKey(codeCacheKey)) {
            logger.warn("该手机号短信发送过于频繁，phone is {}", phone);
            return MsgSendResultEnum.SEND_FAIL;
        }
        // 生成4位随机验证码
        int code = RandomUtils.nextInt(1000, 9999);
        // 将验证码存入Redis，设置过期时间为60秒
        redisTemplate.opsForValue().set(codeCacheKey, code, 60, TimeUnit.SECONDS);
        //发送验证码
        ThreadPoolManager.commonAsyncPool.execute(() -> {
            // 模拟发送验证码给手机
            logger.info("发送验证码====>手机号：{}，验证码：{}", phone, code);
            // 插入发送记录到数据库
            insertOne(phone, code);
        });
        //插入验证码发送记录
        return MsgSendResultEnum.SEND_SUCCESS;
    }

    /**
     * 检查登录验证码的合法性
     *
     * @param phone 用户手机号，用于查找对应的验证码
     * @param code 用户输入的验证码
     * @return 返回一个MsgCheckDTO对象，包含验证码校验结果和提示信息
     */
    @Override
    public MsgCheckDTO checkLoginCode(String phone, Integer code) {
        //参数校验
        if (StringUtils.isEmpty(phone) || code == null || code < 1000) {
            return new MsgCheckDTO(false, "参数异常");
        }

        //redis校验验证码
        String codeCacheKey = msgProviderCacheKeyBuilder.buildSmsLoginCodeKey(phone);
        Integer cacheCode = (Integer) redisTemplate.opsForValue().get(codeCacheKey);
        if (cacheCode == null || cacheCode < 1000) {
            return new MsgCheckDTO(false, "验证码已过期");
        }
        if (cacheCode.equals(code)) {
            redisTemplate.delete(codeCacheKey);
            return new MsgCheckDTO(true, "验证码校验成功");
        }
        return new MsgCheckDTO(false, "验证码校验失败");
    }

    /**
     * 插入一条短信验证码记录
     *
     * @param phone 用户手机号，需要加密处理以保护用户隐私
     * @param code 短信验证码，用于用户身份验证
     */
    @Override
    public void insertOne(String phone, Integer code) {
        // 创建SmsPO对象，用于存储短信验证码相关信息
        SmsPO smsPO = new SmsPO();
        // 设置加密后的手机号，使用DES加密算法确保数据安全
        smsPO.setPhone(DESUtils.encrypt(phone));
        // 设置验证码
        smsPO.setCode(code);
        // 调用mapper层的插入方法，将短信验证码记录插入数据库
        smsMapper.insert(smsPO);
    }
}
