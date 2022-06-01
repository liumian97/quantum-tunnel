package top.liumian.qt.common.util;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池工具类
 *
 * @author liumian  2022/6/1 23:21
 */
public class ExecutorUtil {

    /**
     * 处理ProxyClient请求的线程池
     */
    public final static Executor SERVER_CLIENT_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new DefaultThreadFactory("server-client"));

    /**
     * 处理user请求的线程池
     */
    public final static Executor USER_SERVER_EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new DefaultThreadFactory("user-server"));


}
