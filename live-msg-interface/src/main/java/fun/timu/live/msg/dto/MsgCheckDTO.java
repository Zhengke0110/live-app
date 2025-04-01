package fun.timu.live.msg.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MsgCheckDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private boolean checkStatus;
    private String desc;

    public MsgCheckDTO(boolean checkStatus, String desc) {
        this.checkStatus = checkStatus;
        this.desc = desc;
    }
}