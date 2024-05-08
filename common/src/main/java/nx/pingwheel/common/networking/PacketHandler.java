package nx.pingwheel.common.networking;

import lombok.SneakyThrows;
import net.minecraft.network.FriendlyByteBuf;

public class PacketHandler {
	PacketHandler() {}

	@SneakyThrows
	public static <T> T readSafe(FriendlyByteBuf buf, Class<T> packetClass) {
		try {
			return packetClass.getDeclaredConstructor(FriendlyByteBuf.class).newInstance(buf);
		} catch (Exception e) {
			return packetClass.getDeclaredConstructor().newInstance();
		} finally {
			if (buf.readableBytes() > 0) {
				buf.readerIndex(buf.readerIndex() + buf.readableBytes());
			}
		}
	}
}
