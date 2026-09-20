package nx.pingwheel.common.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import nx.pingwheel.common.CommonClient;
import nx.pingwheel.common.render.WorldRenderContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final private GameRenderState gameRenderState;

	@Shadow protected abstract void bobHurt(CameraRenderState cameraState, PoseStack poseStack);
	@Shadow protected abstract void bobView(CameraRenderState cameraState, PoseStack poseStack);

	@Inject(
		method = "extract",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/Gui;extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V"
		),
		remap = false
	)
	public void extract(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
		var cameraState = this.gameRenderState.levelRenderState.cameraRenderState;
		var projectionMatrix = PingWheel_calculateProjectionMatrix(cameraState, deltaTracker);

		CommonClient.INSTANCE.onRenderWorld(
			WorldRenderContext.of(
				cameraState.viewRotationMatrix,
				projectionMatrix,
				deltaTracker.getGameTimeDeltaPartialTick(false),
				cameraState
			)
		);
	}

	@Unique
	private Matrix4f PingWheel_calculateProjectionMatrix(CameraRenderState cameraState, DeltaTracker deltaTracker) {
		var optionsState = this.gameRenderState.optionsRenderState;
		var playerState = this.gameRenderState.levelRenderState.playerRenderState;

		var projectionMatrix = new Matrix4f(cameraState.projectionMatrix);
		var bobStack = new PoseStack();
		this.bobHurt(cameraState, bobStack);

		if (optionsState.bobView) {
			this.bobView(cameraState, bobStack);
		}

		projectionMatrix.mul(bobStack.last().pose());
		float screenEffectScale = optionsState.screenEffectScale;
		float portalIntensity = playerState.portalEffectIntensity;
		float nauseaIntensity = playerState.nauseaEffectIntensity;
		float spinningEffectIntensity = Math.max(portalIntensity, nauseaIntensity) * screenEffectScale * screenEffectScale;
		if (spinningEffectIntensity > 0.0F) {
			float skew = 5.0F / (spinningEffectIntensity * spinningEffectIntensity + 5.0F) - spinningEffectIntensity * 0.04F;
			skew *= skew;
			Vector3f axis = new Vector3f(0.0F, Mth.SQRT_OF_TWO / 2.0F, Mth.SQRT_OF_TWO / 2.0F);
			float angle = playerState.spinningEffectAngle * ((float)Math.PI / 180F);
			projectionMatrix.rotate(angle, axis);
			projectionMatrix.scale(1.0F / skew, 1.0F, 1.0F);
			projectionMatrix.rotate(-angle, axis);
		}

		return projectionMatrix;
	}
}
