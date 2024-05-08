package nx.pingwheel.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import nx.pingwheel.fabric.event.GuiRenderCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class)
public abstract class GuiMixin {

	@Inject(method = "render", at = @At(value = "HEAD"))
	public void render(PoseStack matrixStack, float tickDelta, CallbackInfo callbackInfo) {
		matrixStack.pushPose();

		/** the hotbar is rendered at Z -90 {@link Gui#renderHotbar(float, PoseStack)} */
		matrixStack.translate(0, 0, -90);
		GuiRenderCallback.START.invoker().onRenderGui(matrixStack, tickDelta);

		matrixStack.popPose();
	}
}
