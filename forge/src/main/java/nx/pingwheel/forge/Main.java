package nx.pingwheel.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import nx.pingwheel.common.commands.ServerCommandBuilder;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.config.ServerConfig;
import nx.pingwheel.common.core.ServerCore;
import nx.pingwheel.common.helper.LanguageUtils;
import nx.pingwheel.common.networking.NetworkHandler;
import nx.pingwheel.common.networking.PingLocationC2SPacket;
import nx.pingwheel.common.networking.PingLocationS2CPacket;
import nx.pingwheel.common.networking.UpdateChannelC2SPacket;

import static nx.pingwheel.common.Global.*;
import static nx.pingwheel.forge.Main.FORGE_ID;

@Mod(FORGE_ID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Main {

	public static final String FORGE_ID = "pingwheel";

	private static final String PROTOCOL_VERSION = "1";
	public static final EventNetworkChannel PING_LOCATION_CHANNEL_C2S = NetworkRegistry.newEventChannel(
		PingLocationC2SPacket.PACKET_ID,
		() -> PROTOCOL_VERSION,
		c -> true,
		s -> true
	);
	public static final EventNetworkChannel PING_LOCATION_CHANNEL_S2C = NetworkRegistry.newEventChannel(
		PingLocationS2CPacket.PACKET_ID,
		() -> PROTOCOL_VERSION,
		c -> true,
		s -> true
	);
	public static final EventNetworkChannel UPDATE_CHANNEL_C2S = NetworkRegistry.newEventChannel(
		UpdateChannelC2SPacket.PACKET_ID,
		() -> PROTOCOL_VERSION,
		c -> true,
		s -> true
	);

	@SuppressWarnings({"java:S1118", "the public constructor is required by forge"})
	public Main() {
		LOGGER.info("Init");

		NetHandler = new NetworkHandler();

		ServerConfigHandler = new ConfigHandler<>(ServerConfig.class, FMLPaths.CONFIGDIR.get().resolve(MOD_ID + ".server.json"));
		ServerConfigHandler.load();

		ModVersion = ModList.get().getModContainerById(FORGE_ID)
			.map(container -> container.getModInfo().getVersion().toString())
			.orElse("Unknown");

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> Client::new);

		PING_LOCATION_CHANNEL_C2S.addListener((event) -> {
			var ctx = event.getSource().get();
			var payload = event.getPayload();
			var sender = ctx.getSender();

			if (payload != null && sender != null) {
				var packet = PingLocationC2SPacket.readSafe(payload);
				ctx.enqueueWork(() -> ServerCore.onPingLocation(sender.getServer(), sender, packet));
			}

			ctx.setPacketHandled(true);
		});

		UPDATE_CHANNEL_C2S.addListener((event) -> {
			var ctx = event.getSource().get();
			var payload = event.getPayload();

			if (payload != null) {
				var packet = UpdateChannelC2SPacket.readSafe(payload);
				ctx.enqueueWork(() -> ServerCore.onChannelUpdate(ctx.getSender(), packet));
			}

			ctx.setPacketHandled(true);
		});

		ServerCore.init();
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		ServerCore.onPlayerDisconnect((ServerPlayer)event.getEntity());
	}

	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(ServerCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendSuccess(() -> LanguageUtils.withModPrefix(response), false);
			} else {
				context.getSource().sendFailure(LanguageUtils.withModPrefix(response));
			}
		}));
	}
}
