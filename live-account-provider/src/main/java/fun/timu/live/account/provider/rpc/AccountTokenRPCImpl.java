package fun.timu.live.account.provider.rpc;

import fun.timu.live.account.interfaces.IAccountTokenRPC;
import fun.timu.live.account.provider.service.IAccountTokenService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class AccountTokenRPCImpl implements IAccountTokenRPC {

    private final IAccountTokenService accountTokenService;

    public AccountTokenRPCImpl(IAccountTokenService accountTokenService) {
        this.accountTokenService = accountTokenService;
    }

    @Override
    public String createAndSaveLoginToken(Long userId) {
        return accountTokenService.createAndSaveLoginToken(userId);
    }

    @Override
    public Long getUserIdByToken(String tokenKey) {
        return accountTokenService.getUserIdByToken(tokenKey);
    }
}
