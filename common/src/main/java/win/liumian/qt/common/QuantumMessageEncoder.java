package win.liumian.qt.common;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import win.liumian.qt.common.proto.QuantumMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author liumian  2021/9/25 11:12
 */
public class QuantumMessageEncoder extends MessageToByteEncoder<QuantumMessage.Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, QuantumMessage.Message msg, ByteBuf out) throws Exception {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {

            String str = JSONObject.toJSONString(msg);
            dataOutputStream.write(str.getBytes(StandardCharsets.UTF_8));
            byte[] data = byteArrayOutputStream.toByteArray();
            out.writeInt(data.length);
            out.writeBytes(data);
        }
    }

}
