package nx.pingwheel.client.helper;

import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.function.Predicate;

import static nx.pingwheel.client.PingWheelClient.Game;

public class Raycast {

	public static HitResult traceDirectional(Vec3d direction,
											 float tickDelta,
											 double maxDistance,
											 boolean hitFluids) {
		var cameraEntity = Game.cameraEntity;

		if (cameraEntity == null || cameraEntity.getWorld() == null) {
			return null;
		}

		var rayStartVec = cameraEntity.getCameraPosVec(tickDelta);
		var rayEndVec = rayStartVec.add(direction.multiply(maxDistance));
		var boundingBox = cameraEntity
			.getBoundingBox()
			.stretch(cameraEntity.getRotationVec(1.f).multiply(maxDistance))
			.expand(1.0, 1.0, 1.0);

		var blockHitResult = cameraEntity.getWorld().raycast(
			new RaycastContext(
				rayStartVec,
				rayEndVec,
				RaycastContext.ShapeType.OUTLINE,
				hitFluids ? RaycastContext.FluidHandling.ANY : RaycastContext.FluidHandling.NONE,
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

		if (rayStartVec.squaredDistanceTo(blockHitResult.getPos()) < rayStartVec.squaredDistanceTo(entityHitResult.getPos())) {
			return blockHitResult;
		}

		return entityHitResult;
	}

	private static EntityHitResult traceEntity(Entity entity,
											   Vec3d min,
											   Vec3d max,
											   Box box,
											   Predicate<Entity> predicate) {
		var minDist = min.squaredDistanceTo(max);
		EntityHitResult minHitResult = null;

		for (var ent : entity.getWorld().getOtherEntities(entity, box, predicate)) {
			var targetBoundingBox = ent.getBoundingBox()
				.expand(ent.getTargetingMargin())
				.expand(0.25);
			var hitPos = targetBoundingBox.raycast(min, max);

			if (hitPos.isEmpty()) {
				continue;
			}

			var hitResult = new EntityHitResult(ent, hitPos.get());
			var hitDist = min.squaredDistanceTo(hitResult.getPos());

			if (minDist > hitDist) {
				minDist = hitDist;
				minHitResult = hitResult;
			}
		}

		return minHitResult;
	}
}
