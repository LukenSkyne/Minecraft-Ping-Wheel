package nx.pingwheel.client

import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vector4f

data class PingData(
	val pos: Vec3d,
	var screenPos: Vector4f? = null,
	var lifeTime: Float = 300f,
)
