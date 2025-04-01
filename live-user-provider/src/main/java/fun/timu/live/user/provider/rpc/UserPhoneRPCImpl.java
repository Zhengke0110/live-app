package fun.timu.live.user.provider.rpc;

import fun.timu.live.user.dto.UserLoginDTO;
import fun.timu.live.user.dto.UserPhoneDTO;
import fun.timu.live.user.interfaces.IUserPhoneRPC;
import fun.timu.live.user.provider.service.IUserPhoneService;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
public class UserPhoneRPCImpl implements IUserPhoneRPC {


    private final IUserPhoneService userPhoneService;

    public UserPhoneRPCImpl(IUserPhoneService userPhoneService) {
        this.userPhoneService = userPhoneService;
    }

    @Override
    public UserLoginDTO login(String phone) {
        return userPhoneService.login(phone);
    }

    @Override
    public UserPhoneDTO queryByPhone(String phone) {
        return userPhoneService.queryByPhone(phone);
    }

    @Override
    public List<UserPhoneDTO> queryByUserId(Long userId) {
        return userPhoneService.queryByUserId(userId);
    }
}
