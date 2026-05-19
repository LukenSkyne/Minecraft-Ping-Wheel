package nx.pingwheel.common.core;

import lombok.Getter;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.integration.ModContext;
import nx.pingwheel.common.math.Raycast;
import nx.pingwheel.common.network.PingLocationC2SPacket;
import nx.pingwheel.common.platform.IPlatformNetworkService;

import java.util.UUID;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.config.ClientConfig.MAX_CORRECTION_PERIOD;
import static nx.pingwheel.common.config.ClientConfig.TPS;

public class PingController {
	private PingController() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();

	@Getter
	private static boolean pingQueued = false;
	private static int pingSequence = 0;
	private static int lastPing = 0;

	public static void revokePingAction() {
		pingQueued = false;
	}

	public static void queuePingAction() {
		pingQueued = true;
	}

	public static void pollPingAction(float tickDelta) {
		if (!pingQueued || Game.level == null) {
			return;
		}

		var time = (int)Game.level.getGameTime();

		if (CLIENT_CONFIG.getCorrectionPeriod() < MAX_CORRECTION_PERIOD && time - lastPing > CLIENT_CONFIG.getCorrectionPeriod() * TPS) {
			++pingSequence;
		}

		lastPing = time;
		pingQueued = false;
		performPingAction(tickDelta);
	}

	private static void performPingAction(float tickDelta) {
		var cameraEntity = Game.getCameraEntity();

		if (cameraEntity == null || Game.level == null) {
			return;
		}

		var cameraDirection = cameraEntity.getViewVector(tickDelta);
		var hitResult = Raycast.traceDirectional(
			cameraDirection,
			tickDelta,
			Math.min(CLIENT_CONFIG.getRaycastDistance(), CLIENT_CONFIG.getPingDistance()),
			cameraEntity.isCrouching());

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			if (ModContext.HasVoxy) {
				var voxyHitResult = Raycast.traceVoxy(cameraDirection, tickDelta, CLIENT_CONFIG.getPingDistance());
				if (voxyHitResult != null && voxyHitResult.getType() != HitResult.Type.MISS) {
					IPlatformNetworkService.INSTANCE.sendToServer(new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), voxyHitResult.getLocation(), null, pingSequence, GameContext.getDimension()));
					return;
				}
			}
			if (ModContext.HasDistantHorizons) {
				Raycast.traceDistantAsync(cameraDirection, tickDelta, (distantHitResult) -> {
					IPlatformNetworkService.INSTANCE.sendToServer(new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), distantHitResult.getLocation(), null, pingSequence, GameContext.getDimension()));
				});
			}

			return;
		}

		UUID uuid = null;

		if (hitResult.getType() == HitResult.Type.ENTITY) {
			uuid = ((EntityHitResult)hitResult).getEntity().getUUID();
		}

		IPlatformNetworkService.INSTANCE.sendToServer(new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), hitResult.getLocation(), uuid, pingSequence, GameContext.getDimension()));
	}
}
