package nx.pingwheel.client.util

import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Matrix4f
import net.minecraft.util.math.Quaternion
import nx.pingwheel.client.PingWheelClient
import org.apache.logging.log4j.Logger
import org.lwjgl.BufferUtils
import kotlin.math.cos
import kotlin.math.sin

// global reference to the minecraft client
val Game: MinecraftClient = MinecraftClient.getInstance()
val ConLog: Logger = PingWheelClient.LOGGER

// .rotateZ extension for usage similar to .translate and .scale
fun MatrixStack.rotateZ(theta: Float) {
	val m = BufferUtils.createFloatBuffer(16)
	val m4f = Matrix4f()

	m.put(cos(theta)); m.put(-sin(theta)); m.put(0f); m.put(0f)
	m.put(sin(theta)); m.put(cos(theta)); m.put(0f); m.put(0f)
	m.put(0f); m.put(0f); m.put(1f); m.put(0f)
	m.put(0f); m.put(0f); m.put(0f); m.put(1f)

	m4f.readRowMajor(m)
	multiplyPositionMatrix(m4f)
}

// extension of .multiply without mutating the original and instead return a quaternion
fun Matrix4f.multiplyReturn(q: Quaternion): Quaternion {
	val m = BufferUtils.createFloatBuffer(16)
	writeRowMajor(m)

	return Quaternion(
		m[0] * q.x + m[1] * q.y + m[2] * q.z + m[3] * q.w,
		m[4] * q.x + m[5] * q.y + m[6] * q.z + m[7] * q.w,
		m[8] * q.x + m[9] * q.y + m[10] * q.z + m[11] * q.w,
		m[12] * q.x + m[13] * q.y + m[14] * q.z + m[15] * q.w
	)
}

// retrieve quaternion in screen space
fun Quaternion.toScreen(): Quaternion {
	val newW = 1f / w * 0.5f

	return Quaternion(
		x * newW + 0.5f,
		y * newW + 0.5f,
		z * newW + 0.5f,
		newW
	)
}
