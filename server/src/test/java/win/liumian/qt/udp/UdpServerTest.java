package win.liumian.qt.udp;

import org.junit.jupiter.api.Test;

/**
 * @author liumian  2021/10/24 17:29
 */
public class UdpServerTest {


    @Test
    public void udpServerTest() {
        System.out.println("准备启动服务器");
        new UdpServer(10000).run();
        System.out.println("服务器停止");
    }


}
