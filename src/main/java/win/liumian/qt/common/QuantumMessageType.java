package win.liumian.qt.common;

/**
 * @author liumian  2021/9/25 11:12
 */
public enum QuantumMessageType {

    /**
     * 注册请求
     */
    REGISTER(1),
    /**
     * 注册结果
     */
    REGISTER_RESULT(2),
    /**
     * 连接成功
     */
    CONNECTED(3),
    /**
     * 断开连接
     */
    DISCONNECTED(4),
    /**
     * 传输数据
     */
    DATA(5),
    /**
     * 保持链接
     */
    KEEPALIVE(6);

    private int code;

    QuantumMessageType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static QuantumMessageType valueOf(int code) {
        for (QuantumMessageType item : QuantumMessageType.values()) {
            if (item.code == code) {
                return item;
            }
        }
        throw new RuntimeException("NatxMessageType code error: " + code);
    }

}
