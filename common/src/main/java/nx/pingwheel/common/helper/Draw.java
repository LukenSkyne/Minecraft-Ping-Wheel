package nx.pingwheel.common.helper;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.opengl.GL11;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.ClientGlobal.PING_TEXTURE_ID;
import static nx.pingwheel.common.resource.ResourceReloadListener.hasCustomTexture;

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

		var matrixStack = RenderSystem.getModelViewStack();
		matrixStack.push();
		matrixStack.multiplyPositionMatrix(matrices.peek().getPositionMatrix());
		matrixStack.translate(0f, 0f, -0.5f);
		matrixStack.scale(1f, -1f, 1f);
		matrixStack.scale(10f, 10f, 0.5f);
		RenderSystem.applyModelViewMatrix();

		var immediate = Game.getBufferBuilders().getEntityVertexConsumers();
		var bl = !model.isSideLit();
		if (bl) {
			DiffuseLighting.disableGuiDepthLighting();
		}

		var matrixStackDummy = new MatrixStack();
		Game.getItemRenderer().renderItem(
			itemStack,
			ModelTransformation.Mode.GUI,
			false,
			matrixStackDummy,
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

		matrixStack.pop();
		RenderSystem.applyModelViewMatrix();
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

	public static void renderArrow(MatrixStack m, boolean antialias) {
		if (antialias) {
			GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		}

		var bufferBuilder = Tessellator.getInstance().getBuffer();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

		var mat = m.peek().getPositionMatrix();
		bufferBuilder.vertex(mat, 5f, 0f, 0f).color(1f, 1f, 1f, 1f).next();
		bufferBuilder.vertex(mat, -5f, -5f, 0f).color(1f, 1f, 1f, 1f).next();
		bufferBuilder.vertex(mat, -3f, 0f, 0f).color(1f, 1f, 1f, 1f).next();
		bufferBuilder.vertex(mat, -5f, 5f, 0f).color(1f, 1f, 1f, 1f).next();
		bufferBuilder.end();
		BufferRenderer.draw(bufferBuilder);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
	}
}
