package nx.pingwheel.fabric.networking;

import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import nx.pingwheel.common.networking.INetworkHandler;
import nx.pingwheel.common.networking.IPacket;

import static nx.pingwheel.common.ClientGlobal.Game;

public class NetworkHandler implements INetworkHandler {

	@Override
	public void sendToServer(IPacket packet) {
		var connection = Game.getConnection();

		if (connection == null) {
			return;
		}

		connection.send(new ServerboundCustomPayloadPacket(packet));
	}

	@Override
	public void sendToClient(IPacket packet, ServerPlayer player) {
		player.connection.send(new ClientboundCustomPayloadPacket(packet));
	}
}
