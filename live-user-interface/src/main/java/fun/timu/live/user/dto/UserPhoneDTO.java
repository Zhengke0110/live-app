package fun.timu.live.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserPhoneDTO implements Serializable {
    private Long id;
    private Long userId;
    private String phone;
    private Integer status;
    private Date createTime;
    private Date updateTime;
    private static final long serialVersionUID = 1L;
}
