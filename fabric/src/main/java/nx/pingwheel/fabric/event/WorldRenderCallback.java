package nx.pingwheel.fabric.event;

import com.mojang.math.Matrix4f;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface WorldRenderCallback {

	Event<WorldRenderCallback> START = EventFactory.createArrayBacked(WorldRenderCallback.class, (listeners) -> (modelViewMatrix, projectionMatrix, tickDelta) -> {
		for (WorldRenderCallback event : listeners) {
			event.onRenderWorld(modelViewMatrix, projectionMatrix, tickDelta);
		}
	});

	void onRenderWorld(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta);
}
