package fun.timu.live.user.provider;

import fun.timu.live.user.dto.UserLoginDTO;
import fun.timu.live.user.provider.service.IUserPhoneService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


/**
 * 用户中台服务提供者启动类
 */
@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class UserProviderApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(UserProviderApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

    @Resource
    private IUserPhoneService userPhoneService;

    @Override
    public void run(String... args) throws Exception {
        String phoneNumber = "19012345678";
        UserLoginDTO userLoginDTO = userPhoneService.login(phoneNumber);
        System.out.println(userLoginDTO);
        System.out.println("根据UserID查询===>" + userPhoneService.queryByUserId(userLoginDTO.getUserId()));
        System.out.println("根据UserID查询===>" + userPhoneService.queryByUserId(userLoginDTO.getUserId()));
        System.out.println("根据PhoneNumber查询===>" + userPhoneService.queryByPhone(phoneNumber));
        System.out.println("根据PhoneNumber查询===>" + userPhoneService.queryByPhone(phoneNumber));
    }
}
