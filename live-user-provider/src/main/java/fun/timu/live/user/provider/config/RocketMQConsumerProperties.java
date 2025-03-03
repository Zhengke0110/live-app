package fun.timu.live.user.provider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@ConfigurationProperties(prefix = "rocketmq.consumer")
@Configuration
public class RocketMQConsumerProperties {
    //rocketmq 的 nameSever 地址
    private String nameSrv;
    //分组名称
    private String groupName;
}
