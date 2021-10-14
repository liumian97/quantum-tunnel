package win.liumian.qt.common;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;

/**
 * @author liumian  2021/9/25 11:12
 */
public class QuantumMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List out) throws Exception {

        if (msg.isReadable()) {
            byte[] bytes = ByteBufUtil.getBytes(msg);
            String s = new String(bytes);
            QuantumMessage quantumMessage = JSONObject.parseObject(s, QuantumMessage.class);
            out.add(quantumMessage);
        }
    }

}
