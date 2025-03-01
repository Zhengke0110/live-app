package fun.timu.live.user.dto;

import lombok.Data;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class UserDTO implements Serializable {

    private Long userId;
    private String nickName;
    private String trueName;
    private String avatar;
    private Integer sex;
    private Integer workCity;
    private Integer bornCity;
    private Date bornDate;
    private Date createTime;
    private Date updateTime;

    @Serial
    private static final long serialVersionUID = 1L;

}
