package top.liumian.qt.common.enumeration;

/**
 * @author liumian  2021/10/23 09:51
 */
public enum RouteMode {

    PROTOCOL_ROUTE("protocol_route", "协议路由"),
    PORT_ROUTE("port_route", "端口路由");

    public final String value;

    public final String desc;

    RouteMode(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
