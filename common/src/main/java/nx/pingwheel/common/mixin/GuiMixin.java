package nx.pingwheel.common.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import nx.pingwheel.common.CommonClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

	@Shadow @Final private Minecraft minecraft;

	@Inject(
		method = "extractRenderState",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
			shift = At.Shift.AFTER
		),
		remap = false
	)
	private void extractRenderState(
		DeltaTracker deltaTracker,
		boolean shouldRenderLevel,
		boolean resourcesLoaded,
		CallbackInfo ci,
		@Local(name = "graphics") GuiGraphicsExtractor graphics
	) {
		if (this.minecraft.gui.hud.isHidden()) {
			return;
		}

		final var matrixStack = graphics.pose();
		matrixStack.pushMatrix();
		CommonClient.INSTANCE.onRenderGUI(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
		matrixStack.popMatrix();
	}
}
