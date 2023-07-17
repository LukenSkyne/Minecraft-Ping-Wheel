package nx.pingwheel.shared;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import nx.pingwheel.shared.network.PingLocationPacketC2S;
import nx.pingwheel.shared.network.UpdateChannelPacketC2S;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.apache.logging.log4j.message.Message;

public class PingWheel implements ModInitializer {

	public static final String MOD_ID = "ping-wheel";
	public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
		.map(container -> container.getMetadata().getVersion().getFriendlyString())
		.orElse("Unknown");
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID,
		new FormattedMessageFactory() {
			@Override
			public Message newMessage(String message) {
				return super.newMessage("[Ping-Wheel] " + message);
			}
		});

	@Override
	public void onInitialize() {
		LOGGER.info("Init");
		ServerPlayNetworking.registerGlobalReceiver(PingLocationPacketC2S.ID, (a, player, b, packet, c) -> ServerCore.onPingLocation(player, packet));
		ServerPlayNetworking.registerGlobalReceiver(UpdateChannelPacketC2S.ID, (a, player, b, packet, c) -> ServerCore.onChannelUpdate(player, packet));
		ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, a) -> ServerCore.onPlayerDisconnect(networkHandler.player));
	}
}
