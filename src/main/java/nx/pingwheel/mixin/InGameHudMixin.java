package nx.pingwheel.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import nx.pingwheel.client.Core;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Inject(method = "render", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/option/GameOptions;debugEnabled:Z",
            opcode = Opcodes.GETFIELD, args = {"log=false"}))
    private void onRenderPreDebugScreen(DrawContext ctx, float f, CallbackInfo ci) {
        Core.onRenderGUI(ctx, ci);
    }
}
