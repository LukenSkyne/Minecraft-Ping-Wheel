package nx.pingwheel.common.networking;

import net.minecraft.server.level.ServerPlayer;

public interface INetworkHandler {
	void sendToServer(IPacket packet);
	void sendToClient(IPacket packet, ServerPlayer player);
}
