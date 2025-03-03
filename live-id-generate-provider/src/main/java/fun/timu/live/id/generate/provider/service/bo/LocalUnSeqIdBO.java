package fun.timu.live.id.generate.provider.service.bo;

import lombok.Data;

import java.util.concurrent.ConcurrentLinkedQueue;

@Data
public class LocalUnSeqIdBO {
    private int id;
    /**
     * 提前将无序的id存放在这条队列中
     */
    private ConcurrentLinkedQueue<Long> idQueue;
    /**
     * 当前id段的开始值
     */
    private Long currentStart;
    /**
     * 当前id段的结束值
     */
    private Long nextThreshold;

}
