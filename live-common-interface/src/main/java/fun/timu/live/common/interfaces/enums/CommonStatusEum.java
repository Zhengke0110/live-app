package fun.timu.live.common.interfaces.enums;

import lombok.Getter;

@Getter
public enum CommonStatusEum {
    INVALID_STATUS(0, "无效"),
    VALID_STATUS(1, "有效");

    CommonStatusEum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    int code;
    String desc;
}
