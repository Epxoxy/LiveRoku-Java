package danmaku;

import base.interfaces.IDanmakuClient;
import base.interfaces.IDanmakuServiceEvents;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class DanmakuClient implements IDanmakuClient{
	private EventSubmitHandler events = new EventSubmitHandler();
	private EventLoopGroup workerGroup;
	
	public IDanmakuServiceEvents getEvents() {
		return events;
	}

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return events.isActive();
	}

	public void start(String host, int port, int channelId) {
		System.out.println(host);
		System.out.println(port);
		System.out.println(channelId);
		workerGroup = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(workerGroup);
			b.channel(NioSocketChannel.class);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(new PacketDecoder(1<<20, 12, 4));
					ch.pipeline().addLast(new PacketEncoder());
					
					ch.pipeline().addLast(new KeepAliveHandler(channelId));
					ch.pipeline().addLast(events);
				}
			});

			// Start the client.
			ChannelFuture f = b.connect(host, port).sync();

			// Wait until the connection is closed.
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
		}

	}
	
	public void stop() {
		if(workerGroup!=null) {
			workerGroup.shutdownGracefully();
			System.out.println("shutdownGracefully");
		}
	}
}
