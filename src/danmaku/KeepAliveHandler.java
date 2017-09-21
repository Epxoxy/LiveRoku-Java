package danmaku;

import java.util.Random;

import danmaku.packet.Packet;
import danmaku.packet.PacketMsgType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class KeepAliveHandler extends ChannelInboundHandlerAdapter{
	private final int channelId;
	private int retryTimes = 3;
	private boolean isActive = false;
	private Thread heartbeat = null;
	
	public KeepAliveHandler(int channelId) {
		this.channelId = channelId;
	}
	
	private Packet simplePacket(int packetType, String payload) {
        Packet packet = new Packet();
        packet.devType = 1;
        packet.packetType = packetType;
        packet.device = 1;
        packet.payload = payload;
        return packet;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.isActive = true;
        //Handshake
        System.out.println("Invoke KeepAliveHandler.channelActive(ctx)");
        long tmpUid = (long) (1e14 + 2e14 * new Random ().nextDouble ());
        String payload = "{ \"roomid\":" + channelId + ", \"uid\":" + tmpUid + "}";
        try {
            ctx.writeAndFlush (simplePacket(PacketMsgType.Handshake, payload));
        } catch (Exception e) {
            e.printStackTrace ();
            ctx.close ();
            return;
        }
        stopThread(heartbeat);
        heartbeat = new Thread() {
        	@Override
        	public void run() {
                int errorTimes = 0;
                Packet ping = simplePacket(PacketMsgType.Heartbeat,"");
                while (isActive) {
                    try {
                        ctx.writeAndFlush (ping);
                        System.out.println("Heartbeat...");
                    } catch (Exception e) {
                        e.printStackTrace ();
                        if (errorTimes > retryTimes) break;
                        ++errorTimes;
                        continue;
                    }
                    try {
						Thread.sleep(30000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
                }
                ctx.close ();
        	}
        };
        heartbeat.start();
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		this.isActive = false;
        System.out.println("channelInactive");
        stopThread(heartbeat);
        super.channelInactive(ctx);
	}
	
	private void stopThread(Thread thread) {
        if(thread!= null) {
        	try {
        		thread.interrupt();
        	}catch (Exception e) {
                e.printStackTrace ();
            }
        }
	}
}
