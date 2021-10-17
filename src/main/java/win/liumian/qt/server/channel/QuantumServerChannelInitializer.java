package win.liumian.qt.server.channel;

import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import win.liumian.qt.server.handler.UserServerHandler;
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
        pipeline.addLast(new ByteArrayDecoder(), new ByteArrayEncoder(), new UserServerHandler());

    }
}
