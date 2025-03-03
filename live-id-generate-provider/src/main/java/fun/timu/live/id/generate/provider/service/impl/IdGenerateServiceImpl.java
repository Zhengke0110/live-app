package fun.timu.live.id.generate.provider.service.impl;

import fun.timu.live.id.generate.provider.dao.mapper.IdGenerateMapper;
import fun.timu.live.id.generate.provider.dao.po.IdGeneratePO;
import fun.timu.live.id.generate.provider.service.IdGenerateService;
import fun.timu.live.id.generate.provider.service.bo.LocalSeqIdBO;
import fun.timu.live.id.generate.provider.service.bo.LocalUnSeqIdBO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IdGenerateServiceImpl implements IdGenerateService, InitializingBean {
    private final IdGenerateMapper idGenerateMapper;

    private static final Logger LOGGER = LoggerFactory.getLogger(IdGenerateServiceImpl.class);
    private static Map<Integer, LocalSeqIdBO> localSeqIdBOMap = new ConcurrentHashMap<>();
    private static Map<Integer, LocalUnSeqIdBO> localUnSeqIdBOMap = new ConcurrentHashMap<>();
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(8, 16, 3, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("id-generate-thread-" + ThreadLocalRandom.current().nextInt(1000));
            return thread;
        }
    });
    private static final float UPDATE_RATE = 0.75f;
    private static final int SEQ_ID = 1;
    private static Map<Integer, Semaphore> semaphoreMap = new ConcurrentHashMap<>();

    public IdGenerateServiceImpl(IdGenerateMapper idGenerateMapper) {
        this.idGenerateMapper = idGenerateMapper;
    }

    /**
     * 获取序列ID
     *
     * @param id 用于获取序列ID的键，不能为空
     * @return 返回序列ID，如果输入参数为空或对应参数无序列ID信息，则返回null
     * <p>
     * 此方法用于根据给定的键获取一个唯一的序列ID它首先检查输入的键是否为空，
     * 如果为空，则记录错误日志并返回null接着，它尝试从一个映射中获取与键关联的序列ID对象，
     * 如果找不到，则同样记录错误日志并返回null在成功获取序列ID对象后，它会刷新该对象以确保序列的正确性，
     * 然后尝试获取下一个可用的序列ID如果当前序列ID超过了预定义的阈值，
     * 表明序列ID已用尽，此时记录错误日志并返回null否则，返回当前序列ID
     */
    @Override
    public Long getSeqId(Integer id) {
        // 检查输入参数是否为空
        if (id == null) {
            LOGGER.error("[getSeqId] id is error,id is {}", id);
            return null;
        }
        // 从映射中获取序列ID对象
        LocalSeqIdBO localSeqIdBO = localSeqIdBOMap.get(id);
        // 检查序列ID对象是否存在
        if (localSeqIdBO == null) {
            LOGGER.error("[getSeqId] localSeqIdBO is null,id is {}", id);
            return null;
        }
        // 刷新序列ID对象
        this.refreshLocalSeqId(localSeqIdBO);
        // 获取当前序列ID
        long returnId = localSeqIdBO.getCurrentNum().incrementAndGet();
        // 检查序列ID是否超过阈值
        if (returnId > localSeqIdBO.getNextThreshold()) {
            //同步去刷新
            LOGGER.error("[getSeqId] id is over limit,id is {}", id);
            return null;
        }
        // 返回当前序列ID
        return returnId;
    }

    /**
     * 获取不连续序列ID
     * 该方法用于从本地不连续序列ID缓存中获取一个ID
     * 如果给定的ID为空或在缓存中找不到对应的ID队列，或者ID队列为空，则返回null
     *
     * @param id 用于标识特定序列的ID
     * @return 从序列中获取的不连续ID，如果无法获取则返回null
     */
    @Override
    public Long getUnSeqId(Integer id) {
        // 检查输入的ID是否为空，如果为空则记录错误日志并返回null
        if (id == null) {
            LOGGER.error("[getSeqId] id is error,id is {}", id);
            return null;
        }

        // 从映射中获取与给定ID关联的本地不连续序列ID对象
        LocalUnSeqIdBO localUnSeqIdBO = localUnSeqIdBOMap.get(id);

        // 检查获取到的对象是否为空，如果为空则记录错误日志并返回null
        if (localUnSeqIdBO == null) {
            LOGGER.error("[getUnSeqId] localUnSeqIdBO is null,id is {}", id);
            return null;
        }

        // 从本地不连续序列ID对象的队列中获取一个ID
        Long returnId = localUnSeqIdBO.getIdQueue().poll();

        // 检查获取到的ID是否为空，如果为空则记录错误日志并返回null
        if (returnId == null) {
            LOGGER.error("[getUnSeqId] returnId is null,id is {}", id);
            return null;
        }

        // 调用方法刷新本地不连续序列ID对象
        this.refreshLocalUnSeqId(localUnSeqIdBO);

        // 返回获取到的ID
        return returnId;
    }

    /**
     * 在所有属性设置完成后执行此方法
     * 主要用于服务启动时，初始化一些必要的资源或完成一些必须的设置
     *
     * @throws Exception 如果属性设置过程中有异常，将会被抛出
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        // 从数据库中获取所有ID生成记录
        List<IdGeneratePO> idGeneratePOList = idGenerateMapper.selectAll();
        for (IdGeneratePO idGeneratePO : idGeneratePOList) {
            // 记录日志，表示服务启动时正在抢占新的ID段
            LOGGER.info("服务刚启动，抢占新的id段");
            // 尝试更新MySQL记录，以获取新的ID段
            tryUpdateMySQLRecord(idGeneratePO);
            // 在信号量映射中为当前ID生成记录添加一个信号量，初始许可数为1
            semaphoreMap.put(idGeneratePO.getId(), new Semaphore(1));
        }
    }

    /**
     * 刷新本地序列ID，以保持ID生成的效率和唯一性
     * 本方法主要目的是为了在本地序列ID使用达到一定阈值时，异步更新本地ID段，减少数据库访问频率
     *
     * @param localSeqIdBO 包含本地序列ID信息的业务对象，包括当前使用的ID段起始值、下一个阈值、当前已使用的ID数量等
     */
    private void refreshLocalSeqId(LocalSeqIdBO localSeqIdBO) {
        // 计算当前ID段的步长，用于判断是否需要更新ID段
        long step = localSeqIdBO.getNextThreshold() - localSeqIdBO.getCurrentStart();
        // 当前已使用的ID数量超过当前ID段起始值加上步长乘以更新率时，触发ID段更新
        if (localSeqIdBO.getCurrentNum().get() - localSeqIdBO.getCurrentStart() > step * UPDATE_RATE) {
            // 获取对应的信号量，用于控制同时更新的线程数量
            Semaphore semaphore = semaphoreMap.get(localSeqIdBO.getId());
            // 如果信号量为null，则记录错误日志并返回
            if (semaphore == null) {
                LOGGER.error("semaphore is null,id is {}", localSeqIdBO.getId());
                return;
            }
            // 尝试获取信号量，如果成功则开始同步操作
            boolean acquireStatus = semaphore.tryAcquire();
            if (acquireStatus) {
                LOGGER.info("开始尝试进行本地id段的同步操作");
                // 异步进行同步id段操作
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 从数据库中获取ID生成记录
                            IdGeneratePO idGeneratePO = idGenerateMapper.selectById(localSeqIdBO.getId());
                            // 尝试更新MySQL记录，以获取新的ID段
                            tryUpdateMySQLRecord(idGeneratePO);
                        } catch (Exception e) {
                            LOGGER.error("[refreshLocalSeqId] error is ", e);
                        } finally {
                            // 释放信号量，确保其他等待的线程可以进行同步操作
                            semaphoreMap.get(localSeqIdBO.getId()).release();
                            LOGGER.info("本地有序id段同步完成,id is {}", localSeqIdBO.getId());
                        }
                    }
                });
            }
        }
    }

    /**
     * 刷新本地无序ID段
     * 当剩余可用ID数量低于预设阈值时，尝试获取信号量以更新本地ID段
     * 此方法确保在多线程环境下高效且安全地更新ID段
     *
     * @param localUnSeqIdBO 包含本地无序ID信息的业务对象
     */
    private void refreshLocalUnSeqId(LocalUnSeqIdBO localUnSeqIdBO) {
        // 获取当前ID段的起始值和下一个阈值
        long begin = localUnSeqIdBO.getCurrentStart();
        long end = localUnSeqIdBO.getNextThreshold();
        // 计算当前剩余的ID数量
        long remainSize = localUnSeqIdBO.getIdQueue().size();
        // 如果使用剩余空间不足25%，则进行刷新
        if ((end - begin) * 0.25 > remainSize) {
            // 获取对应的信号量对象
            Semaphore semaphore = semaphoreMap.get(localUnSeqIdBO.getId());
            // 如果信号量对象为空，则记录错误并返回
            if (semaphore == null) {
                LOGGER.error("semaphore is null,id is {}", localUnSeqIdBO.getId());
                return;
            }
            // 尝试获取信号量
            boolean acquireStatus = semaphore.tryAcquire();
            // 如果成功获取信号量，则提交任务到线程池以更新ID段
            if (acquireStatus) {
                threadPoolExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 从数据库中获取ID生成信息
                            IdGeneratePO idGeneratePO = idGenerateMapper.selectById(localUnSeqIdBO.getId());
                            // 尝试更新MySQL记录
                            tryUpdateMySQLRecord(idGeneratePO);
                        } catch (Exception e) {
                            // 记录异常信息
                            LOGGER.error("[refreshLocalUnSeqId] error is ", e);
                        } finally {
                            // 释放信号量
                            semaphoreMap.get(localUnSeqIdBO.getId()).release();
                            // 记录ID段同步完成的信息
                            LOGGER.info("本地无序id段同步完成，id is {}", localUnSeqIdBO.getId());
                        }
                    }
                });
            }
        }
    }


    /**
     * 尝试更新MySQL数据库中的记录
     * 该方法首先尝试更新数据库中的ID计数和版本信息，如果更新成功，则处理本地ID业务逻辑
     * 如果更新失败，则重试更新操作，重试次数限定为3次
     * 如果多次更新均失败，则抛出运行时异常，表明表ID段占用失败
     *
     * @param idGeneratePO 包含ID和版本信息的实体对象，用于数据库记录的更新
     * @throws RuntimeException 如果多次尝试更新记录失败，抛出此异常
     */
    private void tryUpdateMySQLRecord(IdGeneratePO idGeneratePO) {
        // 尝试更新数据库中的ID计数和版本信息
        int updateResult = idGenerateMapper.updateNewIdCountAndVersion(idGeneratePO.getId(), idGeneratePO.getVersion());
        // 如果更新成功
        if (updateResult > 0) {
            // 处理本地ID业务逻辑
            localIdBOHandler(idGeneratePO);
            return;
        }
        // 重试进行更新
        for (int i = 0; i < 3; i++) {
            // 重新获取当前ID的最新信息
            idGeneratePO = idGenerateMapper.selectById(idGeneratePO.getId());
            // 再次尝试更新ID计数和版本信息
            updateResult = idGenerateMapper.updateNewIdCountAndVersion(idGeneratePO.getId(), idGeneratePO.getVersion());
            // 如果更新成功
            if (updateResult > 0) {
                // 处理本地ID业务逻辑
                localIdBOHandler(idGeneratePO);
                return;
            }
        }
        // 如果多次更新均失败，抛出异常
        throw new RuntimeException("表id段占用失败，竞争过于激烈，id is " + idGeneratePO.getId());
    }

    /**
     * 处理本地ID生成的业务逻辑
     * 根据传入的IdGeneratePO对象中的配置，创建相应的本地ID处理对象
     * 如果是顺序ID，则创建LocalSeqIdBO对象；如果是非顺序ID，则创建LocalUnSeqIdBO对象
     *
     * @param idGeneratePO 包含ID生成策略和当前ID段信息的PO对象
     */
    private void localIdBOHandler(IdGeneratePO idGeneratePO) {
        // 获取当前ID段的起始值
        long currentStart = idGeneratePO.getCurrentStart();
        // 获取当前ID段的结束阈值
        long nextThreshold = idGeneratePO.getNextThreshold();
        // 初始化当前数值为ID段的起始值
        long currentNum = currentStart;

        // 判断是否为顺序ID
        if (idGeneratePO.getIsSeq() == SEQ_ID) {
            // 创建LocalSeqIdBO对象用于处理顺序ID的逻辑
            LocalSeqIdBO localSeqIdBO = new LocalSeqIdBO();
            // 使用AtomicLong来保证并发环境下ID的自增操作是线程安全的
            AtomicLong atomicLong = new AtomicLong(currentNum);
            // 设置LocalSeqIdBO对象的属性值
            localSeqIdBO.setId(idGeneratePO.getId());
            localSeqIdBO.setCurrentNum(atomicLong);
            localSeqIdBO.setCurrentStart(currentStart);
            localSeqIdBO.setNextThreshold(nextThreshold);
            // 将LocalSeqIdBO对象放入map中以便后续使用
            localSeqIdBOMap.put(localSeqIdBO.getId(), localSeqIdBO);
        } else {
            // 创建LocalUnSeqIdBO对象用于处理非顺序ID的逻辑
            LocalUnSeqIdBO localUnSeqIdBO = new LocalUnSeqIdBO();
            // 设置LocalUnSeqIdBO对象的属性值
            localUnSeqIdBO.setCurrentStart(currentStart);
            localUnSeqIdBO.setNextThreshold(nextThreshold);
            localUnSeqIdBO.setId(idGeneratePO.getId());
            // 初始化ID列表，用于存储当前ID段的所有ID
            long begin = localUnSeqIdBO.getCurrentStart();
            long end = localUnSeqIdBO.getNextThreshold();
            List<Long> idList = new ArrayList<>();
            // 将当前ID段的所有ID添加到列表中
            for (long i = begin; i < end; i++) {
                idList.add(i);
            }
            // 将本地ID段提前打乱，然后放入到队列中
            Collections.shuffle(idList);
            ConcurrentLinkedQueue<Long> idQueue = new ConcurrentLinkedQueue<>();
            idQueue.addAll(idList);
            // 将打乱后的ID队列设置到LocalUnSeqIdBO对象中
            localUnSeqIdBO.setIdQueue(idQueue);
            // 将LocalUnSeqIdBO对象放入map中以便后续使用
            localUnSeqIdBOMap.put(localUnSeqIdBO.getId(), localUnSeqIdBO);
        }
    }

}
