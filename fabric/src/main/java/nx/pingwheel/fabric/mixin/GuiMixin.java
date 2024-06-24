package nx.pingwheel.fabric.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import nx.pingwheel.fabric.event.GuiRenderCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class)
public abstract class GuiMixin {

	@Inject(method = "render", at = @At(value = "HEAD"))
	public void render(GuiGraphics guiGraphics, float tickDelta, CallbackInfo ci) {
		var matrixStack = guiGraphics.pose();
		matrixStack.pushPose();

		/** the hotbar is rendered at Z -90 {@link Gui#renderHotbar(float, GuiGraphics)} */
		matrixStack.translate(0, 0, -90);
		GuiRenderCallback.START.invoker().onRenderGui(guiGraphics, tickDelta);

		matrixStack.popPose();
	}
}
