package nx.pingwheel.client

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.sound.SoundCategory
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.*
import nx.pingwheel.PingWheel
import nx.pingwheel.client.util.Game
import nx.pingwheel.client.util.Math
import nx.pingwheel.client.util.RayCasting
import nx.pingwheel.client.util.rotateZ
import nx.pingwheel.shared.Constants
import nx.pingwheel.shared.DirectionalSoundInstance
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.pow

object Core {

	private const val REACH_DISTANCE = 256.0
	private const val PING_LIFETIME = 140 // 7 seconds in Game Ticks

	private var queuePing = false
	private var pingRepo = mutableListOf<PingData>()

	@JvmStatic
	fun markLocation() {
		queuePing = true
	}

	private fun processPing(tickDelta: Float) {
		if (!queuePing) {
			return
		}

		queuePing = false
		val cameraEntity = Game.cameraEntity ?: return
		val cameraDirection = cameraEntity.getRotationVec(tickDelta)
		val hitResult = RayCasting.traceDirectional(cameraDirection, tickDelta, REACH_DISTANCE, cameraEntity.isSneaking)

		if (hitResult == null || hitResult.type == HitResult.Type.MISS) {
			return
		}

		val packet = PacketByteBufs.create()
		packet.writeDouble(hitResult.pos.x)
		packet.writeDouble(hitResult.pos.y)
		packet.writeDouble(hitResult.pos.z)

		if (hitResult.type == HitResult.Type.ENTITY) {
			packet.writeBoolean(true)
			packet.writeUuid((hitResult as EntityHitResult).entity.uuid)
		} else {
			packet.writeBoolean(false)
		}

		ClientPlayNetworking.send(Constants.C2S_PING_LOCATION, packet)
	}

	private fun getDistanceScale(distance: Float): Float {
		val scaleMin = 1f
		val scale = 2f / distance.pow(0.3f)

		return max(scaleMin, scale)
	}

	@JvmStatic
	fun onReceivePing(
		client: MinecraftClient,
		handler: ClientPlayNetworkHandler,
		buf: PacketByteBuf,
		responseSender: PacketSender
	) {
		val pingPos = Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())
		val uuid = if (buf.readBoolean()) buf.readUuid() else null

		client.execute {
			pingRepo.add(PingData(pingPos, uuid, Game.world?.time?.toInt() ?: 0))

			Game.soundManager.play(
				DirectionalSoundInstance(
					PingWheel.PING_SOUND_EVENT,
					SoundCategory.VOICE,
					1f,
					1f,
					0,
					pingPos,
				)
			)
		}
	}

	@JvmStatic
	fun onRenderWorld(stack: MatrixStack, projectionMatrix: Matrix4f, tickDelta: Float) {
		val world = Game.world ?: return
		val modelViewMatrix = stack.peek().positionMatrix

		processPing(tickDelta)

		val time = world.time.toInt()

		for (ping in pingRepo) {
			if (ping.uuid != null) {
				val ent = world.entities.find { entity -> entity.uuid == ping.uuid }

				if (ent != null) {
					ping.pos = ent.getLerpedPos(tickDelta).add(0.0, ent.boundingBox.yLength, 0.0)
				}
			}

			ping.screenPos = Math.project3Dto2D(ping.pos, modelViewMatrix, projectionMatrix)
			ping.aliveTime = time - ping.spawnTime
		}

		pingRepo.removeIf { p -> p.aliveTime!! > PING_LIFETIME }
	}

	@JvmStatic
	fun onRenderGUI(stack: MatrixStack, ci: CallbackInfo) {
		for (ping in pingRepo) {
			val uiScale = Game.window.scaleFactor
			val uiScaleAdjustment = Math.mapValue(uiScale.toFloat(), 1f, 5f, 1f, 2f)

			val pingPosScreen = ping.screenPos ?: continue
			val cameraPosVec = Game.player?.getCameraPosVec(Game.tickDelta)
			val distanceToPing = cameraPosVec?.distanceTo(ping.pos)?.toFloat() ?: 0f
			val pingScale = getDistanceScale(distanceToPing) / uiScale.toFloat() * uiScaleAdjustment

			val white = ColorHelper.Argb.getArgb(255, 255, 255, 255)
			val shadowBlack = ColorHelper.Argb.getArgb(64, 0, 0, 0)

			stack.push() // push

			stack.translate((pingPosScreen.x / uiScale), (pingPosScreen.y / uiScale), 0.0)
			stack.scale(pingScale, pingScale, 1f)

			stack.push() // push text

			val text = "%.1fm".format(distanceToPing)
			val textMetrics = Vec2f(
				Game.textRenderer.getWidth(text).toFloat(),
				Game.textRenderer.fontHeight.toFloat()
			)
			val textOffset = textMetrics.multiply(-0.5f).add(Vec2f(0f, textMetrics.y * -1.5f))

			stack.translate(textOffset.x.toDouble(), textOffset.y.toDouble(), 0.0)

			DrawableHelper.fill(stack, -2, -2, textMetrics.x.toInt() + 1, textMetrics.y.toInt(), shadowBlack)
			Game.textRenderer.draw(stack, text, 0f, 0f, white)
			stack.pop() // pop text

			stack.rotateZ(PI.toFloat() / 4f)
			stack.translate(-2.5, -2.5, 0.0)
			DrawableHelper.fill(stack, 0, 0, 5, 5, white)
			stack.pop() // pop
		}
	}
}
