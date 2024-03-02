package nx.pingwheel.common.helper;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import nx.pingwheel.common.compat.Vector3f;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class PingData {

	@Setter
	private Vec3d pos;
	@Nullable
	private final UUID uuid;
	private final UUID author;
	private final String authorName;
	private final int sequence;
	private final int dimension;
	private final int spawnTime;

	public int aliveTime;
	public double distance;
	@Nullable
	public Vector3f screenPos;
	@Nullable
	public ItemStack itemStack;

	public PingData(Vec3d pos, @Nullable UUID uuid, UUID author, String authorName, int sequence, int dimension, int spawnTime) {
		this.pos = pos;
		this.uuid = uuid;
		this.author = author;
		this.authorName = authorName;
		this.sequence = sequence;
		this.dimension = dimension;
		this.spawnTime = spawnTime;
	}
}
