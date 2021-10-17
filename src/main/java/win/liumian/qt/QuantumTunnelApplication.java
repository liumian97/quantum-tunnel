package win.liumian.qt;

import win.liumian.qt.server.ProxyServer;
import win.liumian.qt.server.UserServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
public class QuantumTunnelApplication {


    private static Executor executor = Executors.newFixedThreadPool(2);


    public static void main(String[] args) {
        SpringApplication.run(QuantumTunnelApplication.class, args);


        executor.execute(() -> {
            //启动用户服务端
            UserServer nettyServer = new UserServer();
            nettyServer.start(new InetSocketAddress("127.0.0.1", 8090));
        });

        executor.execute(() -> {
            //启动代理服务端
            ProxyServer proxyServer = new ProxyServer();
            proxyServer.start(new InetSocketAddress("127.0.0.1", 9090));
        });
    }

}
