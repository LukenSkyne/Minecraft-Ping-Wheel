package nx.pingwheel.forge.shared;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import nx.pingwheel.forge.client.PingWheelClient;
import nx.pingwheel.forge.shared.network.PingLocationPacketC2S;
import nx.pingwheel.forge.shared.network.PingLocationPacketS2C;
import nx.pingwheel.forge.shared.network.UpdateChannelPacketC2S;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessageFactory;
import org.apache.logging.log4j.message.Message;

import static nx.pingwheel.forge.shared.PingWheel.FORGE_ID;

@Mod(FORGE_ID)
public class PingWheel {

	public static final String MOD_ID = "ping-wheel";
	public static final String FORGE_ID = "pingwheel";
	public static final String MOD_VERSION = ModList.get().getModContainerById(FORGE_ID)
		.map(container -> container.getModInfo().getVersion().toString())
		.orElse("Unknown");
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID,
		new FormattedMessageFactory() {
			@Override
			public Message newMessage(String message) {
				return super.newMessage("[Ping-Wheel] " + message);
			}
		});

	private static final String PROTOCOL_VERSION = "1";
	public static final EventNetworkChannel PING_LOCATION_CHANNEL_C2S = NetworkRegistry.newEventChannel(
		PingLocationPacketC2S.ID,
		() -> PROTOCOL_VERSION,
		c -> true,
		s -> true
	);
	public static final EventNetworkChannel PING_LOCATION_CHANNEL_S2C = NetworkRegistry.newEventChannel(
		PingLocationPacketS2C.ID,
		() -> PROTOCOL_VERSION,
		c -> true,
		s -> true
	);
	public static final EventNetworkChannel UPDATE_CHANNEL_C2S = NetworkRegistry.newEventChannel(
		UpdateChannelPacketC2S.ID,
		() -> PROTOCOL_VERSION,
		c -> true,
		s -> true
	);

	public PingWheel() {
		if (FMLLoader.getDist().isClient()) {
			new PingWheelClient();
		}

		LOGGER.info("Init");

		PING_LOCATION_CHANNEL_C2S.addListener((event) -> {
			var ctx = event.getSource().get();
			var packet = event.getPayload();

			if (packet != null) {
				ctx.enqueueWork(() -> ServerCore.onPingLocation(ctx.getSender(), packet));
			}

			ctx.setPacketHandled(true);
		});

		UPDATE_CHANNEL_C2S.addListener((event) -> {
			var ctx = event.getSource().get();
			var packet = event.getPayload();

			if (packet != null) {
				ctx.enqueueWork(() -> ServerCore.onChannelUpdate(ctx.getSender(), packet));
			}

			ctx.setPacketHandled(true);
		});
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		ServerCore.onPlayerDisconnect((ServerPlayerEntity)event.getPlayer());
	}
}
