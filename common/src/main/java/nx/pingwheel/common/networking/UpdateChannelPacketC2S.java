package nx.pingwheel.common.networking;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

import java.util.Optional;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.config.Config.MAX_CHANNEL_LENGTH;

@AllArgsConstructor
@Getter
public class UpdateChannelPacketC2S {

	private String channel;

	public static final Identifier ID = new Identifier(MOD_ID + "-c2s", "update-channel");

	public void send() {
		var netHandler = Game.getNetworkHandler();

		if (netHandler == null) {
			return;
		}

		var packet = new PacketByteBuf(Unpooled.buffer());
		packet.writeIdentifier(UpdateChannelPacketC2S.ID);

		packet.writeString(channel, MAX_CHANNEL_LENGTH);

		netHandler.sendPacket(new CustomPayloadC2SPacket(packet));
	}

	public static Optional<UpdateChannelPacketC2S> parse(PacketByteBuf buf) {
		try {
			var channel = buf.readString(MAX_CHANNEL_LENGTH);

			if (buf.readableBytes() > 0) {
				return Optional.empty();
			}

			return Optional.of(new UpdateChannelPacketC2S(channel));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
