package nx.pingwheel.common.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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

	public static void onPlayerDisconnect(ServerPlayer player) {
		playerChannels.remove(player.getUUID());
		playerRates.remove(player.getUUID());
	}

	public static void onChannelUpdate(ServerPlayer player, FriendlyByteBuf packet) {
		var channelUpdatePacket = UpdateChannelPacketC2S.parse(packet);

		if (channelUpdatePacket.isEmpty()) {
			LOGGER.warn("invalid channel update from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUUID()));
			player.displayClientMessage(Component.nullToEmpty("§c[Ping-Wheel] Channel couldn't be updated. Make sure your version matches the server's version: " + ModVersion), false);
			return;
		}

		updatePlayerChannel(player, channelUpdatePacket.get().getChannel());
	}

	public static void onPingLocation(MinecraftServer server, ServerPlayer player, FriendlyByteBuf packet) {
		var rateLimiter = playerRates.get(player.getUUID());

		if (rateLimiter == null) {
			playerRates.put(player.getUUID(), new RateLimiter());
		} else if (Config.getRateLimit() > 0 && rateLimiter.checkAndBlock()) {
			return;
		}

		var packetCopy = new FriendlyByteBuf(packet.copy());
		var pingLocationPacket = PingLocationPacketC2S.parse(packet);

		if (pingLocationPacket.isEmpty()) {
			LOGGER.warn("invalid ping location from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUUID()));
			player.displayClientMessage(Component.nullToEmpty("[Ping-Wheel] §cUnable to send ping\n§7Make sure your version matches the server's version: §d" + ModVersion), false);
			return;
		}

		var channel = pingLocationPacket.get().getChannel();

		if (channel.isEmpty() && Config.isGlobalChannelDisabled()) {
			player.displayClientMessage(Component.nullToEmpty("[Ping-Wheel] §eThe global channel is disabled on this server\n§7Use §a/pingwheel channel§7 to switch"), false);
			return;
		}

		if (!channel.equals(playerChannels.getOrDefault(player.getUUID(), ""))) {
			updatePlayerChannel(player, channel);
		}

		packetCopy.writeUUID(player.getUUID());

		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!channel.equals(playerChannels.getOrDefault(p.getUUID(), ""))) {
				continue;
			}

			p.connection.send(new ClientboundCustomPayloadPacket(PingLocationPacketS2C.ID, packetCopy));
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
