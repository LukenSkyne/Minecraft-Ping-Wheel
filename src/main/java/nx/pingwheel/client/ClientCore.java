package nx.pingwheel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec2f;
import nx.pingwheel.client.config.Config;
import nx.pingwheel.client.helper.Draw;
import nx.pingwheel.client.helper.MathUtils;
import nx.pingwheel.client.helper.PingData;
import nx.pingwheel.client.helper.Raycast;
import nx.pingwheel.shared.network.PingLocationPacketC2S;
import nx.pingwheel.shared.network.PingLocationPacketS2C;

import java.util.ArrayList;
import java.util.UUID;

import static nx.pingwheel.client.PingWheelClient.*;

@Environment(EnvType.CLIENT)
public class ClientCore {

	private static final double REACH_DISTANCE = 1024.0;
	private static final int TPS = 20;

	private static final Config Config = ConfigHandler.getConfig();
	private static final ArrayList<PingData> pingRepo = new ArrayList<>();
	private static boolean queuePing = false;
	private static ClientWorld lastWorld = null;
	private static int lastPing = 0;
	private static int pingSequence = 0;

	public static void markLocation() {
		queuePing = true;
	}

	public static void onPingLocation(PacketByteBuf packet) {
		var pingLocationPacket = PingLocationPacketS2C.parse(packet);

		if (pingLocationPacket.isEmpty() || Game.player == null || Game.world == null) {
			return;
		}

		var pingLocation = pingLocationPacket.get();

		if (!pingLocation.getChannel().equals(Config.getChannel())) {
			return;
		}

		if (Config.getPingDistance() < 2048) {
			var vecToPing = Game.player.getPos().relativize(pingLocation.getPos());

			if (vecToPing.length() > Config.getPingDistance()) {
				return;
			}
		}

		Game.execute(() -> {
			addOrReplacePing(new PingData(
				pingLocation.getPos(),
				pingLocation.getEntity(),
				pingLocation.getAuthor(),
				pingLocation.getSequence(),
				(int)Game.world.getTime()
			));

			Game.getSoundManager().play(
				new DirectionalSoundInstance(
					PING_SOUND_EVENT,
					SoundCategory.MASTER,
					Config.getPingVolume() / 100f,
					1f,
					pingLocation.getPos()
				)
			);
		});
	}

	public static void onRenderWorld(MatrixStack matrixStack,
									 Matrix4f projectionMatrix,
									 float tickDelta) {
		if (Game.world == null) {
			return;
		}

		if (lastWorld != Game.world) {
			lastWorld = Game.world;
			pingRepo.clear();
		}

		var time = (int)Game.world.getTime();

		if (queuePing) {
			if (time - lastPing > Config.getCorrectionPeriod() * TPS) {
				++pingSequence;
			}

			lastPing = time;
			queuePing = false;
			executePing(tickDelta);
		}

		var modelViewMatrix = matrixStack.peek().getPositionMatrix();

		for (var ping : pingRepo) {
			if (ping.getUuid() != null) {
				var ent = getEntity(ping.getUuid());

				if (ent != null) {
					if (ent.getType() == EntityType.ITEM && Config.isItemIconVisible()) {
						ping.itemStack = ((ItemEntity)ent).getStack().copy();
					}

					ping.setPos(ent.getLerpedPos(tickDelta).add(0, ent.getBoundingBox().getYLength(), 0));
				}
			}

			ping.screenPos = MathUtils.project3Dto2D(ping.getPos(), modelViewMatrix, projectionMatrix);
			ping.aliveTime = time - ping.getSpawnTime();
		}

		pingRepo.removeIf(p -> p.aliveTime > Config.getPingDuration() * TPS);
	}

	public static void onRenderGUI(MatrixStack m, float tickDelta) {
		if (Game.player == null) {
			return;
		}

		for (var ping : pingRepo) {
			var uiScale = (float)Game.getWindow().getScaleFactor();
			var uiScaleAdjustment = uiScale * 2f / 5f;

			if (ping.screenPos == null) {
				continue;
			}

			var pos = ping.screenPos;
			var cameraPosVec = Game.player.getCameraPosVec(tickDelta);
			var distanceToPing = (float)cameraPosVec.distanceTo(ping.getPos());
			var pingScale = getDistanceScale(distanceToPing) / uiScale * uiScaleAdjustment;

			var white = ColorHelper.Argb.getArgb(255, 255, 255, 255);
			var shadowBlack = ColorHelper.Argb.getArgb(64, 0, 0, 0);

			m.push();
			m.translate((pos.getX() / uiScale), (pos.getY() / uiScale), 0);
			m.scale(pingScale, pingScale, 1f);

			var text = String.format("%.1fm", distanceToPing);
			var textMetrics = new Vec2f(
				Game.textRenderer.getWidth(text),
				Game.textRenderer.fontHeight
			);
			var textOffset = textMetrics.multiply(-0.5f).add(new Vec2f(0f, textMetrics.y * -1.5f));

			m.push();
			m.translate(textOffset.x, textOffset.y, 0);
			DrawableHelper.fill(m, -2, -2, (int)textMetrics.x + 1, (int)textMetrics.y, shadowBlack);
			Game.textRenderer.draw(m, text, 0f, 0f, white);
			m.pop();

			if (ping.itemStack != null && Config.isItemIconVisible()) {
				var model = Game.getItemRenderer().getModel(ping.itemStack, null, null, 0);

				Draw.renderGuiItemModel(
					ping.itemStack,
					(pos.getX() / uiScale),
					(pos.getY() / uiScale),
					model,
					pingScale * 2 / 3
				);
			} else {
				MathUtils.rotateZ(m, (float)(Math.PI / 4f));
				m.translate(-2.5, -2.5, 0);
				DrawableHelper.fill(m, 0, 0, 5, 5, white);
			}

			m.pop();
		}
	}

	private static void executePing(float tickDelta) {
		var cameraEntity = Game.cameraEntity;

		if (cameraEntity == null) {
			return;
		}

		var cameraDirection = cameraEntity.getRotationVec(tickDelta);
		var hitResult = Raycast.traceDirectional(
			cameraDirection,
			tickDelta,
			Math.min(REACH_DISTANCE, Config.getPingDistance()),
			cameraEntity.isSneaking());

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			return;
		}

		UUID uuid = null;

		if (hitResult.getType() == HitResult.Type.ENTITY) {
			uuid = ((EntityHitResult)hitResult).getEntity().getUuid();
		}

		new PingLocationPacketC2S(Config.getChannel(), hitResult.getPos(), uuid, pingSequence).send();
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
		if (Game.world == null) {
			return null;
		}

		for (var entity : Game.world.getEntities()) {
			if (entity.getUuid().equals(uuid)) {
				return entity;
			}
		}

		return null;
	}

	private static float getDistanceScale(float distance) {
		var scaleMin = 1f;
		var scale = 2f / Math.pow(distance, 0.3f);

		return (float)Math.max(scaleMin, scale);
	}
}
