package nx.pingwheel.common.networking;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static nx.pingwheel.common.Global.MOD_ID;

@AllArgsConstructor
@Getter
public class PingLocationPacketS2C {

	private String channel;
	private Vec3d pos;
	@Nullable
	private UUID entity;
	private int sequence;
	private UUID author;

	public static final Identifier ID = new Identifier(MOD_ID + "-s2c", "ping-location");

	public static Optional<PingLocationPacketS2C> parse(PacketByteBuf buf) {
		try {
			var channel = buf.readString(128);
			var pos = new Vec3d(
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble());
			var uuid = buf.readBoolean() ? buf.readUuid() : null;
			var sequence = (buf.readableBytes() > 0) ? buf.readInt() : -1;
			var author = (buf.readableBytes() > 0) ? buf.readUuid() : UUID.randomUUID();

			if (buf.readableBytes() > 0) {
				return Optional.empty();
			}

			return Optional.of(new PingLocationPacketS2C(channel, pos, uuid, sequence, author));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
