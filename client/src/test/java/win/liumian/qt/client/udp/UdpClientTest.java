package win.liumian.qt.client.udp;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author liumian  2021/10/24 15:49
 */
public class UdpClientTest {


    private String client1Host = "127.0.0.1";

    private int client1Port = 8888;

    private String client2Host = "127.0.0.1";

    private int client2Port = 9999;

    private Executor executor = Executors.newFixedThreadPool(2);

    @Test
    public void client1Test() throws InterruptedException {
        UdpClient client1 = new UdpClient(client2Host, client2Port, client1Port);
        UdpClient client2 = new UdpClient(client1Host, client1Port, client2Port);

        executor.execute(client1::run);
        executor.execute(client2::run);

        Thread.sleep(10000);
        System.out.println("执行结束");
    }


    @Test
    public void client2Test() {

    }


}
