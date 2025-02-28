package fun.timu.live.user.provider.rpc;

import fun.timu.live.user.interfaces.IUserRpc;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class UserRpcImpl implements IUserRpc {
    @Override
    public String test() {
        System.out.println("UserRpcImpl.test");
        return "success";
    }
}
