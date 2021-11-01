package win.liumian.qt.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import win.liumian.qt.client.tcp.TcpClient;
import win.liumian.qt.client.udp.UdpClient;

import java.io.IOException;

/**
 * @author liumian  2021/9/26 11:40
 */
@Slf4j
@SpringBootApplication
public class ProxyClient {


    public static void main(String[] args) throws ParseException, InterruptedException {
        Options options = new Options();
        options.addOption("help", false, "Help");
        options.addOption("proxy_server_host", true, "内网穿透-代理服务器地址");
        options.addOption("proxy_server_port", true, "内网穿透-代理服务端口");
        options.addOption("network_id", true, "分配的网络id");
        options.addOption("target_server_host", true, "目标服务器host，默认不限制");
        options.addOption("target_server_port", true, "目标服务器port，默认不限制");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {

            String proxyServerHost = cmd.getOptionValue("proxy_server_host");
            if (proxyServerHost == null) {
                log.error("proxy_server_host cannot be null");
                return;
            }
            String proxyServerPort = cmd.getOptionValue("proxy_server_port");
            if (proxyServerPort == null) {
                log.error("proxy_server_port cannot be null");
                return;
            }
            String networkId = cmd.getOptionValue("network_id");
            if (networkId == null) {
                log.error("proxy_server_port cannot be null");
                return;
            }
            String targetServerHost = cmd.getOptionValue("target_server_host");
            String targetServerPort = cmd.getOptionValue("target_server_port");
            log.info("启动参数：\nproxy_server_host：{}\nproxy_server_port：{}\nnetwork_id：{} \ntarget_server_host：{}\ntarget_server_port：{}",
                    proxyServerHost, proxyServerPort, networkId, targetServerHost, targetServerPort);
            TcpClient tcpClient = new TcpClient(proxyServerHost, proxyServerPort, networkId, targetServerHost, targetServerPort);
            new Thread(() -> {
                UdpClient client1 = new UdpClient(networkId, proxyServerHost, 9999, 8881);
                client1.run();
            }).start();

//            new Thread(() -> {
//                UdpClient client1 = new UdpClient(networkId, proxyServerHost, 10000, 8881);
//                client1.run();
//            }).start();
            while (true) {
                try {
                    Channel channel = tcpClient.connect();
                    channel.closeFuture().sync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("内网穿透代理服务器已断开，重试中...");
                Thread.sleep(10 * 1000);
            }
        }

    }
}
