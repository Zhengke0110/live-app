package fun.timu.live.id.generate.interfaces;
public interface IdGenerateRpc {

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
