package nx.pingwheel.common.helper;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

import static nx.pingwheel.common.ClientGlobal.Game;

public class Draw {
	private Draw() {}

	public static void renderGuiItemModel(ItemStack itemStack,
										  double x,
										  double y,
										  BakedModel model,
										  float scale) {
		Game.getTextureManager()
			.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
			.setFilter(false, false);

		RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);

		var matrixStack = RenderSystem.getModelViewStack();
		matrixStack.push();
		matrixStack.translate(x, y, -100);
		matrixStack.scale(scale, scale, scale);
		matrixStack.scale(1.f, -1.f, 1.f);
		matrixStack.scale(16.f, 16.f, 16.f);
		RenderSystem.applyModelViewMatrix();

		var matrixStackDummy = new MatrixStack();
		var immediate = Game.getBufferBuilders().getEntityVertexConsumers();
		var bl = !model.isSideLit();
		if (bl) {
			DiffuseLighting.disableGuiDepthLighting();
		}
		Game.getItemRenderer().renderItem(
			itemStack,
			ModelTransformationMode.GUI,
			false,
			matrixStackDummy,
			immediate,
			15728880,
			OverlayTexture.DEFAULT_UV,
			model
		);
		immediate.draw();
		RenderSystem.enableDepthTest();
		if (bl) {
			DiffuseLighting.enableGuiDepthLighting();
		}

		matrixStack.pop();
		RenderSystem.applyModelViewMatrix();
	}
}
