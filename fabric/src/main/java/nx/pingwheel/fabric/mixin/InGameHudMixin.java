package nx.pingwheel.fabric.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import nx.pingwheel.fabric.event.GameOverlayRenderCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

	@Inject(method = "render", at = @At(value = "HEAD"))
	public void render(DrawContext drawContext, float tickDelta, CallbackInfo callbackInfo) {
		var matrixStack = drawContext.getMatrices();
		matrixStack.push();

		/** the hotbar is rendered at Z -90 {@link InGameHud#renderHotbar(float, DrawContext)} */
		matrixStack.translate(0, 0, -90);
		GameOverlayRenderCallback.START.invoker().onGameOverlayRender(drawContext, tickDelta);

		matrixStack.pop();
	}
}
