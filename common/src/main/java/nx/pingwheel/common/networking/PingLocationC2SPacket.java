package nx.pingwheel.common.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import static nx.pingwheel.common.Global.MOD_ID;
import static nx.pingwheel.common.config.ClientConfig.MAX_CHANNEL_LENGTH;

public record PingLocationC2SPacket(String channel, Vec3 pos, UUID entity, int sequence, int dimension) implements IPacket {

	public static final ResourceLocation PACKET_ID = new ResourceLocation(MOD_ID + "-c2s", "ping-location");
	public static final Type<PingLocationC2SPacket> PACKET_TYPE = new Type<>(PACKET_ID);

	public PingLocationC2SPacket() {
		this(null, null, null, 0, 0);
	}

	public PingLocationC2SPacket(FriendlyByteBuf buf) {
		this(
			buf.readUtf(MAX_CHANNEL_LENGTH),
			new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
			buf.readBoolean() ? buf.readUUID() : null,
			buf.readInt(),
			buf.readInt()
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
	}

	public boolean isCorrupt() {
		return channel == null;
	}

	public ResourceLocation getId() {
		return PACKET_ID;
	}

	public static PingLocationC2SPacket readSafe(FriendlyByteBuf buf) {
		return PacketHandler.readSafe(buf, PingLocationC2SPacket.class);
	}

	@Override
	public Type<PingLocationC2SPacket> type() {
		return PACKET_TYPE;
	}
}
