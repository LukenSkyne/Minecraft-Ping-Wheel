package nx.pingwheel.common.helper;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

import static nx.pingwheel.common.ClientGlobal.Game;

public class MathUtils {
	private MathUtils() {}

	public static Vector4f project3Dto2D(Vec3d pos3d, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		var in3d = Game.gameRenderer.getCamera().getPos().negate().add(pos3d);
		var window = Game.getWindow();
		var pos4d = new Vector4f((float)in3d.x, (float)in3d.y, (float)in3d.z, 1f);

		pos4d.transform(modelViewMatrix);
		pos4d.transform(projectionMatrix);

		var depth = pos4d.getW();

		if (depth != 0) {
			pos4d.normalizeProjectiveCoordinates();
		}

		pos4d.set(
			(pos4d.getX() * 0.5f + 0.5f) * window.getScaledWidth(),
			(1f - (pos4d.getY() * 0.5f + 0.5f)) * window.getScaledHeight(),
			pos4d.getZ(),
			depth
		);

		return pos4d;
	}

	public static void rotateZ(MatrixStack matrixStack, float theta) {
		matrixStack.multiply(Vec3f.POSITIVE_Z.getRadialQuaternion(theta));
	}
}
