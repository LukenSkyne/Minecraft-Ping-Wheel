package nx.pingwheel.common.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface IPacket {
	void write(FriendlyByteBuf buf);
	boolean isCorrupt();
	ResourceLocation getId();
}
