package nx.pingwheel.client.helper;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vector4f;
import org.lwjgl.BufferUtils;

import static nx.pingwheel.client.PingWheelClient.Game;

public class MathUtils {

	public static Vector4f project3Dto2D(Vec3d pos3d,
										 Matrix4f modelViewMatrix,
										 Matrix4f projectionMatrix) {
		var in3d = Game.gameRenderer.getCamera().getPos().negate().add(pos3d);

		var wnd = Game.getWindow();
		var quaternion = new Quaternion((float)in3d.x, (float)in3d.y, (float)in3d.z, 1.f);
		var product = mqProduct(projectionMatrix, mqProduct(modelViewMatrix, quaternion));

		if (product.getW() <= 0f) {
			return null;
		}

		var screenPos = qToScreen(product);
		var x = screenPos.getX() * wnd.getWidth();
		var y = screenPos.getY() * wnd.getHeight();

		if (Float.isInfinite(x) || Float.isInfinite(y)) {
			return null;
		}

		return new Vector4f(x, wnd.getHeight() - y, screenPos.getZ(), 1f / (screenPos.getW() * 2f));
	}

	public static void rotateZ(MatrixStack matrixStack, float theta) {
		var m = BufferUtils.createFloatBuffer(16);
		var m4f = new Matrix4f();

		m.put((float)Math.cos(theta)); m.put(-(float)Math.sin(theta)); m.put(0f); m.put(0f);
		m.put((float)Math.sin(theta)); m.put((float)Math.cos(theta)); m.put(0f); m.put(0f);
		m.put(0f); m.put(0f); m.put(1f); m.put(0f);
		m.put(0f); m.put(0f); m.put(0f); m.put(1f);

		m4f.readRowMajor(m);
		matrixStack.multiplyPositionMatrix(m4f);
	}

	private static Quaternion mqProduct(Matrix4f m4f, Quaternion q) {
		var m = BufferUtils.createFloatBuffer(16);
		m4f.writeRowMajor(m);

		return new Quaternion(
			m.get(0) * q.getX() + m.get(1) * q.getY() + m.get(2) * q.getZ() + m.get(3) * q.getW(),
			m.get(4) * q.getX() + m.get(5) * q.getY() + m.get(6) * q.getZ() + m.get(7) * q.getW(),
			m.get(8) * q.getX() + m.get(9) * q.getY() + m.get(10) * q.getZ() + m.get(11) * q.getW(),
			m.get(12) * q.getX() + m.get(13) * q.getY() + m.get(14) * q.getZ() + m.get(15) * q.getW()
		);
	}

	private static Quaternion qToScreen(Quaternion q) {
		var w = 1f / q.getW() * 0.5f;

		return new Quaternion(
			q.getX() * w + 0.5f,
			q.getY() * w + 0.5f,
			q.getZ() * w + 0.5f,
			w);
	}
}
