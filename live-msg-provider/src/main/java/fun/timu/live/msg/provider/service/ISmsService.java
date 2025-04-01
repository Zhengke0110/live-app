package fun.timu.live.msg.provider.service;

import fun.timu.live.msg.dto.MsgCheckDTO;
import fun.timu.live.msg.enums.MsgSendResultEnum;

public interface ISmsService {

    /**
     * 发送短信接口
     *
     * @param phone
     * @return
     */
    MsgSendResultEnum sendLoginCode(String phone);

    /**
     * 校验登录验证码
     *
     * @param phone
     * @param code
     * @return
     */
    MsgCheckDTO checkLoginCode(String phone, Integer code);

    /**
     * 插入一条短信验证码记录
     *
     * @param phone
     * @param code
     */
    void insertOne(String phone, Integer code);
}
