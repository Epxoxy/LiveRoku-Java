package danmaku;

import java.nio.charset.Charset;

import danmaku.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class PacketDecoder extends LengthFieldBasedFrameDecoder {
	private Charset utf8set = Charset.forName("utf-8");

	public PacketDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
		super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		if (in == null || in.readableBytes() < Packet.HeaderSize) {
			System.out.println("in.readableBytes() < HeaderSize");
			return null;
		}
		in.markReaderIndex();

		int packetLength = in.readInt();
		int payloadLength = packetLength - 4;
		if (packetLength < Packet.HeaderSize || in.readableBytes() < payloadLength) {
			System.out.println("in.resetReaderIndex() " + in.readableBytes() + "/" + payloadLength);
			in.resetReaderIndex();
			return null;
		}
		Packet packet = new Packet();
		packet.length = packetLength;
		packet.headerLength = in.readShort();
		packet.devType = in.readShort();
		packet.packetType = in.readInt();
		packet.device = in.readInt();
		packet.payloadLength = packetLength - Packet.HeaderSize;
		byte[] payload = null;
		switch (packet.packetType) {
		case 1: // Hot update
		case 2: // Hot update
		case 3: { // Hot update
			int hot = in.readInt();
			packet.payload = String.valueOf(hot);
		} break;
		case 5: {// danmaku data
			payload = new byte[packet.payloadLength];
			in.readBytes(payload, 0, payload.length);
			packet.payload = new String(payload, utf8set);
		} break;
		case 4: // unknow
		case 6: // newScrollMessage
		case 7:
		case 8: // hand shake ok.
		case 16:
		default: {
			payload = new byte[packet.payloadLength];
			in.readBytes(payload, 0, payload.length);
			packet.payload = new String(payload, utf8set);
		}break;
		}
		return packet;
	}

}
