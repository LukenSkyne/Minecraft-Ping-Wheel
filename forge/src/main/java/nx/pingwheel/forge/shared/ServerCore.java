package nx.pingwheel.forge.shared;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import nx.pingwheel.forge.shared.network.PingLocationPacketC2S;
import nx.pingwheel.forge.shared.network.PingLocationPacketS2C;
import nx.pingwheel.forge.shared.network.UpdateChannelPacketC2S;

import java.util.HashMap;
import java.util.UUID;

import static nx.pingwheel.forge.shared.PingWheel.LOGGER;
import static nx.pingwheel.forge.shared.PingWheel.MOD_VERSION;

public class ServerCore {

	private static final HashMap<UUID, String> playerChannels = new HashMap<>();

	public static void onPlayerDisconnect(ServerPlayerEntity player) {
		playerChannels.remove(player.getUuid());
	}

	public static void onChannelUpdate(ServerPlayerEntity player, PacketByteBuf packet) {
		var channelUpdatePacket = UpdateChannelPacketC2S.parse(packet);

		if (channelUpdatePacket.isEmpty()) {
			LOGGER.warn("invalid channel update from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUuid()));
			player.sendMessage(Text.of("§c[Ping-Wheel] Channel couldn't be updated. Make sure your version matches the server's version: " + MOD_VERSION), false);
			return;
		}

		updatePlayerChannel(player, channelUpdatePacket.get().getChannel());
	}

	public static void onPingLocation(ServerPlayerEntity player, PacketByteBuf packet) {
		var packetCopy = new PacketByteBuf(packet.copy());
		var pingLocationPacket = PingLocationPacketC2S.parse(packet);

		if (pingLocationPacket.isEmpty()) {
			LOGGER.warn("invalid ping location from " + String.format("%s (%s)", player.getGameProfile().getName(), player.getUuid()));
			player.sendMessage(Text.of("§c[Ping-Wheel] Ping couldn't be sent. Make sure your version matches the server's version: " + MOD_VERSION), false);
			return;
		}

		var channel = pingLocationPacket.get().getChannel();

		if (!channel.equals(playerChannels.getOrDefault(player.getUuid(), ""))) {
			updatePlayerChannel(player, channel);
		}

		packetCopy.writeUuid(player.getUuid());

		for (ServerPlayerEntity p : player.getWorld().getPlayers()) {
			if (!channel.equals(playerChannels.getOrDefault(p.getUuid(), ""))) {
				continue;
			}

			p.networkHandler.connection.send(new CustomPayloadS2CPacket(PingLocationPacketS2C.ID, packetCopy));
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
