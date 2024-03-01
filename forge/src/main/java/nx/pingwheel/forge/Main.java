package nx.pingwheel.forge;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.config.ServerConfig;
import nx.pingwheel.common.core.ServerCore;
import nx.pingwheel.common.networking.PingLocationPacketC2S;
import nx.pingwheel.common.networking.PingLocationPacketS2C;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;

import static nx.pingwheel.common.Global.*;
import static nx.pingwheel.forge.Main.FORGE_ID;

@Mod(FORGE_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Main {

	public static final String FORGE_ID = "pingwheel";

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

	@SuppressWarnings({"java:S1118", "the public constructor is required by forge"})
	public Main() {
		LOGGER.info("Init");

		ServerConfigHandler = new ConfigHandler<>(ServerConfig.class, FMLPaths.CONFIGDIR.get().resolve(MOD_ID + ".server.json"));
		ServerConfigHandler.load();

		ModVersion = ModList.get().getModContainerById(FORGE_ID)
			.map(container -> container.getModInfo().getVersion().toString())
			.orElse("Unknown");

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> Client::new);

		PING_LOCATION_CHANNEL_C2S.addListener((event) -> {
			var ctx = event.getSource().get();
			var packet = event.getPayload();
			var sender = ctx.getSender();

			if (packet != null && sender != null) {
				ctx.enqueueWork(() -> ServerCore.onPingLocation(sender.getServer(), sender, packet));
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

		ServerCore.init();
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		ServerCore.onPlayerDisconnect((ServerPlayerEntity)event.getPlayer());
	}
}
