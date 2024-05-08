package nx.pingwheel.common.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public record PingLocationS2CPacket(String channel, Vec3 pos, UUID entity, int sequence, int dimension, UUID author) implements IPacket {

	public static final ResourceLocation PACKET_ID = new ResourceLocation(MOD_ID + "-s2c", "ping-location");

	public PingLocationS2CPacket() {
		this(null, null, null, 0, 0, null);
	}

	public PingLocationS2CPacket(FriendlyByteBuf buf) {
		this(
			buf.readUtf(MAX_CHANNEL_LENGTH),
			new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
			buf.readBoolean() ? buf.readUUID() : null,
			buf.readInt(),
			buf.readInt(),
			buf.readUUID()
		);
	}

	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(channel, MAX_CHANNEL_LENGTH);
		buf.writeDouble(pos.x);
		buf.writeDouble(pos.y);
		buf.writeDouble(pos.z);
		buf.writeBoolean(entity != null);

		if (entity != null) {
			buf.writeUUID(entity);
		}

		buf.writeInt(sequence);
		buf.writeInt(dimension);
		buf.writeUUID(author);
	}

	public boolean isCorrupt() {
		return channel == null;
	}

	public ResourceLocation getId() {
		return PACKET_ID;
	}

	public static PingLocationS2CPacket readSafe(FriendlyByteBuf buf) {
		return PacketHandler.readSafe(buf, PingLocationS2CPacket.class);
	}

	public static PingLocationS2CPacket fromClientPacket(PingLocationC2SPacket clientPacket, UUID author) {
		return new PingLocationS2CPacket(
			clientPacket.channel(),
			clientPacket.pos(),
			clientPacket.entity(),
			clientPacket.sequence(),
			clientPacket.dimension(),
			author
		);
	}
}
