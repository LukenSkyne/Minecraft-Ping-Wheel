package nx.pingwheel.common.helper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static nx.pingwheel.common.ClientGlobal.Game;

public class MathUtils {
	private MathUtils() {}

	public static ScreenPos worldToScreen(Vec3 worldPos, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		var window = Game.getWindow();
		var camera = Game.gameRenderer.getMainCamera();
		var worldPosRel = new Vector4f(camera.getPosition().reverse().add(worldPos).toVector3f(), 1f);
		worldPosRel.mul(modelViewMatrix);
		worldPosRel.mul(projectionMatrix);

		var depth = worldPosRel.w;

		if (depth != 0) {
			worldPosRel.div(depth);
		}

		return new ScreenPos(
			window.getGuiScaledWidth() * (0.5f + worldPosRel.x * 0.5f),
			window.getGuiScaledHeight() * (0.5f - worldPosRel.y * 0.5f),
			depth
		);
	}

	public static void rotateZ(PoseStack matrixStack, float theta) {
		matrixStack.mulPoseMatrix(new Matrix4f().rotateZ(theta));
	}

	public static Vec2 calculateAngleRectIntersection(float angle, Vec2 leftTop, Vec2 rightBottom) {
		var direction = new Vec3(Math.cos(angle), Math.sin(angle), 0f);
		var width = rightBottom.x - leftTop.x;
		var height = rightBottom.y - leftTop.y;
		direction = direction.multiply(new Vec3(1f / width, 1f / height, 0f));

		var dx = Math.cos(angle);
		var dy = Math.sin(angle);
		var cx = width * 0.5f;
		var cy = height * 0.5f;

		if (Math.abs(direction.x) < Math.abs(direction.y)) {
			if (direction.y < 0) {
				// top
				var t = -cy / dy;
				var x = cx + t * dx;

				return new Vec2((float)x + leftTop.x, leftTop.y);
			}

			// bottom
			var t = cy / dy;
			var x = cx + t * dx;

			return new Vec2((float)x + leftTop.x, rightBottom.y);
		}

		if (direction.x < 0) {
			// left
			var t = -cx / dx;
			var y = cy + t * dy;

			return new Vec2(leftTop.x, (float)y + leftTop.y);
		}

		// right
		var t = cx / dx;
		var y = cy + t * dy;

		return new Vec2(rightBottom.x, (float)y + leftTop.y);
	}
}
