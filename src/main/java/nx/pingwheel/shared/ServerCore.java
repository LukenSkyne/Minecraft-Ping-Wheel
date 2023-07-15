package nx.pingwheel.shared;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import nx.pingwheel.shared.network.PingLocationPacketC2S;
import nx.pingwheel.shared.network.PingLocationPacketS2C;

public class ServerCore {

	public static void onPingLocation(ServerPlayerEntity player, PacketByteBuf packet) {
		var packetCopy = PacketByteBufs.copy(packet);
		var pingLocationPacket = PingLocationPacketC2S.parse(packet);

		if (pingLocationPacket.isEmpty()) {
			return;
		}

		for (ServerPlayerEntity p : PlayerLookup.world(player.getWorld())) {
			ServerPlayNetworking.send(p, PingLocationPacketS2C.ID, packetCopy);
		}
	}
}
