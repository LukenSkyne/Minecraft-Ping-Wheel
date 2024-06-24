package nx.pingwheel.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.GuiGraphics;

public interface GuiRenderCallback {

	Event<GuiRenderCallback> START = EventFactory.createArrayBacked(GuiRenderCallback.class, (listeners) -> (drawContext, delta) -> {
		for (GuiRenderCallback event : listeners) {
			event.onRenderGui(drawContext, delta);
		}
	});

	void onRenderGui(GuiGraphics drawContext, float tickDelta);
}
