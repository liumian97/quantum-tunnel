package win.liumian.qt.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import win.liumian.qt.client.handler.ProxyClientHandler;
import win.liumian.qt.common.QuantumMessageDecoder;
import win.liumian.qt.common.QuantumMessageEncoder;

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

    private final String proxyServerHost;
    private final String proxyServerPort;
    private final String networkId;
    private final String targetServerHost;
    private final String targetServerPort;


    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    public ProxyClient(String proxyServerHost, String proxyServerPort, String networkId, String targetServerHost, String targetServerPort) {
        this.proxyServerHost = proxyServerHost;
        this.proxyServerPort = proxyServerPort;
        this.networkId = networkId;
        this.targetServerHost = targetServerHost;
        this.targetServerPort = targetServerPort;
    }

    /**
     * @throws InterruptedException
     */
    public Channel connect() throws InterruptedException, IOException {

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ProxyClientHandler proxyClientHandler = new ProxyClientHandler(networkId, targetServerHost, targetServerPort);
                ch.pipeline().addLast(
                        new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4),
                        new QuantumMessageDecoder(),
                        new QuantumMessageEncoder(),
                        new IdleStateHandler(360, 300, 0),
                        proxyClientHandler);
            }
        });
        return b.connect(proxyServerHost, Integer.parseInt(proxyServerPort)).addListener(future -> log.info("内网穿透客户端启动成功...")).sync().channel();
    }
}
