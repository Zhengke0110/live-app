package fun.timu.live.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UserCacheAsyncDeleteDTO implements Serializable {
    /**
     * 不同业务场景的code，区别不同的延迟消息
     */
    private int code;
    private String json;
    @Serial
    private static final long serialVersionUID = 1L;
}
