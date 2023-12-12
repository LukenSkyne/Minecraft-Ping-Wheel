package nx.pingwheel.common.compat;

import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

public class Vector4f {

	public float x;
	public float y;
	public float z;
	public float w;

	public Vector4f(Vec3d vec3d, float w) {
		this.x = (float)vec3d.x;
		this.y = (float)vec3d.y;
		this.z = (float)vec3d.z;
		this.w = w;
	}

	public Vector4f mul(Matrix4f mat) {
		var v = new net.minecraft.util.math.Vector4f(this.x, this.y, this.z, this.w);
		v.transform(mat);

		this.x = v.getX();
		this.y = v.getY();
		this.z = v.getZ();
		this.w = v.getW();

		return this;
	}

	public Vector4f div(float scalar) {
		float inv = 1f / scalar;
		this.x *= inv;
		this.y *= inv;
		this.z *= inv;
		this.w *= inv;

		return this;
	}
}
