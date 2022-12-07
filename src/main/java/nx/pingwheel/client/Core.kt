package nx.pingwheel.client

import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Matrix4f
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3f
import net.minecraft.world.RaycastContext
import nx.pingwheel.client.util.ConLog
import nx.pingwheel.client.util.Game
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

object Core {

	private const val REACH_DISTANCE = 256.0
	private const val HIT_FLUIDS = false

	private var queuePing = false

	@JvmStatic
	fun doPing() {
		ConLog.info("key.ping-wheel.ping pressed")
		queuePing = true
	}

	private fun map(
		anglePerPixel: Float, center: Vec3d, horizontalRotationAxis: Vec3f,
		verticalRotationAxis: Vec3f, x: Int, y: Int, width: Int, height: Int
	): Vec3d {
		val horizontalRotation = (x - width / 2f) * anglePerPixel
		val verticalRotation = (y - height / 2f) * anglePerPixel
		val temp2 = Vec3f(center)
		temp2.rotate(verticalRotationAxis.getDegreesQuaternion(verticalRotation))
		temp2.rotate(horizontalRotationAxis.getDegreesQuaternion(horizontalRotation))

		return Vec3d(temp2)
	}

	private fun castRayDirectional(direction: Vec3d, tickDelta: Float): HitResult? {
		val cameraEntity = Game.cameraEntity

		if (cameraEntity == null || Game.world == null) {
			return null
		}

		val rayStartVec = cameraEntity.getCameraPosVec(tickDelta)
		val rayEndVec = rayStartVec.add(direction.multiply(REACH_DISTANCE))
		val boundingBox = cameraEntity
			.boundingBox
			.stretch(cameraEntity.getRotationVec(1.0f).multiply(REACH_DISTANCE))
			.expand(1.0, 1.0, 1.0)

		val blockHitResult = cameraEntity.world.raycast(RaycastContext(
			rayStartVec,
			rayEndVec,
			RaycastContext.ShapeType.OUTLINE,
			if (HIT_FLUIDS) RaycastContext.FluidHandling.ANY else RaycastContext.FluidHandling.NONE,
			cameraEntity,
		))

		return ProjectileUtil.raycast(
			cameraEntity,
			rayStartVec,
			rayEndVec,
			boundingBox,
			{ targetEntity -> !targetEntity.isSpectator },
			REACH_DISTANCE,
		) ?: return blockHitResult
	}

	@JvmStatic
	fun onRenderWorld(stack: MatrixStack, projectionMatrix: Matrix4f, tickDelta: Float) {
		if (!queuePing) {
			return
		}

		queuePing = false

		val cameraEntity = Game.cameraEntity ?: return

		val scaledWindow = Vec2f(Game.window.scaledWidth.toFloat(), Game.window.scaledHeight.toFloat())
		val cameraDirection = cameraEntity.getRotationVec(tickDelta)
		val fov = Game.options.fov.value
		val angleSize = fov/scaledWindow.y

		var verticalRotationAxis = Vec3f(cameraDirection)
		verticalRotationAxis.cross(Vec3f.POSITIVE_Y)

		if (!verticalRotationAxis.normalize()) {
			return
		}

		val horizontalRotationAxis = Vec3f(cameraDirection)
		horizontalRotationAxis.cross(verticalRotationAxis)
		horizontalRotationAxis.normalize()

		verticalRotationAxis = Vec3f(cameraDirection)
		verticalRotationAxis.cross(horizontalRotationAxis)

		val direction = map(
			angleSize,
			cameraDirection,
			horizontalRotationAxis,
			verticalRotationAxis,
			(scaledWindow.x / 2f).toInt(),
			(scaledWindow.y / 2f).toInt(),
			scaledWindow.x.toInt(),
			scaledWindow.y.toInt(),
		)

		val hitResult = castRayDirectional(direction, tickDelta)

		// when (val hitResult = castDirectionalRay(direction, tickDelta)?.type) {
		when (hitResult?.type) {
			HitResult.Type.BLOCK -> {
				ConLog.info("HitResult.Type.BLOCK")
			}
			HitResult.Type.ENTITY -> {
				ConLog.info("HitResult.Type.ENTITY")
			}
			else -> {
				ConLog.info("HitResult.Type.MISS")
			}
		}
	}

	@JvmStatic
	fun onRenderGUI(stack: MatrixStack, ci: CallbackInfo) {

	}
}
