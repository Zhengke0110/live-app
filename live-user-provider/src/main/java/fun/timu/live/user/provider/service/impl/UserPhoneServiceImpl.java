package fun.timu.live.user.provider.service.impl;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import fun.timu.live.common.interfaces.enums.CommonStatusEum;
import fun.timu.live.common.interfaces.utils.ConvertBeanUtils;
import fun.timu.live.common.interfaces.utils.DESUtils;
import fun.timu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import fun.timu.live.id.generate.enums.IdTypeEnum;
import fun.timu.live.id.generate.interfaces.IdGenerateRpc;
import fun.timu.live.user.dto.UserDTO;
import fun.timu.live.user.dto.UserLoginDTO;
import fun.timu.live.user.dto.UserPhoneDTO;
import fun.timu.live.user.provider.dao.mapper.IUserPhoneMapper;
import fun.timu.live.user.provider.dao.po.UserPhonePO;
import fun.timu.live.user.provider.service.IUserPhoneService;
import fun.timu.live.user.provider.service.IUserService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class UserPhoneServiceImpl implements IUserPhoneService {
    private final IUserPhoneMapper userPhoneMapper;
    private final IUserService userService;
    private final UserProviderCacheKeyBuilder cacheKeyBuilder;
    @DubboReference
    private IdGenerateRpc idGenerateRpc;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public UserPhoneServiceImpl(IUserPhoneMapper userPhoneMapper, IUserService userService, UserProviderCacheKeyBuilder cacheKeyBuilder) {
        this.userPhoneMapper = userPhoneMapper;
        this.userService = userService;
        this.cacheKeyBuilder = cacheKeyBuilder;
    }

    /**
     * 用户登录方法
     *
     * @param phone 用户手机号，用于登录
     * @return UserLoginDTO对象，包含登录结果和用户信息如果登录成功，否则返回null
     */
    @Override
    public UserLoginDTO login(String phone) {
        // 检查手机号是否为空，为空则返回null，表示登录失败
        if (StringUtils.isEmpty(phone)) {
            return null;
        }
        // 查询手机号是否已注册
        UserPhoneDTO userPhoneDTO = this.queryByPhone(phone);
        // 如果手机号已注册，创建并返回登录成功的UserLoginDTO对象
        if (userPhoneDTO != null) {
            return UserLoginDTO.loginSuccess(userPhoneDTO.getUserId());
        }
        // 如果手机号未注册，执行注册并登录的逻辑
        return registerAndLogin(phone);
    }

    /**
     * 注册并登录用户
     * <p>
     * 本方法实现用户的注册和登录流程，主要步骤包括：
     * 1. 生成用户ID
     * 2. 创建用户信息并插入数据库
     * 3. 将用户手机号加密后存入数据库
     * 4. 删除缓存中的用户手机号信息
     * 5. 返回用户登录成功信息
     *
     * @param phone 用户手机号，用于注册和登录
     * @return UserLoginDTO 包含用户登录信息的对象
     */
    private UserLoginDTO registerAndLogin(String phone) {
        // 生成用户ID
        Long userId = idGenerateRpc.getUnSeqId(IdTypeEnum.USER_ID.getCode());

        // 创建用户基本信息并插入数据库
        UserDTO userDTO = new UserDTO();
        userDTO.setNickName("用户-" + userId);
        userDTO.setUserId(userId);
        userService.insertOne(userDTO);

        // 将用户手机号加密后存入数据库
        UserPhonePO userPhonePO = new UserPhonePO();
        userPhonePO.setUserId(userId);
        userPhonePO.setPhone(DESUtils.encrypt(phone));
        userPhonePO.setStatus(CommonStatusEum.VALID_STATUS.getCode());
        userPhoneMapper.insert(userPhonePO);

        // 删除缓存中的用户手机号信息，确保数据一致性
        redisTemplate.delete(cacheKeyBuilder.buildUserPhoneObjKey(phone));

        // 返回用户登录成功信息
        return UserLoginDTO.loginSuccess(userId);
    }

    /**
     * 根据电话号码查询用户信息
     * 首先检查电话号码是否为空，如果为空则返回null
     * 接着尝试从Redis缓存中获取用户信息，如果缓存中存在且不为空，则直接返回用户信息
     * 如果缓存中不存在，则从数据库中查询用户信息，并在返回前对电话号码进行解密
     * 如果数据库中查询到用户信息，将其存入Redis缓存中，并设置过期时间
     * 如果数据库中未查询到用户信息，则创建一个空的UserPhoneDTO对象存入缓存，以避免缓存击穿
     *
     * @param phone 电话号码
     * @return 用户信息对象，如果未找到则返回null
     */
    @Override
    public UserPhoneDTO queryByPhone(String phone) {
        // 检查电话号码是否为空
        if (StringUtils.isEmpty(phone)) {
            return null;
        }
        // 构建Redis缓存的键
        String redisKey = cacheKeyBuilder.buildUserPhoneObjKey(phone);
        // 尝试从Redis缓存中获取用户信息
        UserPhoneDTO userPhoneDTO = (UserPhoneDTO) redisTemplate.opsForValue().get(redisKey);
        // 如果缓存中存在用户信息
        if (userPhoneDTO != null) {
            // 属于空值缓存对象
            if (userPhoneDTO.getUserId() == null) {
                return null;
            }
            return userPhoneDTO;
        }
        // 如果缓存中不存在用户信息，则从数据库中查询
        userPhoneDTO = this.queryByPhoneFromDB(phone);
        // 如果数据库中查询到用户信息
        if (userPhoneDTO != null) {
            // 对电话号码进行解密
            userPhoneDTO.setPhone(DESUtils.decrypt(userPhoneDTO.getPhone()));
            // 将用户信息存入Redis缓存，并设置过期时间
            redisTemplate.opsForValue().set(redisKey, userPhoneDTO, 30, TimeUnit.MINUTES);
            return userPhoneDTO;
        }
        // 缓存击穿，空值缓存
        userPhoneDTO = new UserPhoneDTO();
        // 将空的UserPhoneDTO对象存入缓存，以避免缓存击穿，并设置较短的过期时间
        redisTemplate.opsForValue().set(redisKey, userPhoneDTO, 5, TimeUnit.MINUTES);
        return null;
    }

    /**
     * 根据用户ID查询用户电话信息列表
     * 首先检查传入的用户ID是否有效，然后尝试从缓存中获取数据
     * 如果缓存中没有数据，则从数据库中查询，并将结果缓存
     * 此方法还处理了空对象缓存的情况，以避免缓存击穿
     *
     * @param userId 用户ID，用于查询用户电话信息
     * @return 用户电话信息列表，如果用户ID无效或没有找到信息，则返回空列表
     */
    @Override
    public List<UserPhoneDTO> queryByUserId(Long userId) {
        // 检查用户ID是否有效
        if (userId == null || userId < 10000) {
            return Collections.emptyList();
        }
        // 构建缓存键
        String redisKey = cacheKeyBuilder.buildUserPhoneListKey(userId);
        // 从缓存中获取用户电话信息列表
        List<Object> userPhoneList = redisTemplate.opsForList().range(redisKey, 0, -1);
        // 检查缓存中是否有数据
        if (!CollectionUtils.isEmpty(userPhoneList)) {
            // 证明是空值缓存
            if (((UserPhoneDTO) userPhoneList.get(0)).getUserId() == null) {
                return Collections.emptyList();
            }
            // 将缓存中的数据转换为UserPhoneDTO列表并返回
            return userPhoneList.stream().map(x -> (UserPhoneDTO) x).collect(Collectors.toList());
        }
        // 从数据库中查询用户电话信息
        List<UserPhoneDTO> userPhoneDTOS = this.queryByUserIdFromDB(userId);
        // 如果查询到数据
        if (!CollectionUtils.isEmpty(userPhoneDTOS)) {
            // 解密电话号码
            userPhoneDTOS.stream().forEach(x -> x.setPhone(DESUtils.decrypt(x.getPhone())));
            // 将查询结果缓存，并设置过期时间
            redisTemplate.opsForList().leftPushAll(redisKey, userPhoneDTOS.toArray());
            redisTemplate.expire(redisKey, 30, TimeUnit.MINUTES);
            return userPhoneDTOS;
        }
        // 缓存击穿，空对象缓存
        redisTemplate.opsForList().leftPush(redisKey, new UserPhoneDTO());
        redisTemplate.expire(redisKey, 5, TimeUnit.MINUTES);
        return Collections.emptyList();
    }

    /**
     * 根据用户ID从数据库中查询用户电话信息
     * 此方法专注于处理用户电话信息的查询，确保只获取状态为有效的最新记录
     * 选择使用LambdaQueryWrapper以提高查询的可读性和效率
     *
     * @param userId 用户ID，用于查询特定用户的电话信息
     * @return 返回一个用户电话DTO列表，尽管预期结果是单个对象，但返回列表以保持灵活性
     */
    private List<UserPhoneDTO> queryByUserIdFromDB(Long userId) {
        // 创建LambdaQueryWrapper实例，用于构建查询条件
        LambdaQueryWrapper<UserPhonePO> queryWrapper = new LambdaQueryWrapper<>();

        // 设置查询条件，匹配用户ID
        queryWrapper.eq(UserPhonePO::getUserId, userId);

        // 仅查询有效状态的记录，确保数据的准确性
        queryWrapper.eq(UserPhonePO::getStatus, CommonStatusEum.VALID_STATUS.getCode());

        // 限制查询结果数量为1，旨在获取最新或唯一的有效电话信息记录
        queryWrapper.last("limit 1");

        // 使用ConvertBeanUtils工具类将查询结果转换为UserPhoneDTO列表并返回
        return ConvertBeanUtils.convertList(userPhoneMapper.selectList(queryWrapper), UserPhoneDTO.class);
    }

    /**
     * 根据电话号码从数据库中查询用户信息
     * 此方法首先对输入的电话号码进行加密，然后在数据库中查找与加密后的电话号码匹配且状态为有效的用户信息
     * 如果找到匹配的记录，将其转换为UserPhoneDTO对象并返回
     *
     * @param phone 用户的电话号码，用于查询
     * @return 如果找到匹配的用户信息，则返回UserPhoneDTO对象；否则返回null
     */
    private UserPhoneDTO queryByPhoneFromDB(String phone) {
        // 创建LambdaQueryWrapper对象，用于构建查询条件
        LambdaQueryWrapper<UserPhonePO> queryWrapper = new LambdaQueryWrapper<>();

        // 对输入的电话号码进行加密，并作为查询条件之一
        queryWrapper.eq(UserPhonePO::getPhone, DESUtils.encrypt(phone));

        // 添加状态为有效的查询条件，确保只查询有效的用户信息
        queryWrapper.eq(UserPhonePO::getStatus, CommonStatusEum.VALID_STATUS.getCode());

        // 设置查询的SQL语句末尾为"limit 1"，确保只返回一条记录
        queryWrapper.last("limit 1");

        // 执行查询，并将查询结果转换为UserPhoneDTO对象后返回
        return ConvertBeanUtils.convert(userPhoneMapper.selectOne(queryWrapper), UserPhoneDTO.class);
    }

}
