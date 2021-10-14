package win.liumian.qt.server.channel;

import win.liumian.qt.server.handler.UserServerHandlerV2;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;


/**
 * @author liumian  2021/9/18 16:24
 */
public class QuantumServerChannelInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        //添加编解码
        ChannelPipeline pipeline = socketChannel.pipeline();

        // 请求解码器
        pipeline.addLast("http-decoder", new HttpRequestDecoder());
        // 将HTTP消息的多个部分合成一条完整的HTTP消息
        pipeline.addLast("http-aggregator", new HttpObjectAggregator(65535));

        pipeline.addLast(new ByteArrayEncoder(), new UserServerHandlerV2());

    }
}
