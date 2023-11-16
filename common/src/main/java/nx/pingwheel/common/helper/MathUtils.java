package nx.pingwheel.common.helper;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import static nx.pingwheel.common.ClientGlobal.Game;

public class MathUtils {
	private MathUtils() {}

	public static Vector4f project3Dto2D(Vec3d pos3d, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		var in3d = Game.gameRenderer.getCamera().getPos().negate().add(pos3d);
		var window = Game.getWindow();
		var pos4d = new Vector4f((float)in3d.x, (float)in3d.y, (float)in3d.z, 1f);

		pos4d.mul(modelViewMatrix);
		pos4d.mul(projectionMatrix);

		var depth = pos4d.w;

		if (depth != 0) {
			pos4d.div(depth);
		}

		pos4d.set(
			(pos4d.x * 0.5f + 0.5f) * window.getScaledWidth(),
			(1f - (pos4d.y * 0.5f + 0.5f)) * window.getScaledHeight(),
			pos4d.z,
			depth
		);

		return pos4d;
	}

	public static void rotateZ(MatrixStack matrixStack, float theta) {
		matrixStack.multiplyPositionMatrix(new Matrix4f().rotateZ(theta));
	}

	public static Vec2f calculateAngleRectIntersection(float angle, Vec2f leftTop, Vec2f rightBottom) {
		var direction = new Vec3d(Math.cos(angle), Math.sin(angle), 0f);
		var width = rightBottom.x - leftTop.x;
		var height = rightBottom.y - leftTop.y;
		direction = direction.multiply(new Vec3d(1f / width, 1f / height, 0f));

		var dx = Math.cos(angle);
		var dy = Math.sin(angle);
		var cx = width * 0.5f;
		var cy = height * 0.5f;

		if (Math.abs(direction.x) < Math.abs(direction.y)) {
			if (direction.y < 0) {
				// top
				var t = -cy / dy;
				var x = cx + t * dx;

				return new Vec2f((float)x + leftTop.x, leftTop.y);
			}

			// bottom
			var t = cy / dy;
			var x = cx + t * dx;

			return new Vec2f((float)x + leftTop.x, rightBottom.y);
		}

		if (direction.x < 0) {
			// left
			var t = -cx / dx;
			var y = cy + t * dy;

			return new Vec2f(leftTop.x, (float)y + leftTop.y);
		}

		// right
		var t = cx / dx;
		var y = cy + t * dy;

		return new Vec2f(rightBottom.x, (float)y + leftTop.y);
	}
}
