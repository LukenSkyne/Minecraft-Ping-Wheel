package nx.pingwheel.forge.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.PacketDistributor;
import nx.pingwheel.common.networking.INetworkHandler;
import nx.pingwheel.common.networking.IPacket;

import java.util.HashMap;
import java.util.Map;

import static nx.pingwheel.common.ClientGlobal.Game;

public class NetworkHandler implements INetworkHandler {

	private final Map<ResourceLocation, Channel<FriendlyByteBuf>> channels;

	@SafeVarargs
	public NetworkHandler(Channel<FriendlyByteBuf>... channels) {
		this.channels = new HashMap<>();

		for (Channel<FriendlyByteBuf> channel : channels) {
			this.channels.put(channel.getName(), channel);
		}
	}

	@Override
	public void sendToServer(IPacket packet) {
		var channel = channels.get(packet.getId());

		if (Game.getConnection() == null || channel == null) {
			return;
		}

		var buffer = new FriendlyByteBuf(Unpooled.buffer());
		packet.write(buffer);
		channel.send(buffer, PacketDistributor.SERVER.noArg());
	}

	@Override
	public void sendToClient(IPacket packet, ServerPlayer player) {
		var channel = channels.get(packet.getId());

		if (Game.getConnection() == null || channel == null) {
			return;
		}

		var buffer = new FriendlyByteBuf(Unpooled.buffer());
		packet.write(buffer);
		channel.send(buffer, PacketDistributor.PLAYER.with(player));
	}
}
