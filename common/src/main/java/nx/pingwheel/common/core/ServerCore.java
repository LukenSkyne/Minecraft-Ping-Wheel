package nx.pingwheel.common.core;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nx.pingwheel.common.config.ServerConfig;
import nx.pingwheel.common.helper.RateLimiter;
import nx.pingwheel.common.networking.PingLocationC2SPacket;
import nx.pingwheel.common.networking.PingLocationS2CPacket;
import nx.pingwheel.common.networking.UpdateChannelC2SPacket;

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

	public static void onPlayerDisconnect(ServerPlayer player) {
		playerChannels.remove(player.getUUID());
		playerRates.remove(player.getUUID());
	}

	public static void onChannelUpdate(ServerPlayer player, UpdateChannelC2SPacket packet) {
		if (packet.isCorrupt()) {
			LOGGER.warn("invalid channel update from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUUID()));
			player.displayClientMessage(Component.nullToEmpty("[Ping-Wheel] §cChannel couldn't be updated\n§7Make sure your version matches the server's version: §d" + ModVersion), false);
			return;
		}

		updatePlayerChannel(player, packet.channel());
	}

	public static void onPingLocation(MinecraftServer server, ServerPlayer player, PingLocationC2SPacket packet) {
		if (packet.isCorrupt()) {
			LOGGER.warn("invalid ping location from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUUID()));
			player.displayClientMessage(Component.nullToEmpty("[Ping-Wheel] §cUnable to send ping\n§7Make sure your version matches the server's version: §d" + ModVersion), false);
			return;
		}

		var rateLimiter = playerRates.get(player.getUUID());

		if (rateLimiter == null) {
			playerRates.put(player.getUUID(), new RateLimiter());
		} else if (Config.getRateLimit() > 0 && rateLimiter.checkAndBlock()) {
			return;
		}
		
		var channel = packet.channel();

		if (channel.isEmpty() && Config.isGlobalChannelDisabled()) {
			player.displayClientMessage(Component.nullToEmpty("[Ping-Wheel] §eThe global channel is disabled on this server\n§7Use §a/pingwheel channel§7 to switch"), false);
			return;
		}

		if (!channel.equals(playerChannels.getOrDefault(player.getUUID(), ""))) {
			updatePlayerChannel(player, channel);
		}

		var packetOut = PingLocationS2CPacket.fromClientPacket(packet, player.getUUID());

		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!channel.equals(playerChannels.getOrDefault(p.getUUID(), ""))) {
				continue;
			}

			NetHandler.sendToClient(packetOut, p);
		}
	}

	private static void updatePlayerChannel(ServerPlayer player, String channel) {
		if (channel.isEmpty()) {
			playerChannels.remove(player.getUUID());
			LOGGER.info("Channel update: " + String.format("%s -> Global", player.getGameProfile().getName()));
		} else {
			playerChannels.put(player.getUUID(), channel);
			LOGGER.info("Channel update: " + String.format("%s -> \"%s\"", player.getGameProfile().getName(), channel));
		}
	}
}
