package top.liumian.qt.channel;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author liumian  2021/10/14 23:23
 */
public class ChannelMap {
    public static final Map<String, Channel> PROXY_CHANNEL_MAP = new ConcurrentHashMap<>();
    public static Map<String, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

}
