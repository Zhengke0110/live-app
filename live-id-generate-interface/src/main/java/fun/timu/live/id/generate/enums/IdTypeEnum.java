package fun.timu.live.id.generate.enums;


import lombok.Getter;

@Getter
public enum IdTypeEnum {
    USER_ID(1,"用户id生成策略");

    int code;
    String desc;

    IdTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
