package fun.timu.live.msg.provider.config;

import lombok.Getter;

@Getter
public enum SmsTemplateIDEnum {

    SMS_LOGIN_CODE_TEMPLATE("1", "登录验证码短信模版");

    String templateId;
    String desc;

    SmsTemplateIDEnum(String templateId, String desc) {
        this.templateId = templateId;
        this.desc = desc;
    }
}
