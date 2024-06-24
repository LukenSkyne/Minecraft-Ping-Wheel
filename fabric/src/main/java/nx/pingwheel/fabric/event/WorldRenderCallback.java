package nx.pingwheel.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.joml.Matrix4f;

public interface WorldRenderCallback {

	Event<WorldRenderCallback> START = EventFactory.createArrayBacked(WorldRenderCallback.class, (listeners) -> (modelViewMatrix, projectionMatrix, tickDelta) -> {
		for (WorldRenderCallback event : listeners) {
			event.onRenderWorld(modelViewMatrix, projectionMatrix, tickDelta);
		}
	});

	void onRenderWorld(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta);
}
