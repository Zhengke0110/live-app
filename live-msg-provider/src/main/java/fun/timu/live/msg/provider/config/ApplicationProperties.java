package fun.timu.live.msg.provider.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ApplicationProperties {
    private String smsServerIp;
    private Integer port;
    private String accountSId;
    private String accountToken;
    private String appId;
    private String testPhone;
}
