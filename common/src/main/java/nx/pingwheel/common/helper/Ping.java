package nx.pingwheel.common.helper;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import nx.pingwheel.common.config.ClientConfig;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static nx.pingwheel.common.ClientGlobal.ConfigHandler;
import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.config.ClientConfig.*;

@Getter
public class Ping {

	private static final ClientConfig Config = ConfigHandler.getConfig();

	@Setter
	private Vec3 pos;
	@Nullable
	private final UUID uuid;
	private final PlayerInfo author;
	private final int sequence;
	private final int dimension;
	private final int spawnTime;

	@Setter
	private int age;
	@Setter
	private double distance;
	@Setter
	@Nullable
	private ScreenPos screenPos;
	@Setter
	@Nullable
	private ItemStack itemStack;

	public Ping(Vec3 pos, @Nullable UUID uuid, PlayerInfo author, int sequence, int dimension, int spawnTime) {
		this.pos = pos;
		this.uuid = uuid;
		this.author = author;
		this.sequence = sequence;
		this.dimension = dimension;
		this.spawnTime = spawnTime;
	}

	public boolean isExpired() {
		return Config.getPingDuration() < MAX_PING_DURATION && this.age > Config.getPingDuration() * TPS;
	}

	public boolean isRemovable() {
		return (Config.getCorrectionPeriod() >= MAX_CORRECTION_PERIOD || this.age > Config.getCorrectionPeriod() * TPS) && this.distanceToCenter() < Config.getRemoveRadius();
	}

	public float distanceToCenter() {
		if (this.screenPos == null) {
			return 0f;
		}

		var wnd = Game.getWindow();
		var center = new Vec2(wnd.getGuiScaledWidth() * 0.5f, wnd.getGuiScaledHeight() * 0.5f);

		return this.screenPos.distanceTo(center);
	}

	public boolean isCloserToCenter(@Nullable Ping b) {
		if (b == null) {
			return true;
		}

		return this.distanceToCenter() < b.distanceToCenter();
	}
}
