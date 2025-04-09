package fun.timu.live.account.provider;

import com.alibaba.dubbo.config.spring.context.annotation.EnableDubbo;
import fun.timu.live.account.provider.service.IAccountTokenService;
import jakarta.annotation.Resource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class AccountProviderApplication implements CommandLineRunner {

    @Resource
    private IAccountTokenService accountTokenService;

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(AccountProviderApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        long userID = 123456L;
        String token = accountTokenService.createAndSaveLoginToken(userID);
        System.out.println("token->" + token);
        Long userId = accountTokenService.getUserIdByToken(token);
        System.out.println("userId->" + userId);
    }
}
