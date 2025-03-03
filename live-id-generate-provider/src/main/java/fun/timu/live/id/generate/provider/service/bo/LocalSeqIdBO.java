package fun.timu.live.id.generate.provider.service.bo;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class LocalSeqIdBO {
    private int id;
    /**
     * 在内存中记录的当前有序id的值
     */
    private AtomicLong currentNum;

    /**
     * 当前id段的开始值
     */
    private Long currentStart;
    /**
     * 当前id段的结束值
     */
    private Long nextThreshold;

}
