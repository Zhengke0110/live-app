package fun.timu.live.user.provider.config;

import com.alibaba.fastjson.JSON;
import fun.timu.live.framework.redis.starter.key.UserProviderCacheKeyBuilder;
import fun.timu.live.user.dto.UserDTO;
import jakarta.annotation.Resource;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

@Configuration
public class RocketMQConsumerConfig implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocketMQConsumerConfig.class);
    @Resource
    private RocketMQConsumerProperties consumerProperties;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private UserProviderCacheKeyBuilder userProviderCacheKeyBuilder;

    @Override
    public void afterPropertiesSet() throws Exception {
        initConsumer();
    }

    /**
     * 初始化 RocketMQ 消费者
     * 该方法负责配置和启动一个 RocketMQ 消费者实例，根据给定的属性进行设置，
     * 并定义消息监听器以处理接收到的消息
     */
    public void initConsumer() {
        try {
            //初始化我们的 RocketMQ 消费者
            DefaultMQPushConsumer defaultMQPushConsumer = new DefaultMQPushConsumer();

            //设置 Name Server 地址
            defaultMQPushConsumer.setNamesrvAddr(consumerProperties.getNameSrv());

            //设置消费者组名
            defaultMQPushConsumer.setConsumerGroup(consumerProperties.getGroupName());

            //设置消费消息的最大批次
            defaultMQPushConsumer.setConsumeMessageBatchMaxSize(1);

            //设置从第一条消息开始消费
            defaultMQPushConsumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_FIRST_OFFSET);

            //订阅主题
            defaultMQPushConsumer.subscribe("user-update-cache", "*");

            //设置消息监听器
            defaultMQPushConsumer.setMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                    //解析消息体为 UserDTO 对象
                    String msgStr = new String(msgs.get(0).getBody());
                    UserDTO userDTO = JSON.parseObject(msgStr, UserDTO.class);

                    //检查用户信息是否有效
                    if (userDTO == null || userDTO.getUserId() == null) {
                        LOGGER.error("用户 id 为空，参数异常，内容: {} ", msgStr);
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    }

                    //延迟消息的回调，处理相关的缓存二次删除
                    redisTemplate.delete(userProviderCacheKeyBuilder.buildUserInfoKey(userDTO.getUserId()));
                    LOGGER.error("延迟删除处理，userDTO is {}", userDTO);

                    //确认消息消费成功
                    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                }
            });

            //启动消费者
            defaultMQPushConsumer.start();

            //日志记录消费者启动成功
            LOGGER.info("MQ 消费者启动成功,nameSrv is {}", consumerProperties.getNameSrv());
        } catch (MQClientException e) {
            //如果初始化消费者时发生异常，则抛出运行时异常
            throw new RuntimeException(e);
        }
    }
}