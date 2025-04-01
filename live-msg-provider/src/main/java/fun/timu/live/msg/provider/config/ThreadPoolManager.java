package fun.timu.live.msg.provider.config;

import java.util.concurrent.*;

public class ThreadPoolManager {
    public static ThreadPoolExecutor commonAsyncPool = new ThreadPoolExecutor(2, 8, 3, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000), new ThreadFactory() {
        /**
         * 重写新建线程的方法
         * 该方法用于为线程池创建一个新的线程
         *
         * @param r 线程需要执行的任务，不能为空
         * @return 返回新创建的线程
         */
        @Override
        public Thread newThread(Runnable r) {
            // 创建一个新的线程实例，传入给定的任务
            Thread newThread = new Thread(r);
            // 为新线程设置一个唯一的名字，格式为"commonAsyncPool - 随机数字"
            // 这里使用ThreadLocalRandom生成一个随机数作为线程名字的一部分，以减少线程间的冲突
            newThread.setName("commonAsyncPool - " + ThreadLocalRandom.current().nextInt(10000));
            // 返回新创建的线程
            return newThread;
        }
    });
}
