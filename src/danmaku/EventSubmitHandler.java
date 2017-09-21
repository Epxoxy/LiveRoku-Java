package danmaku;

import base.IAction;
import base.IAction1;
import base.Utils;
import base.interfaces.IDanmakuServiceEvents;
import base.model.DanmakuModel;
import danmaku.packet.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class EventSubmitHandler extends ChannelInboundHandlerAdapter implements IDanmakuServiceEvents{
	private IAction1<Integer> hotUpdated;
	private IAction1<DanmakuModel> danmakuReceived;
	private IAction active;
	private IAction inactive;
	private boolean isActive;
	
	public boolean isActive() {
		return isActive;
	}

	@Override
	public void setHotUpdated(IAction1<Integer> hotUpdated) {
		this.hotUpdated = hotUpdated;
	}
	@Override
	public void setDanmakuReceived(IAction1<DanmakuModel> danmakuReceived) {
		this.danmakuReceived = danmakuReceived;
	}
	@Override
	public void setOnActive(IAction active) {
		this.active = active;
	}
	@Override
	public void setOnInactive(IAction inactive) {
		this.inactive = inactive;
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if(msg instanceof Packet) {
			checkPacket((Packet)msg);
		}
		super.channelRead(ctx, msg);
	}

	private void checkPacket (Packet packet) {
		System.out.println(packet.toString());
        switch (packet.packetType) {
            case 1: //online count parameter
            case 2: //online count parameter
            case 3: //online count parameter
                int num = Integer.parseInt(packet.payload);
                Utils.tryInvoke(hotUpdated, num);
                break; 
            case 5: //danmaku data
                long nowTime = System.currentTimeMillis();
                DanmakuModel danmaku = DanmakuParser.parse (packet.payload, nowTime, 2);
                Utils.tryInvoke(danmakuReceived, danmaku);
                break;
            case 4: //unknown
            case 6: //newScrollMessage
            case 7:
            case 16:
            default: break;
        }
    }

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		isActive = true;
		Utils.tryInvoke(active);
		super.channelActive(ctx);
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		isActive = false;
		Utils.tryInvoke(inactive);
		super.channelInactive(ctx);
	}
}
