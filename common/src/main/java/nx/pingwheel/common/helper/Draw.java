package nx.pingwheel.common.helper;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec2f;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.ClientGlobal.PING_TEXTURE_ID;

public class Draw {
	private Draw() {}

	private static final int WHITE = ColorHelper.Argb.getArgb(255, 255, 255, 255);
	private static final int SHADOW_BLACK = ColorHelper.Argb.getArgb(64, 0, 0, 0);
	private static final int LIGHT_VALUE_MAX = 15728880;

	public static void renderLabel(MatrixStack matrices, String text) {
		var textMetrics = new Vec2f(
			Game.textRenderer.getWidth(text),
			Game.textRenderer.fontHeight
		);
		var textOffset = textMetrics.multiply(-0.5f).add(new Vec2f(0f, textMetrics.y * -1.5f));

		matrices.push();
		matrices.translate(textOffset.x, textOffset.y, 0);
		DrawableHelper.fill(matrices, -2, -2, (int)textMetrics.x + 1, (int)textMetrics.y, SHADOW_BLACK);
		Game.textRenderer.draw(matrices, text, 0f, 0f, WHITE);
		matrices.pop();
	}

	public static void renderPing(MatrixStack matrices, ItemStack itemStack, boolean drawItemIcon) {
		if (itemStack != null && drawItemIcon) {
			Draw.renderGuiItemModel(matrices, itemStack);
		} else if (hasCustomTexture()) {
			renderCustomPingIcon(matrices);
		} else {
			renderDefaultPingIcon(matrices);
		}
	}

	public static void renderGuiItemModel(MatrixStack matrices, ItemStack itemStack) {
		var model = Game.getItemRenderer().getModel(itemStack, null, null, 0);

		Game.getTextureManager()
			.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
			.setFilter(false, false);

		RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		matrices.push();
		matrices.scale(1f, -1f, 1f);
		matrices.scale(16f, 16f, 16f);

		var immediate = Game.getBufferBuilders().getEntityVertexConsumers();
		var bl = !model.isSideLit();
		if (bl) {
			DiffuseLighting.disableGuiDepthLighting();
		}

		Game.getItemRenderer().renderItem(
			itemStack,
			ModelTransformation.Mode.GUI,
			false,
			matrices,
			immediate,
			LIGHT_VALUE_MAX,
			OverlayTexture.DEFAULT_UV,
			model
		);
		immediate.draw();
		RenderSystem.enableDepthTest();

		if (bl) {
			DiffuseLighting.enableGuiDepthLighting();
		}

		matrices.pop();
	}

	public static void renderCustomPingIcon(MatrixStack matrices) {
		final var size = 12;
		final var offset = size / -2;

		RenderSystem.setShaderTexture(0, PING_TEXTURE_ID);
		RenderSystem.enableBlend();
		DrawableHelper.drawTexture(
			matrices,
			offset,
			offset,
			0,
			0,
			0,
			size,
			size,
			size,
			size
		);
		RenderSystem.disableBlend();
	}

	public static void renderDefaultPingIcon(MatrixStack matrices) {
		matrices.push();
		MathUtils.rotateZ(matrices, (float)(Math.PI / 4f));
		matrices.translate(-2.5, -2.5, 0);
		DrawableHelper.fill(matrices, 0, 0, 5, 5, WHITE);
		matrices.pop();
	}

	private static boolean hasCustomTexture() {
		return Game.getTextureManager().getOrDefault(PING_TEXTURE_ID, null) != null;
	}
}
