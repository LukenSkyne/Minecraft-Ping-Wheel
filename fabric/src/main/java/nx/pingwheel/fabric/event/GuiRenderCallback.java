package nx.pingwheel.fabric.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface GuiRenderCallback {

	Event<GuiRenderCallback> START = EventFactory.createArrayBacked(GuiRenderCallback.class, (listeners) -> (matrixStack, delta) -> {
		for (GuiRenderCallback event : listeners) {
			event.onRenderGui(matrixStack, delta);
		}
	});

	void onRenderGui(PoseStack matrixStack, float tickDelta);
}
