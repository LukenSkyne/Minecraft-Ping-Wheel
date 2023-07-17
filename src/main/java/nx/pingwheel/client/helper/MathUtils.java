package nx.pingwheel.client.helper;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import static nx.pingwheel.client.PingWheelClient.Game;

public class MathUtils {

	public static Vector4f project3Dto2D(Vec3d pos3d,
										 Matrix4f modelViewMatrix,
										 Matrix4f projectionMatrix) {
		var in3d = Game.gameRenderer.getCamera().getPos().negate().add(pos3d);

		var wnd = Game.getWindow();
		var quaternion = new Quaternionf((float) in3d.x, (float) in3d.y, (float) in3d.z, 1.f);
		var product = mqProduct(projectionMatrix, mqProduct(modelViewMatrix, quaternion));

		if (product.w <= 0f) {
			return null;
		}

		var screenPos = qToScreen(product);
		var x = screenPos.x * wnd.getWidth();
		var y = screenPos.y * wnd.getHeight();

		if (Float.isInfinite(x) || Float.isInfinite(y)) {
			return null;
		}

		return new Vector4f(x, wnd.getHeight() - y, screenPos.z, 1f / (screenPos.w * 2f));
	}

	public static void rotateZ(MatrixStack matrixStack, float theta) {
		matrixStack.multiplyPositionMatrix(new Matrix4f().rotateZ(theta));
	}

	private static Quaternionf mqProduct(Matrix4f m, Quaternionf q) {
		return new Quaternionf(
			m.m00() * q.x + m.m10() * q.y + m.m20() * q.z + m.m30() * q.w,
			m.m01() * q.x + m.m11() * q.y + m.m21() * q.z + m.m31() * q.w,
			m.m02() * q.x + m.m12() * q.y + m.m22() * q.z + m.m32() * q.w,
			m.m03() * q.x + m.m13() * q.y + m.m23() * q.z + m.m33() * q.w
		);
	}

	private static Quaternionf qToScreen(Quaternionf q) {
		var w = 1f / q.w * 0.5f;

		return new Quaternionf(
			q.x * w + 0.5f,
			q.y * w + 0.5f,
			q.z * w + 0.5f,
			w);
	}
}
