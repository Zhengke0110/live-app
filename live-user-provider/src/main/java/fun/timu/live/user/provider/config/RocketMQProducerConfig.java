package fun.timu.live.user.provider.config;

import jakarta.annotation.Resource;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class RocketMQProducerConfig {
    private final static Logger LOGGER = LoggerFactory.getLogger(RocketMQProducerConfig.class);
    @Resource
    private RocketMQProducerProperties producerProperties;
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 创建并配置MQProducer bean
     *
     * 该方法首先创建一个线程池用于异步发送消息，然后初始化一个DefaultMQProducer实例，
     * 并设置其相关属性，如NameServer地址、生产者组名、发送失败重试次数等最后启动生产者
     *
     * @return 配置好的MQProducer实例
     * @throws RuntimeException 如果生产者启动过程中抛出MQClientException，则将其转换为RuntimeException抛出
     */
    @Bean
    public MQProducer mqProducer() {
        // 创建线程池，用于异步发送消息
        ThreadPoolExecutor asyncThreadPoolExecutor = new ThreadPoolExecutor(100, 150, 3, TimeUnit.MINUTES, new ArrayBlockingQueue<>(1000), new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                // 为线程命名，方便调试和识别
                thread.setName(applicationName + ":rocketmq-producer:" + ThreadLocalRandom.current().nextInt(1000));
                return thread;
            }
        });
        //初始化 rocketmq 的生产者
        DefaultMQProducer defaultMQProducer = new DefaultMQProducer();
        try {
            // 设置NameServer地址
            defaultMQProducer.setNamesrvAddr(producerProperties.getNameSrv());
            // 设置生产者组名
            defaultMQProducer.setProducerGroup(producerProperties.getGroupName());
            // 设置发送失败重试次数
            defaultMQProducer.setRetryTimesWhenSendFailed(producerProperties.getRetryTimes());
            // 设置异步发送失败重试次数
            defaultMQProducer.setRetryTimesWhenSendAsyncFailed(producerProperties.getRetryTimes());
            // 设置是否在消息无法存储时尝试发送到其他Broker
            defaultMQProducer.setRetryAnotherBrokerWhenNotStoreOK(true);
            //设置异步发送的线程池
            defaultMQProducer.setAsyncSenderExecutor(asyncThreadPoolExecutor);
            // 启动生产者
            defaultMQProducer.start();
            // 记录生产者启动日志
            LOGGER.info("MQ 生产者启动成功,nameSrv is {}", producerProperties.getNameSrv());
        } catch (MQClientException e) {
            // 如果启动失败，抛出运行时异常
            throw new RuntimeException(e);
        }
        // 返回配置好的生产者实例
        return defaultMQProducer;
    }
}