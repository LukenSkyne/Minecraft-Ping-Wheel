package nx.pingwheel.common.core;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.helper.Draw;
import nx.pingwheel.common.helper.MathUtils;
import nx.pingwheel.common.helper.PingData;
import nx.pingwheel.common.helper.Raycast;
import nx.pingwheel.common.networking.PingLocationC2SPacket;
import nx.pingwheel.common.networking.PingLocationS2CPacket;
import nx.pingwheel.common.sound.DirectionalSoundInstance;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.UUID;

import static nx.pingwheel.common.ClientGlobal.*;
import static nx.pingwheel.common.Global.LOGGER;
import static nx.pingwheel.common.Global.NetHandler;

public class ClientCore {
	private ClientCore() {}

	private static final int TPS = 20;

	private static final ClientConfig Config = ConfigHandler.getConfig();
	private static final ArrayList<PingData> pingRepo = new ArrayList<>();
	private static boolean queuePing = false;
	private static ClientLevel lastWorld = null;
	private static int dimension = 0;
	private static int lastPing = 0;
	private static int pingSequence = 0;

	public static void markLocation() {
		queuePing = true;
	}

	public static void onPingLocation(PingLocationS2CPacket packet) {
		if (packet.isCorrupt()) {
			LOGGER.warn("received invalid ping location from server");
			return;
		}

		if (Game.player == null || Game.level == null) {
			return;
		}

		if (!packet.channel().equals(Config.getChannel())) {
			return;
		}

		if (Config.getPingDistance() < 2048) {
			var vecToPing = Game.player.position().vectorTo(packet.pos());

			if (vecToPing.length() > Config.getPingDistance()) {
				return;
			}
		}

		final var authorPlayer = Game.player.connection.getPlayerInfo(packet.author());
		final var authorName = authorPlayer != null ? authorPlayer.getProfile().getName() : "";

		Game.execute(() -> {
			addOrReplacePing(new PingData(
				packet.pos(),
				packet.entity(),
				packet.author(),
				authorName,
				packet.sequence(),
				packet.dimension(),
				(int)Game.level.getGameTime()
			));

			if (packet.dimension() == dimension) {
				Game.getSoundManager().play(
					new DirectionalSoundInstance(
						PING_SOUND_EVENT,
						SoundSource.MASTER,
						Config.getPingVolume() / 100f,
						1f,
						packet.pos()
					)
				);
			}
		});
	}

	public static void onRenderWorld(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta) {
		if (Game.level == null) {
			return;
		}

		if (lastWorld != Game.level) {
			lastWorld = Game.level;
			dimension = lastWorld.dimension().location().hashCode();
		}

		var time = (int)Game.level.getGameTime();

		if (queuePing) {
			if (time - lastPing > Config.getCorrectionPeriod() * TPS) {
				++pingSequence;
			}

			lastPing = time;
			queuePing = false;
			executePing(tickDelta);
		}

		processPings(modelViewMatrix, projectionMatrix, tickDelta, time);
	}

	public static void onRenderGUI(GuiGraphics ctx, float tickDelta) {
		if (Game.player == null || pingRepo.isEmpty()) {
			return;
		}

		var m = ctx.pose();

		var wnd = Game.getWindow();
		var screenBounds = new Vec3(wnd.getGuiScaledWidth(), wnd.getGuiScaledHeight(), 0);
		var safeZoneTopLeft = new Vec2(Config.getSafeZoneLeft(), Config.getSafeZoneTop());
		var safeZoneBottomRight = new Vec2((float)screenBounds.x - Config.getSafeZoneRight(), (float)screenBounds.y - Config.getSafeZoneBottom());
		var safeScreenCentre = new Vec2((safeZoneBottomRight.x - safeZoneTopLeft.x) * 0.5f, (safeZoneBottomRight.y - safeZoneTopLeft.y) * 0.5f);
		final var showDirectionIndicator = Config.isDirectionIndicatorVisible();
		final var showNameLabels = Config.isNameLabelForced() || KEY_BINDING_NAME_LABELS.isDown();

		m.pushPose();
		m.translate(0f, 0f, -pingRepo.size());

		for (var ping : pingRepo) {
			if (ping.screenPos == null || (ping.screenPos.z <= 0 && !showDirectionIndicator) || ping.getDimension() != dimension) {
				continue;
			}

			m.translate(0f, 0f, 1f);

			var pos = ping.screenPos;
			var pingSize = Config.getPingSize() / 100f;
			var pingScale = getDistanceScale(ping.distance) * pingSize * 0.4f;

			var pingDirectionVec = new Vec2(pos.x - safeZoneTopLeft.x - safeScreenCentre.x, pos.y - safeZoneTopLeft.y - safeScreenCentre.y);
			var behindCamera = false;

			if (pos.z <= 0) {
				behindCamera = true;
				pingDirectionVec = pingDirectionVec.scale(-1);
			}

			var pingAngle = (float)Math.atan2(pingDirectionVec.y, pingDirectionVec.x);
			var isOffScreen = behindCamera || pos.x < 0 || pos.x > screenBounds.x || pos.y < 0 || pos.y > screenBounds.y;

			if (isOffScreen && showDirectionIndicator) {
				var indicator = MathUtils.calculateAngleRectIntersection(pingAngle, safeZoneTopLeft, safeZoneBottomRight);

				m.pushPose();
				m.translate(indicator.x, indicator.y, 0f);

				m.pushPose();
				m.scale(pingScale, pingScale, 1f);
				var indicatorOffsetX = Math.cos(pingAngle + Math.PI) * 12;
				var indicatorOffsetY = Math.sin(pingAngle + Math.PI) * 12;
				m.translate(indicatorOffsetX, indicatorOffsetY, 0);
				Draw.renderPing(ctx, ping.itemStack, Config.isItemIconVisible());
				m.popPose();

				m.pushPose();
				MathUtils.rotateZ(m, pingAngle);
				m.scale(pingSize, pingSize, 1f);

				m.scale(0.25f, 0.25f, 1f);
				m.translate(-5f, 0f, 0f);
				Draw.renderArrow(m, true);
				m.scale(0.9f, 0.9f, 1f);
				Draw.renderArrow(m, false);
				m.popPose();

				m.popPose();
			}

			if (!behindCamera) {
				m.pushPose();
				m.translate(pos.x, pos.y, 0);
				m.scale(pingScale, pingScale, 1f);

				var text = String.format("%,.1fm", ping.distance);
				Draw.renderLabel(ctx, text, -1.5f);
				Draw.renderPing(ctx, ping.itemStack, Config.isItemIconVisible());

				if (showNameLabels && !ping.getAuthor().equals(Game.player.getUUID())) {
					Draw.renderLabel(ctx, ping.getAuthorName(), 1.75f);
				}

				m.popPose();
			}
		}

		m.popPose();
	}

	private static void processPings(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta, int time) {
		if (Game.player == null || pingRepo.isEmpty()) {
			return;
		}

		var cameraPos = Game.player.getEyePosition(tickDelta);

		for (var ping : pingRepo) {
			if (ping.getUuid() != null) {
				var ent = getEntity(ping.getUuid());

				if (ent != null) {
					if (ent.getType() == EntityType.ITEM && Config.isItemIconVisible()) {
						ping.itemStack = ((ItemEntity)ent).getItem().copy();
					}

					ping.setPos(ent.getPosition(tickDelta).add(0, ent.getBoundingBox().getYsize(), 0));
				}
			}

			ping.distance = cameraPos.distanceTo(ping.getPos());
			ping.screenPos = MathUtils.worldToScreen(ping.getPos(), modelViewMatrix, projectionMatrix);
			ping.aliveTime = time - ping.getSpawnTime();
		}

		pingRepo.removeIf(p -> p.aliveTime > Config.getPingDuration() * TPS);
		pingRepo.sort((a, b) -> Double.compare(b.distance, a.distance));
	}

	private static void executePing(float tickDelta) {
		var cameraEntity = Game.cameraEntity;

		if (cameraEntity == null || Game.level == null) {
			return;
		}

		var cameraDirection = cameraEntity.getViewVector(tickDelta);
		var hitResult = Raycast.traceDirectional(
			cameraDirection,
			tickDelta,
			Math.min(Config.getRaycastDistance(), Config.getPingDistance()),
			cameraEntity.isCrouching());

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return;
		}

		UUID uuid = null;

		if (hitResult.getType() == HitResult.Type.ENTITY) {
			uuid = ((EntityHitResult)hitResult).getEntity().getUUID();
		}

		NetHandler.sendToServer(new PingLocationC2SPacket(Config.getChannel(), hitResult.getLocation(), uuid, pingSequence, dimension));
	}

	private static void addOrReplacePing(PingData newPing) {
		int index = -1;

		for (int i = 0; i < pingRepo.size(); i++) {
			var entry = pingRepo.get(i);

			if (entry.getAuthor().equals(newPing.getAuthor()) && entry.getSequence() == newPing.getSequence()) {
				index = i;
				break;
			}
		}

		if (index != -1) {
			pingRepo.set(index, newPing);
		} else {
			pingRepo.add(newPing);
		}
	}

	private static Entity getEntity(UUID uuid) {
		if (Game.level == null) {
			return null;
		}

		for (var entity : Game.level.entitiesForRendering()) {
			if (entity.getUUID().equals(uuid)) {
				return entity;
			}
		}

		return null;
	}

	private static float getDistanceScale(double distance) {
		var scale = 2.0 / Math.pow(distance, 0.3);

		return (float)Math.max(1.0, scale);
	}
}
