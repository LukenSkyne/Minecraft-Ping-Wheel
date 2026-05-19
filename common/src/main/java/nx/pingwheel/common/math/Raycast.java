package nx.pingwheel.common.math;


import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nx.pingwheel.common.CommonClient.Game;

public class Raycast {
	private Raycast() {}

	private static IDhApiTerrainDataCache terrainCache = null;
	private static Instant lastCacheLoad = Instant.EPOCH;

	public static void traceDistantAsync(Vec3 direction, float tickDelta, Consumer<BlockHitResult> callback) {
		final var cameraEntity = Game.getCameraEntity();

		if (cameraEntity == null || cameraEntity.level() == null) {
			return;
		}

		final var rayStartVec = cameraEntity.getEyePosition(tickDelta);

		final var ft = CompletableFuture.supplyAsync(() -> {
			if (DhApi.Delayed.worldProxy == null) {
				return null;
			}

			final var levelWrapper = DhApi.Delayed.worldProxy.getSinglePlayerLevel();

			if (levelWrapper == null) {
				return null;
			}

			if (terrainCache == null || Duration.between(lastCacheLoad, Instant.now()).getSeconds() > 10) {
				terrainCache = DhApi.Delayed.terrainRepo.createSoftCache();
				lastCacheLoad = Instant.now();
			}

			final var rayCastResult = DhApi.Delayed.terrainRepo.raycast(
				levelWrapper,
				rayStartVec.x, rayStartVec.y, rayStartVec.z,
				(float)direction.x, (float)direction.y, (float)direction.z,
				4096,
				terrainCache
			);

			if (!rayCastResult.success || rayCastResult.payload == null) {
				return null;
			}

			final var pos = new Vec3(rayCastResult.payload.pos.x, rayCastResult.payload.pos.y, rayCastResult.payload.pos.z);

			return new BlockHitResult(pos, Direction.UP, new BlockPos((int)pos.x, (int)pos.y, (int)pos.z), true);
		});

		ft.thenAccept(result -> Optional.ofNullable(result).ifPresent(callback));
	}

	public static BlockHitResult traceVoxy(Vec3 direction, float tickDelta, double maxDistance) {
		try {
			var cameraEntity = Game.getCameraEntity();

			if (cameraEntity == null || cameraEntity.level() == null) {
				return null;
			}

			var rayStartVec = cameraEntity.getEyePosition(tickDelta);

			var apiClass = Class.forName("me.cortex.voxy.client.api.VoxyRaycastAPI");
			var raycastMethod = apiClass.getMethod("raycast",
				net.minecraft.world.level.Level.class,
				Vec3.class,
				Vec3.class,
				double.class);
			var result = raycastMethod.invoke(null,
				cameraEntity.level(),
				rayStartVec,
				direction,
				maxDistance);

			if (result == null) {
				return null;
			}

			var resultClass = result.getClass();
			var pos = (BlockPos) resultClass.getMethod("blockPos").invoke(result);
			var hitLoc = (Vec3) resultClass.getMethod("hitLocation").invoke(result);
			var face = (Direction) resultClass.getMethod("face").invoke(result);

			return new BlockHitResult(hitLoc, face, pos, true);
		} catch (Exception e) {
			return null;
		}
	}

	public static HitResult traceDirectional(Vec3 direction,
											 float tickDelta,
											 double maxDistance,
											 boolean hitTranslucent) {
		var cameraEntity = Game.getCameraEntity();

		if (cameraEntity == null || cameraEntity.level() == null) {
			return null;
		}

		var rayStartVec = cameraEntity.getEyePosition(tickDelta);
		var rayEndVec = rayStartVec.add(direction.scale(maxDistance));
		var boundingBox = cameraEntity
			.getBoundingBox()
			.expandTowards(cameraEntity.getViewVector(1.f).scale(maxDistance))
			.inflate(1.0, 1.0, 1.0);

		var blockHitResult = cameraEntity.level().clip(
			new ClipContext(
				rayStartVec,
				rayEndVec,
				hitTranslucent ? ClipContext.Block.OUTLINE : ClipContext.Block.VISUAL,
				hitTranslucent ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE,
				cameraEntity)
		);

		var entityHitResult = traceEntity(
			cameraEntity,
			rayStartVec,
			rayEndVec,
			boundingBox,
			(targetEntity) -> !targetEntity.isSpectator());

		if (entityHitResult == null) {
			return blockHitResult;
		}

		if (rayStartVec.distanceToSqr(blockHitResult.getLocation()) < rayStartVec.distanceToSqr(entityHitResult.getLocation())) {
			return blockHitResult;
		}

		return entityHitResult;
	}

	private static EntityHitResult traceEntity(Entity entity,
											   Vec3 min,
											   Vec3 max,
											   AABB box,
											   Predicate<Entity> predicate) {
		var minDist = min.distanceToSqr(max);
		EntityHitResult minHitResult = null;

		for (var ent : entity.level().getEntities(entity, box, predicate)) {
			var targetBoundingBox = ent.getBoundingBox()
				.inflate(ent.getPickRadius())
				.inflate(0.25);
			var hitPos = targetBoundingBox.clip(min, max);

			if (hitPos.isEmpty()) {
				continue;
			}

			var hitResult = new EntityHitResult(ent, hitPos.get());
			var hitDist = min.distanceToSqr(hitResult.getLocation());

			if (minDist > hitDist) {
				minDist = hitDist;
				minHitResult = hitResult;
			}
		}

		return minHitResult;
	}
}
