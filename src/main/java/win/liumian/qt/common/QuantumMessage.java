package win.liumian.qt.common;

import lombok.Data;

/**
 * @author liumian  2021/9/25 10:50
 */
@Data
public class QuantumMessage {

    /**
     * 客户端id
     */
    private String clientId;

    /**
     * 代理通道
     */
    private String channelId;

    /**
     * 代理地址
     */
    private String proxyHost;

    /**
     * 代理端口
     */
    private int proxyPort;

    /**
     * 消息类型
     */
    private QuantumMessageType messageType;

    /**
     * 原始数据
     */
    private byte[] data;

}
