package fun.timu.live.user.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class UserTagDTO implements Serializable {

    private Long userId;
    private Long tagInfo01;
    private Long tagInfo02;
    private Long tagInfo03;
    private Date createTime;
    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
