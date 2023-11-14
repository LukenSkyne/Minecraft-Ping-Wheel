package nx.pingwheel.common.core;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Matrix4f;
import nx.pingwheel.common.config.Config;
import nx.pingwheel.common.helper.Draw;
import nx.pingwheel.common.helper.MathUtils;
import nx.pingwheel.common.helper.PingData;
import nx.pingwheel.common.helper.Raycast;
import nx.pingwheel.common.networking.PingLocationPacketC2S;
import nx.pingwheel.common.networking.PingLocationPacketS2C;
import nx.pingwheel.common.sound.DirectionalSoundInstance;

import java.util.ArrayList;
import java.util.UUID;

import static nx.pingwheel.common.ClientGlobal.*;

@Environment(EnvType.CLIENT)
public class ClientCore {
	private ClientCore() {}

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

		processPings(matrixStack, projectionMatrix, tickDelta, time);
	}

	public static void onRenderGUI(MatrixStack m, float tickDelta) {
		if (Game.player == null) {
			return;
		}

		for (var ping : pingRepo) {
			if (ping.screenPos == null || ping.screenPos.getW() <= 0) {
				continue;
			}

			var pos = ping.screenPos;
			var cameraPosVec = Game.player.getCameraPosVec(tickDelta);
			var distanceToPing = (float)cameraPosVec.distanceTo(ping.getPos());
			var pingSize = Config.getPingSize() / 100f;
			var pingScale = getDistanceScale(distanceToPing) * pingSize * 0.4f;

			m.push();
			m.translate(pos.getX(), pos.getY(), 0);
			m.scale(pingScale, pingScale, 1f);

			var text = String.format("%.1fm", distanceToPing);
			Draw.renderLabel(m, text);

			Draw.renderPing(m, ping.itemStack, Config.isItemIconVisible());

			m.pop();
		}
	}

	private static void processPings(MatrixStack matrixStack, Matrix4f projectionMatrix, float tickDelta, int time) {
		var modelViewMatrix = matrixStack.peek().getPositionMatrix();

		pingRepo.removeIf(p -> p.aliveTime > Config.getPingDuration() * TPS);

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
			Math.min(Config.getRaycastDistance(), Config.getPingDistance()),
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
