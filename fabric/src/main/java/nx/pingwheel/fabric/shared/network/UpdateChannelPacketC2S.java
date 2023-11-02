package nx.pingwheel.fabric.shared.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import nx.pingwheel.fabric.shared.PingWheel;

import java.util.Optional;

@AllArgsConstructor
@Getter
public class UpdateChannelPacketC2S {

	private String channel;

	public static final Identifier ID = new Identifier(PingWheel.MOD_ID + "-c2s", "update-channel");

	public void send() {
		var packet = PacketByteBufs.create();
		packet.writeString(channel);
		ClientPlayNetworking.send(ID, packet);
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
}
