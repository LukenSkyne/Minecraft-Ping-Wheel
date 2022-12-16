package nx.pingwheel.client.util

import net.minecraft.util.math.Matrix4f
import net.minecraft.util.math.Quaternion
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vector4f

object Math {

	@JvmStatic
	fun mapValue(value: Float, inMin: Float, inMax: Float, outMin: Float, outMax: Float): Float {
		return ((value - inMin) / (inMax - inMin)) * (outMax - outMin) + outMin
	}
	
	@JvmStatic
	fun project3Dto2D(pos3D: Vec3d, modelViewMatrix: Matrix4f, projectionMatrix: Matrix4f): Vector4f? {
		val in3D = Game.gameRenderer.camera.pos.negate().add(pos3D);

		val wnd = Game.window
		val quaternion = Quaternion(
			in3D.x.toFloat(), in3D.y.toFloat(), in3D.z.toFloat(),
			1f
		)

		val result = projectionMatrix.multiplyReturn(modelViewMatrix.multiplyReturn(quaternion));

		if (result.w <= 0f) {
			return null
		}

		val screenCoords = result.toScreen()
		val x = screenCoords.x * wnd.width
		val y = screenCoords.y * wnd.height

		if (x.isInfinite() || y.isInfinite()) {
			return null
		}

		return Vector4f(x, wnd.height - y, screenCoords.z, 1f / (screenCoords.w * 2f))
	}
}
