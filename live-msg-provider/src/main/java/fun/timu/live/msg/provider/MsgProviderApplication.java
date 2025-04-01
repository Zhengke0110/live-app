package fun.timu.live.msg.provider;


import fun.timu.live.msg.dto.MsgCheckDTO;
import fun.timu.live.msg.enums.MsgSendResultEnum;
import fun.timu.live.msg.provider.service.ISmsService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

import java.util.Scanner;


@SpringBootApplication
@EnableDubbo
@EnableDiscoveryClient
public class MsgProviderApplication implements CommandLineRunner {
    @Resource
    private ISmsService smsService;

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(MsgProviderApplication.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        String phoneNumber = "19012345678";
        MsgSendResultEnum msgSendResultEnum = smsService.sendLoginCode(phoneNumber);
        System.out.println(msgSendResultEnum);
        while (true) {
            System.out.println("输入验证码");
            Scanner scanner = new Scanner(System.in);
            Integer code = scanner.nextInt();
            MsgCheckDTO checkStatus = smsService.checkLoginCode(phoneNumber, code);
            if (checkStatus.isCheckStatus()) {
                System.out.println("验证码正确");
            } else {
                System.out.println("验证码错误");
            }

        }

    }
}