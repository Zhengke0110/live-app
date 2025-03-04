package fun.timu.live.framework.datasource.starter.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.shardingsphere.driver.jdbc.core.driver.ShardingSphereDriverURLProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Properties;

public class NacosDriverURLProvider implements ShardingSphereDriverURLProvider {
    private static Logger logger = LoggerFactory.getLogger(NacosDriverURLProvider.class);
    private static final String NACOS_TYPE = "nacos:";
    private static final String GROUP = "DEFAULT_GROUP";

    @Override
    public boolean accept(String url) {
        return url != null && url.contains(NACOS_TYPE);
    }

    @Override
    public byte[] getContent(final String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }

        // 提取nacos URL部分: 127.0.0.1:8848:live-user-shardingjdbc.yaml
        String nacosUrl = url.substring(url.lastIndexOf(NACOS_TYPE) + NACOS_TYPE.length());
        String[] nacosStr = nacosUrl.split(":");
        String serverAddr = nacosStr[0] + ":" + nacosStr[1];
        String nacosFileStr = nacosStr[2];

        // 设置服务器地址属性
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr);

        // 检查是否提供了额外参数
        String dataId;
        if (nacosFileStr.contains("?")) {
            String[] nacosFileProp = nacosFileStr.split("\\?");
            dataId = nacosFileProp[0];

            // 如果存在额外参数，进行处理
            if (nacosFileProp.length > 1) {
                String[] acceptProp = nacosFileProp[1].split("&&");
                for (String propertyName : acceptProp) {
                    String[] propertyItem = propertyName.split("=");
                    if (propertyItem.length == 2) {
                        String key = propertyItem[0];
                        String value = propertyItem[1];

                        // 只有在提供时才设置认证属性
                        switch (key) {
                            case "username":
                                properties.setProperty(PropertyKeyConst.USERNAME, value);
                                break;
                            case "password":
                                properties.setProperty(PropertyKeyConst.PASSWORD, value);
                                break;
                            case "namespace":
                                properties.setProperty(PropertyKeyConst.NAMESPACE, value);
                                break;
                        }
                    }
                }
            }
        } else {
            // 不包含查询参数的简单情况
            dataId = nacosFileStr;
        }

        try {
            ConfigService configService = NacosFactory.createConfigService(properties);
            String content = configService.getConfig(dataId, GROUP, 6000);
            logger.info("从Nacos获取配置内容，dataId: {}", dataId);
            return content.getBytes();
        } catch (NacosException e) {
            logger.error("从Nacos获取配置失败", e);
            throw new RuntimeException("从Nacos获取配置失败", e);
        }
    }
}
