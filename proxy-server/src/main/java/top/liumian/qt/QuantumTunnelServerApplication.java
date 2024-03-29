package top.liumian.qt;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import top.liumian.qt.common.enumeration.RouteMode;
import top.liumian.qt.common.util.BannerUtil;
import top.liumian.qt.server.ProxyServer;
import top.liumian.qt.server.UserServer;

import static top.liumian.qt.common.util.ExecutorUtil.SERVER_CLIENT_EXECUTOR;
import static top.liumian.qt.common.util.ExecutorUtil.USER_SERVER_EXECUTOR;

/**
 * @author liumian 2022/06/01 11:14 下午
 */
@Slf4j
@PropertySource("classpath:gitBuildInfo.properties")
@SpringBootApplication
public class QuantumTunnelServerApplication {


    public static void main(String[] args) throws ParseException {


        Options options = new Options();
        options.addOption("help", false, "Help");
        options.addOption("proxy_server_port", true, "内网穿透-代理服务端口");
        options.addOption("user_server_port", true, "内网穿透-用户服务端口");
        options.addOption("route_mode", true, "路由模式：protocol_route 或者 port_route，默认protocol_route");
        options.addOption("network_id", true, "网络id，port_route需要指定");
        options.addOption("target_server_host", true, "目标host，port_route需要指定");
        options.addOption("target_server_port", true, "目标port，port_route需要指定");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {
            SpringApplication.run(QuantumTunnelServerApplication.class, args);
            BannerUtil.printGitBuildInfo();
            String proxyServerPort = cmd.getOptionValue("proxy_server_port");
            if (proxyServerPort == null) {
                log.error("proxy_server_host cannot be null");
                return;
            }
            String userServerPort = cmd.getOptionValue("user_server_port");
            if (userServerPort == null) {
                log.error("proxy_server_port cannot be null");
                return;
            }

            String routeMode = cmd.getOptionValue("route_mode", RouteMode.PROTOCOL_ROUTE.value);


            String networkId = cmd.getOptionValue("network_id");
            if (RouteMode.PORT_ROUTE.value.equals(routeMode) && networkId == null) {
                log.error("network_id cannot be null");
                return;
            }

            String targetHost = cmd.getOptionValue("target_server_host");
            if (RouteMode.PORT_ROUTE.value.equals(routeMode) && targetHost == null) {
                log.error("target_server_host cannot be null");
                return;
            }

            String targetPort = cmd.getOptionValue("target_server_port");
            if (RouteMode.PORT_ROUTE.value.equals(routeMode) && targetPort == null) {
                log.error("target_server_port cannot be null");
                return;
            }
            SERVER_CLIENT_EXECUTOR.execute(() -> {
                //启动代理服务端
                ProxyServer proxyServer = new ProxyServer(proxyServerPort);
                proxyServer.start();
            });

            USER_SERVER_EXECUTOR.execute(() -> {
                //启动用户服务端
                UserServer nettyServer = new UserServer(userServerPort, routeMode, networkId, targetHost, targetPort);
                nettyServer.start();
            });
        }


    }


}
