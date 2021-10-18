package win.liumian.qt;

import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import win.liumian.qt.server.ProxyServer;
import win.liumian.qt.server.UserServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SpringBootApplication
public class QuantumTunnelApplication {


    private static Executor executor = Executors.newFixedThreadPool(2);


    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("help", false, "Help");
        options.addOption("proxy_server_port", true, "内网穿透-代理服务端口");
        options.addOption("user_server_port", true, "内网穿透-用户服务端口");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {
            SpringApplication.run(QuantumTunnelApplication.class, args);

            String proxyServerPort = cmd.getOptionValue("proxy_server_port");
            if (proxyServerPort == null) {
                System.out.println("proxy_server_host cannot be null");
                return;
            }
            String userServerPort = cmd.getOptionValue("user_server_port");
            if (userServerPort == null) {
                System.out.println("proxy_server_port cannot be null");
                return;
            }

            executor.execute(() -> {
                //启动代理服务端
                ProxyServer proxyServer = new ProxyServer();
                proxyServer.start(new InetSocketAddress("127.0.0.1", Integer.parseInt(proxyServerPort)));
            });

            executor.execute(() -> {
                //启动用户服务端
                UserServer nettyServer = new UserServer();
                nettyServer.start(new InetSocketAddress("127.0.0.1", Integer.parseInt(userServerPort)));
            });

        }


    }

}
