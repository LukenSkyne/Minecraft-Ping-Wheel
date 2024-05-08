package nx.pingwheel.fabric.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

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
	void onGameOverlayRender(PoseStack matrixStack, float tickDelta);
}
