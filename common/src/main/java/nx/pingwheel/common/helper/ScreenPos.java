package nx.pingwheel.common.helper;

import net.minecraft.world.phys.Vec2;

public class ScreenPos {

	public final float x;
	public final float y;
	public final float depth;

	public ScreenPos(float x, float y, float depth) {
		this.x = x;
		this.y = y;
		this.depth = depth;
	}

	public boolean isBehindCamera() {
		return this.depth <= 0f;
	}

	public float distanceTo(Vec2 b) {
		if (this.isBehindCamera()) {
			return Float.MAX_VALUE;
		}

		return new Vec2(b.x - this.x, b.y - this.y).length();
	}

	public boolean isInBounds(Vec2 topLeft, Vec2 bottomRight) {
		return this.x >= topLeft.x && this.x <= bottomRight.x && this.y >= topLeft.y && this.y <= bottomRight.y;
	}
}
