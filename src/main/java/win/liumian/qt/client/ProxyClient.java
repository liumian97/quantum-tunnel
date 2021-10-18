package win.liumian.qt.client;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import win.liumian.qt.client.handler.ProxyClientHandler;
import win.liumian.qt.common.QuantumMessageDecoder;
import win.liumian.qt.common.QuantumMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author liumian  2021/9/26 11:40
 */
@Slf4j
public class ProxyClient {

    public static void main(String[] args) throws ParseException, InterruptedException {
        Options options = new Options();
        options.addOption("help", false, "Help");
        options.addOption("proxy_server_host", true, "内网穿透-代理服务器地址");
        options.addOption("proxy_server_port", true, "内网穿透-代理服务端口");
        options.addOption("network_id", true, "分配的网络id");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {

            String serverHost = cmd.getOptionValue("proxy_server_host");
            if (serverHost == null) {
                System.out.println("proxy_server_host cannot be null");
                return;
            }
            String serverPort = cmd.getOptionValue("proxy_server_port");
            if (serverPort == null) {
                System.out.println("proxy_server_port cannot be null");
                return;
            }
            String networkId = cmd.getOptionValue("network_id");
            if (networkId == null) {
                System.out.println("proxy_server_port cannot be null");
                return;
            }
            ProxyClient proxyClient = new ProxyClient();
            while (true) {
                try {
                    Channel channel = proxyClient.connect(serverHost, Integer.parseInt(serverPort),networkId);
                    channel.closeFuture().sync();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.info("内网穿透代理服务器已断开，重试中...");
                Thread.sleep(10 * 1000);
            }
        }

    }

    private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();


    /**
     * @param host
     * @param port
     * @throws InterruptedException
     */
    public Channel connect(String host, int port,String networkId) throws InterruptedException, IOException {

        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                ProxyClientHandler proxyClientHandler = new ProxyClientHandler(networkId);
                ch.pipeline().addLast(
                        new LengthFieldBasedFrameDecoder(65535, 0, 4, 0, 4),
                        new QuantumMessageDecoder(),
                        new QuantumMessageEncoder(),
                        proxyClientHandler);
            }
        });
        return b.connect(host, port).addListener(future -> log.info("内网穿透客户端启动成功...")).sync().channel();
    }
}
