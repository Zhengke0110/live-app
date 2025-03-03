package fun.timu.live.user.provider.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "rocketmq.producer")
@Configuration
public class RocketMQProducerProperties {
    //rocketmq 的 nameSever 地址
    private String nameSrv;
    //分组名称
    private String groupName;
    //消息重发次数
    private int retryTimes;
    //发送超时时间
    private int sendTimeOut;
}
