package nx.pingwheel.client.util

import net.minecraft.entity.Entity
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import java.util.function.Predicate

object RayCasting {

	@JvmStatic
	fun traceEntity(
		entity: Entity,
		min: Vec3d,
		max: Vec3d,
		box: Box,
		predicate: Predicate<Entity>,
	): EntityHitResult? {
		var minDist = min.squaredDistanceTo(max)
		var minHitResult: EntityHitResult? = null

		for (ent in entity.world.getOtherEntities(entity, box, predicate)) {
			val targetBoundingBox = ent.boundingBox
				.expand(ent.targetingMargin.toDouble())
				.expand(0.25)
			val hitPos = targetBoundingBox.raycast(min, max)

			if (!hitPos.isPresent)
				continue

			val hitResult = EntityHitResult(ent, hitPos.get())
			val hitDist = min.squaredDistanceTo(hitResult.pos)

			if (minDist > hitDist) {
				minDist = hitDist
				minHitResult = hitResult
			}
		}

		return minHitResult
	}

	@JvmStatic
	fun traceDirectional(direction: Vec3d, tickDelta: Float, maxDistance: Double, hitFluids: Boolean): HitResult? {
		val cameraEntity = Game.cameraEntity

		if (cameraEntity?.world == null) {
			return null
		}

		val rayStartVec = cameraEntity.getCameraPosVec(tickDelta)
		val rayEndVec = rayStartVec.add(direction.multiply(maxDistance))
		val boundingBox = cameraEntity
			.boundingBox
			.stretch(cameraEntity.getRotationVec(1.0f).multiply(maxDistance))
			.expand(1.0, 1.0, 1.0)

		val blockHitResult = cameraEntity.world.raycast(
			RaycastContext(
				rayStartVec,
				rayEndVec,
				RaycastContext.ShapeType.OUTLINE,
				if (hitFluids) RaycastContext.FluidHandling.ANY else RaycastContext.FluidHandling.NONE,
				cameraEntity,
			)
		)

		val entityHitResult = traceEntity(
			cameraEntity,
			rayStartVec,
			rayEndVec,
			boundingBox,
		) { targetEntity -> !targetEntity.isSpectator } ?: blockHitResult

		if (rayStartVec.squaredDistanceTo(entityHitResult.pos) < rayStartVec.squaredDistanceTo(blockHitResult.pos)) {
			return entityHitResult
		}

		return blockHitResult
	}
}
