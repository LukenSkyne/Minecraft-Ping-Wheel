package nx.pingwheel.common.core;

import lombok.Getter;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.math.MathUtils;
import nx.pingwheel.common.math.ScreenPos;
import nx.pingwheel.common.network.PingLocationS2CPacket;
import nx.pingwheel.common.render.WorldRenderContext;
import nx.pingwheel.common.util.DirectionalSoundInstance;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.config.ClientConfig.*;
import static nx.pingwheel.common.resource.ResourceConstants.PING_SOUND_EVENT;

public class PingView extends PingData {

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();
	private static final int WHITE = 0xFFFFFFFF;

	private int spawnTime;
	private int age;
	@Getter
	private @Nullable PlayerInfo playerInfo;
	@Getter
	private @Nullable ItemStack itemStack;
	@Getter
	private ScreenPos screenPos;
	@Getter
	private double distance;
	@Getter
	private float scale;

	private PingView(Vec3 pos, @Nullable UUID entityId, @Nullable UUID authorId, int sequence, int dimension) {
		super(pos, entityId, authorId, sequence, dimension);
	}

	public static PingView of(Vec3 pos, @Nullable UUID entityId, @Nullable UUID authorId, int sequence, int dimension) {
		return new PingView(
			pos,
			entityId,
			authorId,
			sequence,
			dimension
		);
	}

	public static PingView from(PingLocationS2CPacket packet) {
		return PingView.of(
			packet.pos(),
			packet.entity(),
			packet.author(),
			packet.sequence(),
			packet.dimension()
		);
	}

	public void update(WorldRenderContext ctx, int gameTime) {
		if (this.spawnTime == 0) {
			this.spawnTime = gameTime;
		}

		this.age = gameTime - spawnTime;

		final var connection = Game.getConnection();

		if (connection != null && this.authorId != null) {
			this.playerInfo = connection.getPlayerInfo(this.authorId);
		}

		if (this.entityId != null) {
			final var ent = GameContext.getEntity(this.entityId);

			if (ent != null) {
				if (ent instanceof ItemEntity && CLIENT_CONFIG.isItemIconVisible()) {
					this.itemStack = ((ItemEntity)ent).getItem().copy();
				}

				this.pos = ent.getPosition(ctx.tickDelta).add(0, ent.getBoundingBox().getYsize(), 0);
			}
		}

		this.screenPos = MathUtils.worldToScreen(pos, ctx);
		this.distance = ctx.camera.pos.distanceTo(pos);
		this.calculateScale();
	}

	public void playSoundInDimension() {
		if (this.dimension != GameContext.getDimension()) {
			return;
		}

		Game.getSoundManager().play(
			new DirectionalSoundInstance(
				PING_SOUND_EVENT,
				SoundSource.MASTER,
				CLIENT_CONFIG.getPingVolume() / 100f,
				1f,
				this.pos
			)
		);
	}

	public boolean isExpired() {
		return this.age > CLIENT_CONFIG.getPingDuration() * TPS && CLIENT_CONFIG.getPingDuration() < MAX_PING_DURATION;
	}

	public boolean isRemovable() {
		return (CLIENT_CONFIG.getCorrectionPeriod() >= MAX_CORRECTION_PERIOD || this.age > CLIENT_CONFIG.getCorrectionPeriod() * TPS) && this.distanceToCenter() < CLIENT_CONFIG.getRemoveRadius();
	}

	public float distanceToCenter() {
		if (this.screenPos == null) {
			return 0f;
		}

		final var wnd = Game.getWindow();
		final var center = new Vec2(wnd.getGuiScaledWidth() * 0.5f, wnd.getGuiScaledHeight() * 0.5f);

		return this.screenPos.distanceTo(center);
	}

	public boolean isCloserToCenter(@Nullable PingView b) {
		if (b == null) {
			return true;
		}

		return this.distanceToCenter() < b.distanceToCenter();
	}

	public int getTeamColor() {
		if (this.playerInfo == null || this.playerInfo.getTeam() == null) return WHITE;

		return this.playerInfo.getTeam().getColor()
			.map(teamColor -> (255 << 24) | teamColor.rgb())
			.orElse(WHITE);
	}

	private void calculateScale() {
		final var scale = 2.0 / Math.pow(distance, 0.3);
		final var pingSize = CLIENT_CONFIG.getPingSize() / 100f;

		this.scale = (float)Math.max(1.0, scale) * 0.5f * pingSize;
	}
}
