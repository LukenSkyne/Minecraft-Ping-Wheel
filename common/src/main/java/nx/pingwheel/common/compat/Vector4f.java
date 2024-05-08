package nx.pingwheel.common.compat;

import com.mojang.math.Matrix4f;
import net.minecraft.world.phys.Vec3;

public class Vector4f {

	public float x;
	public float y;
	public float z;
	public float w;

	public Vector4f(Vec3 vec3d, float w) {
		this.x = (float)vec3d.x;
		this.y = (float)vec3d.y;
		this.z = (float)vec3d.z;
		this.w = w;
	}

	public Vector4f mul(Matrix4f mat) {
		var v = new com.mojang.math.Vector4f(this.x, this.y, this.z, this.w);
		v.transform(mat);

		this.x = v.x();
		this.y = v.y();
		this.z = v.z();
		this.w = v.w();

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
