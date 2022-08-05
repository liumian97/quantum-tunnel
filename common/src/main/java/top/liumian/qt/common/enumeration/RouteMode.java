package top.liumian.qt.common.enumeration;

/**
 * 路由模式
 *
 * @author liumian  2021/10/23 09:51
 */
public enum RouteMode {

    /**
     * 协议路由，对原有协议有侵入
     * 将目标被代理网络id、目标网络地址、目标端口放在协议头中
     * 如http header中
     */
    PROTOCOL_ROUTE("protocol_route", "协议路由"),

    /**
     * 端口路由
     * 将目标被代理网络id、目标网络地址、目标端口与服务器端口绑定，对原有协议无侵入
     */
    PORT_ROUTE("port_route", "端口路由");

    public final String value;

    public final String desc;

    RouteMode(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
