package nx.pingwheel.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import nx.pingwheel.fabric.event.GameOverlayRenderCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class InGameHudMixin {

	@Inject(method = "render", at = @At(value = "HEAD"))
	public void render(PoseStack matrixStack, float tickDelta, CallbackInfo callbackInfo) {
		matrixStack.pushPose();

		/** the hotbar is rendered at Z -90 {@link InGameHud#renderHotbar(float, MatrixStack)} */
		matrixStack.translate(0, 0, -90);
		GameOverlayRenderCallback.START.invoker().onGameOverlayRender(matrixStack, tickDelta);

		matrixStack.popPose();
	}
}
