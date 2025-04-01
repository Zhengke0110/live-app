package fun.timu.live.msg.provider.rpc;

import fun.timu.live.msg.dto.MsgCheckDTO;
import fun.timu.live.msg.enums.MsgSendResultEnum;
import fun.timu.live.msg.interfaces.ISmsRpc;
import fun.timu.live.msg.provider.service.ISmsService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class SmsRpcImpl implements ISmsRpc {
    private final ISmsService smsService;

    public SmsRpcImpl(ISmsService smsService) {
        this.smsService = smsService;
    }

    @Override
    public MsgSendResultEnum sendLoginCode(String phone) {
        return smsService.sendLoginCode(phone);
    }

    @Override
    public MsgCheckDTO checkLoginCode(String phone, Integer code) {
        return smsService.checkLoginCode(phone, code);
    }
}
