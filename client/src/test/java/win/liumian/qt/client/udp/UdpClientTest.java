package win.liumian.qt.client.udp;


import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author liumian  2021/10/24 15:49
 */
public class UdpClientTest {

    private String networkId = "udpNetworkId";

    private String client1Host = "127.0.0.1";

    private int client1Port = 8888;

    private String client2Host = "127.0.0.1";

    private int client2Port = 9999;

    private Executor executor = Executors.newFixedThreadPool(2);

    public String udpServer = "127.0.0.1";

    @Test
    public void client1Test() throws InterruptedException {
        UdpClient client1 = new UdpClient(networkId, client2Host, client2Port);
        UdpClient client2 = new UdpClient(networkId, client1Host, client1Port);

        executor.execute(client1::run);
        executor.execute(client2::run);

        Thread.sleep(20000);
        System.out.println("执行结束");
    }


    @Test
    public void client2ServerTest() throws InterruptedException {
        UdpClient client1 = new UdpClient(networkId, udpServer, 9999);
        UdpClient client2 = new UdpClient(networkId, udpServer, 9999);
        executor.execute(client1::run);
        executor.execute(client2::run);
        Thread.sleep(30000);
    }


    @Test
    public void udpClient() {
        UdpClient client1 = new UdpClient(networkId, udpServer, 10000);
        client1.run();
    }

}
