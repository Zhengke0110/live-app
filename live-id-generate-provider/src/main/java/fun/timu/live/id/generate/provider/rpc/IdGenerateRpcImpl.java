package fun.timu.live.id.generate.provider.rpc;

import fun.timu.live.id.generate.interfaces.IdGenerateRpc;
import fun.timu.live.id.generate.provider.service.IdGenerateService;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class IdGenerateRpcImpl implements IdGenerateRpc {

    private final IdGenerateService idGenerateService;

    public IdGenerateRpcImpl(IdGenerateService idGenerateService) {
        this.idGenerateService = idGenerateService;
    }

    @Override
    public Long getSeqId(Integer id) {
        return idGenerateService.getSeqId(id);
    }

    @Override
    public Long getUnSeqId(Integer id) {
        return idGenerateService.getUnSeqId(id);
    }
}
