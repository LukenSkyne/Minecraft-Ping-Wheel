package nx.pingwheel.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import nx.pingwheel.common.core.ServerCore;
import nx.pingwheel.common.networking.PingLocationPacketC2S;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;

import static nx.pingwheel.common.Global.*;

public class Main implements ModInitializer {

	@Override
	public void onInitialize() {
		LOGGER.info("Init");

		ModVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("Unknown");

		ServerPlayNetworking.registerGlobalReceiver(PingLocationPacketC2S.ID, (a, player, b, packet, c) -> ServerCore.onPingLocation(player, packet));
		ServerPlayNetworking.registerGlobalReceiver(UpdateChannelPacketC2S.ID, (a, player, b, packet, c) -> ServerCore.onChannelUpdate(player, packet));
		ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, a) -> ServerCore.onPlayerDisconnect(networkHandler.player));
	}
}
