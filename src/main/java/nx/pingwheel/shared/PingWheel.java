package nx.pingwheel.shared;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import nx.pingwheel.shared.network.PingLocationPacketC2S;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.apache.logging.log4j.message.Message;

public class PingWheel implements ModInitializer {

	public static final String MOD_ID = "ping-wheel";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID,
		new FormattedMessageFactory() {
			@Override
			public Message newMessage(String message) {
				return super.newMessage("[Ping-Wheel] " + message);
			}
		});
	public static final MinecraftClient Game = MinecraftClient.getInstance();

	@Override
	public void onInitialize() {
		LOGGER.info("Init");
		ServerPlayNetworking.registerGlobalReceiver(PingLocationPacketC2S.ID, (a, player, b, packet, c) -> ServerCore.onPingLocation(player, packet));
	}
}
