package fun.timu.live.msg.enums;


import lombok.Getter;

@Getter
public enum MsgSendResultEnum {

    SEND_SUCCESS(0, "成功"), SEND_FAIL(1, "发送失败"), MSG_PARAM_ERROR(2, "消息参数异常");

    int code;
    String desc;

    MsgSendResultEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }


}