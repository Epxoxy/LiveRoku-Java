package danmaku.packet;

public class Packet {
	public static final short HeaderSize = 16;
    public int length;
    public short headerLength;
    public short devType;
    public int packetType;
    public int device;
    public String payload;
    public int payloadLength;
    
    @Override
    public String toString() {
    	return "length["+length+"],header["+headerLength+"],devType:"+devType+",device:"+device+",msgType:"+packetType+"\\n\\tpayload["+payloadLength+"]:"+payload;
    }
}
