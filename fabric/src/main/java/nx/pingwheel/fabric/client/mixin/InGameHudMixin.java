package nx.pingwheel.fabric.client.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import nx.pingwheel.fabric.client.event.GameOverlayRenderCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

	@Inject(method = "render", at = @At(value = "HEAD"))
	public void render(MatrixStack matrixStack, float tickDelta, CallbackInfo callbackInfo) {
		matrixStack.push();

		/** the hotbar is rendered at Z -90 {@link InGameHud#renderHotbar(float, MatrixStack)} */
		matrixStack.translate(0, 0, -90);
		GameOverlayRenderCallback.START.invoker().onGameOverlayRender(matrixStack, tickDelta);

		matrixStack.pop();
	}
}
