package fun.timu.live.id.generate.provider.service;

public interface IdGenerateService {

    /**
     * 获取有序id
     *
     * @param id
     * @return
     */
    Long getSeqId(Integer id);

    /**
     * 获取无序id
     *
     * @param id
     * @return
     */
    Long getUnSeqId(Integer id);
}
