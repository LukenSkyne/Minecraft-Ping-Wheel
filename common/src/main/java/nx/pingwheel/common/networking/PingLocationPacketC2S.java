package nx.pingwheel.common.networking;

import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

@AllArgsConstructor
@Getter
public class PingLocationPacketC2S {

	private String channel;
	private Vec3 pos;
	@Nullable
	private UUID entity;
	private int sequence;
	private int dimension;

	public static final ResourceLocation ID = new ResourceLocation(MOD_ID + "-c2s", "ping-location");

	public void send() {
		var netHandler = Game.getConnection();

		if (netHandler == null) {
			return;
		}

		var packet = new FriendlyByteBuf(Unpooled.buffer());

		packet.writeUtf(channel, MAX_CHANNEL_LENGTH);
		packet.writeDouble(pos.x);
		packet.writeDouble(pos.y);
		packet.writeDouble(pos.z);
		packet.writeBoolean(entity != null);

		if (entity != null) {
			packet.writeUUID(entity);
		}

		packet.writeInt(sequence);
		packet.writeInt(dimension);

		netHandler.send(new ServerboundCustomPayloadPacket(ID, packet));
	}

	public static Optional<PingLocationPacketC2S> parse(FriendlyByteBuf buf) {
		try {
			var channel = buf.readUtf(MAX_CHANNEL_LENGTH);
			var pos = new Vec3(
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble());
			var uuid = buf.readBoolean() ? buf.readUUID() : null;
			var sequence = buf.readInt();
			var dimension = buf.readInt();

			if (buf.readableBytes() > 0) {
				return Optional.empty();
			}

			return Optional.of(new PingLocationPacketC2S(channel, pos, uuid, sequence, dimension));
		} catch (Exception e) {
			return Optional.empty();
		}
	}
}
