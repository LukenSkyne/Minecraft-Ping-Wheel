package nx.pingwheel.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import nx.pingwheel.common.commands.ServerCommandBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import nx.pingwheel.common.config.ConfigHandler;
import nx.pingwheel.common.config.ServerConfig;
import nx.pingwheel.common.core.ServerCore;
import nx.pingwheel.common.helper.LanguageUtils;
import nx.pingwheel.common.networking.PingLocationC2SPacket;
import nx.pingwheel.common.networking.PingLocationS2CPacket;
import nx.pingwheel.common.networking.UpdateChannelC2SPacket;
import nx.pingwheel.fabric.networking.NetworkHandler;

import static nx.pingwheel.common.Global.*;

public class Main implements ModInitializer {

	private static final StreamCodec<FriendlyByteBuf, PingLocationS2CPacket> PING_LOCATION_S2C_CODEC = StreamCodec.ofMember(PingLocationS2CPacket::write, PingLocationS2CPacket::readSafe);
	private static final StreamCodec<FriendlyByteBuf, PingLocationC2SPacket> PING_LOCATION_C2S_CODEC = StreamCodec.ofMember(PingLocationC2SPacket::write, PingLocationC2SPacket::readSafe);
	private static final StreamCodec<FriendlyByteBuf, UpdateChannelC2SPacket> UPDATE_CHANNEL_C2S_CODEC = StreamCodec.ofMember(UpdateChannelC2SPacket::write, UpdateChannelC2SPacket::readSafe);

	@Override
	public void onInitialize() {
		LOGGER.info("Init");

		NetHandler = new NetworkHandler();

		ServerConfigHandler = new ConfigHandler<>(ServerConfig.class, FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".server.json"));
		ServerConfigHandler.load();

		ModVersion = FabricLoader.getInstance().getModContainer(MOD_ID)
			.map(container -> container.getMetadata().getVersion().getFriendlyString())
			.orElse("Unknown");

		PayloadTypeRegistry.playS2C().register(PingLocationS2CPacket.PACKET_TYPE, PING_LOCATION_S2C_CODEC);
		PayloadTypeRegistry.playC2S().register(PingLocationC2SPacket.PACKET_TYPE, PING_LOCATION_C2S_CODEC);
		PayloadTypeRegistry.playC2S().register(UpdateChannelC2SPacket.PACKET_TYPE, UPDATE_CHANNEL_C2S_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(PingLocationC2SPacket.PACKET_TYPE, (packet, context) -> ServerCore.onPingLocation(context.player().server, context.player(), packet));
		ServerPlayNetworking.registerGlobalReceiver(UpdateChannelC2SPacket.PACKET_TYPE, (packet, context) -> ServerCore.onChannelUpdate(context.player(), packet));
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
