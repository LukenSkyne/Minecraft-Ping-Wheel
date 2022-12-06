package nx.pingwheel.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method="render", at=@At(
			value="FIELD",
			target="Lnet/minecraft/client/option/GameOptions;debugEnabled:Z",
			opcode = Opcodes.GETFIELD, args = {"log=false"}))

	private void onRenderPreDebugScreen(MatrixStack stack, float f, CallbackInfo ci) {

	}
}
