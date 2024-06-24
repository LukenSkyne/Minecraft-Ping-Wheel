package nx.pingwheel.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import nx.pingwheel.common.commands.ServerCommandBuilder;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.config.ServerConfig;
import nx.pingwheel.common.core.ServerCore;
import nx.pingwheel.common.commands.ServerCommandBuilder;
import nx.pingwheel.common.helper.LanguageUtils;
import nx.pingwheel.common.networking.NetworkHandler;
import nx.pingwheel.common.networking.PingLocationC2SPacket;
import nx.pingwheel.common.networking.UpdateChannelC2SPacket;

import static nx.pingwheel.common.Global.*;

public class Main implements ModInitializer {

	@Override
	public void onInitialize() {
		LOGGER.info("Init");

		NetHandler = new NetworkHandler();

		ServerConfigHandler = new ConfigHandler<>(ServerConfig.class, FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".server.json"));
		ServerConfigHandler.load();

		ModVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("Unknown");

		ServerPlayNetworking.registerGlobalReceiver(PingLocationC2SPacket.PACKET_ID, (server, player, b, packet, c) -> ServerCore.onPingLocation(server, player, PingLocationC2SPacket.readSafe(packet)));
		ServerPlayNetworking.registerGlobalReceiver(UpdateChannelC2SPacket.PACKET_ID, (a, player, b, packet, c) -> ServerCore.onChannelUpdate(player, UpdateChannelC2SPacket.readSafe(packet)));
		ServerPlayConnectionEvents.DISCONNECT.register((networkHandler, a) -> ServerCore.onPlayerDisconnect(networkHandler.player));

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(ServerCommandBuilder.build((context, success, response) -> {
			if (success) {
				context.getSource().sendSuccess(() -> LanguageUtils.withModPrefix(response), false);
			} else {
				context.getSource().sendFailure(LanguageUtils.withModPrefix(response));
			}
		})));

		ServerCore.init();
	}
}
