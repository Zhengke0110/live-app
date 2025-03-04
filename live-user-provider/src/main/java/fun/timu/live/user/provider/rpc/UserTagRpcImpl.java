package fun.timu.live.user.provider.rpc;

import fun.timu.live.user.constants.UserTagsEnum;
import fun.timu.live.user.interfaces.IUserTagRpc;
import fun.timu.live.user.provider.service.IUserTagService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class UserTagRpcImpl implements IUserTagRpc {
    private final IUserTagService userTagService;

    public UserTagRpcImpl(IUserTagService userTagService) {
        this.userTagService = userTagService;
    }

    @Override
    public boolean setTag(Long userId, UserTagsEnum userTagsEnum) {
        return userTagService.setTag(userId, userTagsEnum);
    }

    @Override
    public boolean cancelTag(Long userId, UserTagsEnum userTagsEnum) {
        return userTagService.cancelTag(userId, userTagsEnum);
    }

    @Override
    public boolean containTag(Long userId, UserTagsEnum userTagsEnum) {
        return userTagService.containTag(userId, userTagsEnum);
    }
}
