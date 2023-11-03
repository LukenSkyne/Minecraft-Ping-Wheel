package nx.pingwheel.forge.shared.network;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import nx.pingwheel.forge.shared.PingWheel;

import java.util.Optional;

import static nx.pingwheel.forge.client.PingWheelClient.Game;

@AllArgsConstructor
@Getter
public class UpdateChannelPacketC2S {

	private String channel;

	public static final Identifier ID = new Identifier(PingWheel.MOD_ID + "-c2s", "update-channel");

	public void send() {
		var netHandler = Game.getNetworkHandler();

		if (netHandler == null) {
			return;
		}

		var packet = new PacketByteBuf(Unpooled.buffer());
		packet.writeString(channel);

		netHandler.getConnection().send(new CustomPayloadC2SPacket(ID, packet));
	}

	public static Optional<UpdateChannelPacketC2S> parse(PacketByteBuf buf) {
		try {
			var channel = buf.readString(128);

			if (buf.readableBytes() > 0) {
				return Optional.empty();
			}

			return Optional.of(new UpdateChannelPacketC2S(channel));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static void encode(UpdateChannelPacketC2S msg, PacketByteBuf buffer) {
		buffer.writeString(msg.channel, 128);
	}

	public static UpdateChannelPacketC2S decode(PacketByteBuf buffer) {
		var channel = buffer.readString(128);

		return new UpdateChannelPacketC2S(channel);
	}
}
