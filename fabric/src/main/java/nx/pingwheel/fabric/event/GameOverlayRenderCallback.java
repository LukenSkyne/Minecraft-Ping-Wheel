package nx.pingwheel.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.util.math.MatrixStack;

public interface GameOverlayRenderCallback {
	Event<GameOverlayRenderCallback> START = EventFactory.createArrayBacked(GameOverlayRenderCallback.class, (listeners) -> (matrixStack, delta) -> {
		for (GameOverlayRenderCallback event : listeners) {
			event.onGameOverlayRender(matrixStack, delta);
		}
	});

	/**
	 * Called before rendering the whole hud, which is displayed in game, in a world.
	 *
	 * @param matrixStack the matrixStack
	 * @param tickDelta Progress for linearly interpolating between the previous and current game state
	 */
	void onGameOverlayRender(MatrixStack matrixStack, float tickDelta);
}
