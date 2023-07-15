package nx.pingwheel.shared.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import nx.pingwheel.shared.PingWheel;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

@AllArgsConstructor
@Getter
public class PingLocationPacketS2C {

	private String channel;
	private Vec3d pos;
	@Nullable
	private UUID entity;

	public static final Identifier ID = new Identifier(PingWheel.MOD_ID + "-s2c", "ping-location");

	public void send() {
		var packet = PacketByteBufs.create();

		packet.writeString(channel);
		packet.writeDouble(pos.x);
		packet.writeDouble(pos.y);
		packet.writeDouble(pos.z);
		packet.writeBoolean(entity != null);

		if (entity != null) {
			packet.writeUuid(entity);
		}

		ClientPlayNetworking.send(ID, packet);
	}

	public static Optional<PingLocationPacketS2C> parse(PacketByteBuf buf) {
		try {
			var channel = buf.readString(128);
			var pos = new Vec3d(
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble());
			var uuid = buf.readBoolean() ? buf.readUuid() : null;

			return Optional.of(new PingLocationPacketS2C(channel, pos, uuid));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
