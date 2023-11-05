package nx.pingwheel.common.networking;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.config.Config.MAX_CHANNEL_LENGTH;

@AllArgsConstructor
@Getter
public class PingLocationPacketC2S {

	private String channel;
	private Vec3d pos;
	@Nullable
	private UUID entity;
	private int sequence;

	public static final Identifier ID = new Identifier(MOD_ID + "-c2s", "ping-location");

	public void send() {
		var netHandler = Game.getNetworkHandler();

		if (netHandler == null) {
			return;
		}

		var packet = new PacketByteBuf(Unpooled.buffer());
		packet.writeIdentifier(PingLocationPacketC2S.ID);

		packet.writeString(channel, MAX_CHANNEL_LENGTH);
		packet.writeDouble(pos.x);
		packet.writeDouble(pos.y);
		packet.writeDouble(pos.z);
		packet.writeBoolean(entity != null);

		if (entity != null) {
			packet.writeUuid(entity);
		}

		packet.writeInt(sequence);

		netHandler.sendPacket(new CustomPayloadC2SPacket(packet));
	}

	public static Optional<PingLocationPacketC2S> parse(PacketByteBuf buf) {
		try {
			var channel = buf.readString(MAX_CHANNEL_LENGTH);
			var pos = new Vec3d(
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble());
			var uuid = buf.readBoolean() ? buf.readUuid() : null;
			var sequence = buf.readInt();

			if (buf.readableBytes() > 0) {
				return Optional.empty();
			}

			return Optional.of(new PingLocationPacketC2S(channel, pos, uuid, sequence));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
