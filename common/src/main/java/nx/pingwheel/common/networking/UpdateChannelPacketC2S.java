package nx.pingwheel.common.networking;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

@AllArgsConstructor
@Getter
public class UpdateChannelPacketC2S {

	private String channel;

	public static final ResourceLocation ID = new ResourceLocation(MOD_ID + "-c2s", "update-channel");

	public void send() {
		var netHandler = Game.getConnection();

		if (netHandler == null) {
			return;
		}

		var packet = new FriendlyByteBuf(Unpooled.buffer());
		packet.writeUtf(channel, MAX_CHANNEL_LENGTH);

		netHandler.send(new ServerboundCustomPayloadPacket(ID, packet));
	}

	public static Optional<UpdateChannelPacketC2S> parse(FriendlyByteBuf buf) {
		try {
			var channel = buf.readUtf(MAX_CHANNEL_LENGTH);

			if (buf.readableBytes() > 0) {
				return Optional.empty();
			}

			return Optional.of(new UpdateChannelPacketC2S(channel));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
