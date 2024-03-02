package nx.pingwheel.common.core;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import nx.pingwheel.common.config.ServerConfig;
import nx.pingwheel.common.helper.RateLimiter;
import nx.pingwheel.common.networking.PingLocationPacketC2S;
import nx.pingwheel.common.networking.PingLocationPacketS2C;
import nx.pingwheel.common.networking.UpdateChannelPacketC2S;

import java.util.HashMap;
import java.util.UUID;

import static nx.pingwheel.common.Global.*;

public class ServerCore {
	private ServerCore() {}

	private static final ServerConfig Config = ServerConfigHandler.getConfig();
	private static final HashMap<UUID, String> playerChannels = new HashMap<>();
	private static final HashMap<UUID, RateLimiter> playerRates = new HashMap<>();

	public static void init() {
		RateLimiter.setRates(Config.getMsToRegenerate(), Config.getRateLimit());
	}

	public static void onPlayerDisconnect(ServerPlayerEntity player) {
		playerChannels.remove(player.getUuid());
		playerRates.remove(player.getUuid());
	}

	public static void onChannelUpdate(ServerPlayerEntity player, PacketByteBuf packet) {
		var channelUpdatePacket = UpdateChannelPacketC2S.parse(packet);

		if (channelUpdatePacket.isEmpty()) {
			LOGGER.warn("invalid channel update from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUuid()));
			player.sendMessage(Text.of("§c[Ping-Wheel] Channel couldn't be updated. Make sure your version matches the server's version: " + ModVersion), false);
			return;
		}

		updatePlayerChannel(player, channelUpdatePacket.get().getChannel());
	}

	public static void onPingLocation(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf packet) {
		var rateLimiter = playerRates.get(player.getUuid());

		if (rateLimiter == null) {
			playerRates.put(player.getUuid(), new RateLimiter());
		} else if (Config.getRateLimit() > 0 && rateLimiter.checkAndBlock()) {
			return;
		}

		var packetCopy = new PacketByteBuf(packet.copy());
		var pingLocationPacket = PingLocationPacketC2S.parse(packet);

		if (pingLocationPacket.isEmpty()) {
			LOGGER.warn("invalid ping location from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUuid()));
			player.sendMessage(Text.of("[Ping-Wheel] §cUnable to send ping\n§7Make sure your version matches the server's version: §d" + ModVersion), false);
			return;
		}

		var channel = pingLocationPacket.get().getChannel();

		if (!channel.equals(playerChannels.getOrDefault(player.getUuid(), ""))) {
			updatePlayerChannel(player, channel);
		}

		packetCopy.writeUuid(player.getUuid());

		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (!channel.equals(playerChannels.getOrDefault(p.getUuid(), ""))) {
				continue;
			}

			p.networkHandler.sendPacket(new CustomPayloadS2CPacket(PingLocationPacketS2C.ID, packetCopy));
		}
	}

	private static void updatePlayerChannel(ServerPlayerEntity player, String channel) {
		if (channel.isEmpty()) {
			playerChannels.remove(player.getUuid());
			LOGGER.info("Channel update: " + String.format("%s -> Global", player.getGameProfile().getName()));
		} else {
			playerChannels.put(player.getUuid(), channel);
			LOGGER.info("Channel update: " + String.format("%s -> \"%s\"", player.getGameProfile().getName(), channel));
		}
	}
}
