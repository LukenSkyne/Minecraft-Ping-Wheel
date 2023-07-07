package nx.pingwheel.client

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawableHelper
import net.minecraft.client.network.ClientPlayNetworkHandler
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.EntityType
import net.minecraft.entity.ItemEntity
import net.minecraft.network.PacketByteBuf
import net.minecraft.sound.SoundCategory
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import nx.pingwheel.client.util.*
import nx.pingwheel.shared.Constants
import nx.pingwheel.shared.DirectionalSoundInstance
import org.joml.Matrix4f
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object Core {

	private const val REACH_DISTANCE = 1024.0
	private const val TPS = 20

	private val config = PingWheelConfigHandler.getInstance().config
	private var pingRepo = mutableListOf<PingData>()
	private var queuePing = false

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
		val hitResult = RayCasting.traceDirectional(
			cameraDirection,
			tickDelta,
			min(REACH_DISTANCE, config.pingDistance.toDouble()),
			cameraEntity.isSneaking
		)

		if (hitResult == null || hitResult.type == HitResult.Type.MISS) {
			return
		}

		val packet = PacketByteBufs.create()
		packet.writeString(config.channel)
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
		val channel = buf.readString()

		if (channel != config.channel) {
			return
		}

		val pingPos = Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble())

		if (config.pingDistance < 2048) {
			val vecToPing = Game.player?.pos?.relativize(pingPos)

			if (vecToPing != null && vecToPing.length() > config.pingDistance.toDouble()) {
				return
			}
		}

		val uuid = if (buf.readBoolean()) buf.readUuid() else null

		client.execute {
			pingRepo.add(PingData(pingPos, uuid, Game.world?.time?.toInt() ?: 0))

			Game.soundManager.play(
				DirectionalSoundInstance(
					PingWheelClient.PING_SOUND_EVENT,
					SoundCategory.MASTER,
					config.pingVolume / 100f,
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
					if (ent.type == EntityType.ITEM && config.itemIconVisible) {
						val itemEnt = ent as ItemEntity
						ping.itemStack = itemEnt.stack.copy()
					}

					ping.pos = ent.getLerpedPos(tickDelta).add(0.0, ent.boundingBox.yLength, 0.0)
				}
			}

			ping.screenPos = Math.project3Dto2D(ping.pos, modelViewMatrix, projectionMatrix)
			ping.aliveTime = time - ping.spawnTime
		}

		pingRepo.removeIf { p -> p.aliveTime!! > config.pingDuration * TPS }
	}

	@JvmStatic
	fun onRenderGUI(stack: MatrixStack, ci: CallbackInfo) {
		for (ping in pingRepo) {
			val uiScale = Game.window.scaleFactor
			val uiScaleAdjustment = Math.mapValue(uiScale.toFloat(), 1f, 5f, 1f, 2f)

			val pingPosScreen = ping.screenPos ?: continue
			val cameraPosVec = Game.player?.getCameraPosVec(Game.tickDelta) ?: continue
			val distanceToPing = cameraPosVec.distanceTo(ping.pos).toFloat()
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

			if (ping.itemStack != null && config.itemIconVisible) {
				val model = Game.itemRenderer.getModel(ping.itemStack, null, null, 0)

				Draw.renderGuiItemModel(
					ping.itemStack,
					(pingPosScreen.x / uiScale),
					(pingPosScreen.y / uiScale),
					model,
					stack,
					pingScale * 2 / 3
				)
			} else {
				stack.rotateZ(PI.toFloat() / 4f)
				stack.translate(-2.5, -2.5, 0.0)
				DrawableHelper.fill(stack, 0, 0, 5, 5, white)
			}

			stack.pop() // pop
		}
	}
}
