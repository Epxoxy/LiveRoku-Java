package danmaku;

import java.nio.charset.Charset;

import danmaku.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PacketEncoder extends MessageToByteEncoder<Packet>{
	private Charset utf8set = Charset.forName("utf-8");

	@Override
	protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
		byte[] payload = packet.payload.getBytes(utf8set);
		packet.length = payload.length + Packet.HeaderSize;
		packet.headerLength = Packet.HeaderSize;
		out.writeInt(packet.length);
		out.writeShort(packet.headerLength);
		out.writeShort(packet.devType);
		out.writeInt(packet.packetType);
		out.writeInt(packet.device);
		if (payload.length > 0)
			out.writeBytes(payload);
	}

}
