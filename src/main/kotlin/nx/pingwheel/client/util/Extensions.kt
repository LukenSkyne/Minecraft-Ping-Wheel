package nx.pingwheel.client.util

import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import org.joml.Matrix4f
import org.joml.Quaternionf

// global reference to the minecraft client
val Game: MinecraftClient = MinecraftClient.getInstance()

// .rotateZ extension for usage similar to .translate and .scale
fun MatrixStack.rotateZ(theta: Float) {
	multiplyPositionMatrix(Matrix4f().rotateZ(theta))
}

// extension of .multiply without mutating the original and instead return a quaternion
fun Matrix4f.multiplyReturn(q: Quaternionf): Quaternionf {
	return Quaternionf(
		m00() * q.x + m10() * q.y + m20() * q.z + m30() * q.w,
		m01() * q.x + m11() * q.y + m21() * q.z + m31() * q.w,
		m02() * q.x + m12() * q.y + m22() * q.z + m32() * q.w,
		m03() * q.x + m13() * q.y + m23() * q.z + m33() * q.w
	)
}

// retrieve quaternion in screen space
fun Quaternionf.toScreen(): Quaternionf {
	val newW = 1f / w * 0.5f

	return Quaternionf(
		x * newW + 0.5f,
		y * newW + 0.5f,
		z * newW + 0.5f,
		newW
	)
}
