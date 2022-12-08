package nx.pingwheel.client

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.network.PacketByteBuf
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import net.minecraft.world.RaycastContext
import nx.pingwheel.client.util.Game
import nx.pingwheel.client.util.Math
import nx.pingwheel.client.util.rotateZ
import nx.pingwheel.shared.Constants
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.PI
import kotlin.math.pow

object Core {

	private const val REACH_DISTANCE = 256.0

	private var queuePing = false
	private var pingRepo = mutableListOf<PingData>()

	@JvmStatic
	fun markLocation() {
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

	private fun castRayDirectional(direction: Vec3d, tickDelta: Float, hitFluids: Boolean): HitResult? {
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

		val blockHitResult = cameraEntity.world.raycast(
			RaycastContext(
				rayStartVec,
				rayEndVec,
				RaycastContext.ShapeType.OUTLINE,
				if (hitFluids) RaycastContext.FluidHandling.ANY else RaycastContext.FluidHandling.NONE,
				cameraEntity,
			)
		)

		return ProjectileUtil.raycast(
			cameraEntity,
			rayStartVec,
			rayEndVec,
			boundingBox,
			{ targetEntity -> !targetEntity.isSpectator },
			REACH_DISTANCE,
		) ?: return blockHitResult
	}

	private fun processPing(tickDelta: Float) {
		if (!queuePing) {
			return
		}

		queuePing = false
		val cameraEntity = Game.cameraEntity ?: return

		val scaledWindow = Vec2f(Game.window.scaledWidth.toFloat(), Game.window.scaledHeight.toFloat())
		val cameraDirection = cameraEntity.getRotationVec(tickDelta)
		val fov = Game.options.fov.value
		val angleSize = fov / scaledWindow.y

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

		val hitResult = castRayDirectional(direction, tickDelta, cameraEntity.isSneaking)
		val pingPos = when (hitResult?.type) {
			HitResult.Type.BLOCK -> {
				hitResult.pos
			}

			HitResult.Type.ENTITY -> {
				hitResult.pos
			}

			else -> {
				null
			}
		}

		if (pingPos != null) {
			val packet = PacketByteBufs.create()
			packet.writeDouble(pingPos.x)
			packet.writeDouble(pingPos.y)
			packet.writeDouble(pingPos.z)

			ClientPlayNetworking.send(Constants.C2S_PING_LOCATION, packet)
		}
	}

	@JvmStatic
	fun onReceivePing(
		client: MinecraftClient,
		handler: ClientPlayNetworkHandler,
		buf: PacketByteBuf,
		responseSender: PacketSender
	) {
		val pingPos = Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())

		client.execute {
			pingRepo.add(PingData(pingPos))
		}
	}

	@JvmStatic
	fun onRenderWorld(stack: MatrixStack, projectionMatrix: Matrix4f, tickDelta: Float) {
		val modelViewMatrix = stack.peek().positionMatrix

		processPing(tickDelta)

		for (ping in pingRepo) {
			ping.screenPos = Math.project3Dto2D(ping.pos, modelViewMatrix, projectionMatrix)
			ping.lifeTime -= tickDelta
		}

		pingRepo.removeIf { p -> p.lifeTime < 0 }
	}

	@JvmStatic
	fun onRenderGUI(stack: MatrixStack, ci: CallbackInfo) {
		for (ping in pingRepo) {
			val pingPosScreen = ping.screenPos ?: continue

			val cameraPosVec = Game.player?.getCameraPosVec(Game.tickDelta)
			val distanceToPing = cameraPosVec?.distanceTo(ping.pos) ?: 0.0
			val pingScaleMin = 0.25f

			var pingScale = (1 / (distanceToPing.pow(0.3))).toFloat()

			if (pingScale < pingScaleMin)
				pingScale = pingScaleMin

			stack.push() // push
			val uiScale = Game.window.scaleFactor
			stack.translate(
				pingPosScreen.x / uiScale,
				pingPosScreen.y / uiScale,
				0.0
			)

			stack.scale(pingScale, pingScale, 1f)

			val white = ColorHelper.Argb.getArgb(255, 255, 255, 255)
			val shadowBlack = ColorHelper.Argb.getArgb(64, 0, 0, 0)

			stack.push() // push text
			val text = "%.1fm".format(distanceToPing)
			val textWidth: Int = Game.textRenderer.getWidth(text)
			val textHeight: Int = Game.textRenderer.fontHeight
			stack.translate((-textWidth * 0.5f).toDouble(), (-textHeight * 2.5f).toDouble(), 0.0)
			DrawableHelper.fill(stack, -2, -2, textWidth + 1, textHeight, shadowBlack)
			Game.textRenderer.draw(stack, text, 0f, 0f, white)
			stack.pop() // pop text

			stack.rotateZ(PI.toFloat() / 4f)
			stack.translate(
				-2.5,
				-2.5,
				0.0
			)
			DrawableHelper.fill(stack, 0, 0, 5, 5, white)
			stack.pop() // pop
		}
	}
}
