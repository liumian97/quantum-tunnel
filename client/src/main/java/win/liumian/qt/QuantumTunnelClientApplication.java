package win.liumian.qt;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import win.liumian.qt.client.ProxyClient;
import win.liumian.qt.common.util.BannerUtil;

import java.io.IOException;

/**
 * @author liumian
 * @date 2021/11/6 12:45 下午
 */
@Slf4j
@SpringBootApplication
public class QuantumTunnelClientApplication {

    public static void main(String[] args) throws ParseException, InterruptedException, IOException {

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

            SpringApplication.run(QuantumTunnelClientApplication.class, args);
            BannerUtil.printGitBuildInfo();

            String targetServerHost = cmd.getOptionValue("target_server_host");
            String targetServerPort = cmd.getOptionValue("target_server_port");
            log.info("启动参数：\nproxy_server_host：{}\nproxy_server_port：{}\nnetwork_id：{} \ntarget_server_host：{}\ntarget_server_port：{}",
                    proxyServerHost, proxyServerPort, networkId, targetServerHost, targetServerPort);
            ProxyClient proxyClient = new ProxyClient(proxyServerHost, proxyServerPort, networkId, targetServerHost, targetServerPort);
            while (true) {
                try {
                    Channel channel = proxyClient.connect();
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
