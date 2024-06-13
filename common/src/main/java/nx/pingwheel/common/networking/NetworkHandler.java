package nx.pingwheel.common.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;

import static nx.pingwheel.common.ClientGlobal.Game;

public class NetworkHandler implements INetworkHandler {

	@Override
	public void sendToServer(IPacket packet) {
		var connection = Game.getConnection();

		if (connection == null) {
			return;
		}

		var buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeResourceLocation(packet.getId());
		packet.write(buf);

		connection.send(new ServerboundCustomPayloadPacket(buf));
	}

	@Override
	public void sendToClient(IPacket packet, ServerPlayer player) {
		var buf = new FriendlyByteBuf(Unpooled.buffer());
		buf.writeResourceLocation(packet.getId());
		packet.write(buf);

		player.connection.send(new ClientboundCustomPayloadPacket(buf));
	}
}
