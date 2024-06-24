package nx.pingwheel.common.helper;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import org.lwjgl.opengl.GL11;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.ClientGlobal.PING_TEXTURE_ID;
import static nx.pingwheel.common.resource.ResourceReloadListener.hasCustomTexture;

public class DrawContext {

	private static final int WHITE = FastColor.ARGB32.color(255, 255, 255, 255);
	private static final int SHADOW_BLACK = FastColor.ARGB32.color(64, 0, 0, 0);
	private static final int LIGHT_VALUE_MAX = 15728880;

	private PoseStack matrices;

	public DrawContext(PoseStack matrices) {
		this.matrices = matrices;
	}

	public void renderLabel(Component text, float yOffset, PlayerInfo player) {
		var extraWidth = (player != null) ? 10f : 0f;
		var textMetrics = new Vec2(
			Game.font.width(text) + extraWidth,
			Game.font.lineHeight
		);
		var textOffset = textMetrics.scale(-0.5f).add(new Vec2(0f, textMetrics.y * yOffset));

		matrices.pushPose();
		matrices.translate(textOffset.x, textOffset.y, 0);
		GuiComponent.fill(matrices, -2, -2, (int)textMetrics.x + 1, (int)textMetrics.y, SHADOW_BLACK);
		Game.font.draw(matrices, text, extraWidth, 0f, WHITE);

		if (player != null) {
			matrices.translate(-0.5, -0.5, 0);
			renderPlayerHead(player);
		}

		matrices.popPose();
	}

	public void renderPlayerHead(PlayerInfo player) {
		RenderSystem.setShaderTexture(0, player.getSkinLocation());
		RenderSystem.enableBlend();
		GuiComponent.blit(matrices, 0, 0, 0, 8, 8, 8, 8, 64, 64);
		GuiComponent.blit(matrices, 0, 0, 0, 40, 8, 8, 8, 64, 64); // Overlay (hat)
		RenderSystem.disableBlend();
	}

	public void renderPing(ItemStack itemStack, boolean drawItemIcon) {
		if (itemStack != null && drawItemIcon) {
			renderGuiItemModel(itemStack);
		} else if (hasCustomTexture()) {
			renderCustomPingIcon();
		} else {
			renderDefaultPingIcon();
		}
	}

	public void renderGuiItemModel(ItemStack itemStack) {
		var model = Game.getItemRenderer().getModel(itemStack, null, null, 0);

		Game.getTextureManager()
			.getTexture(TextureAtlas.LOCATION_BLOCKS)
			.setFilter(false, false);

		RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		var matrixStack = RenderSystem.getModelViewStack();
		matrixStack.pushPose();
		matrixStack.mulPoseMatrix(matrices.last().pose());
		matrixStack.translate(0f, 0f, -0.5f);
		matrixStack.scale(1f, -1f, 1f);
		matrixStack.scale(10f, 10f, 0.5f);
		RenderSystem.applyModelViewMatrix();

		var immediate = Game.renderBuffers().bufferSource();
		var bl = !model.usesBlockLight();
		if (bl) {
			Lighting.setupForFlatItems();
		}

		var matrixStackDummy = new PoseStack();
		Game.getItemRenderer().render(
			itemStack,
			ItemTransforms.TransformType.GUI,
			false,
			matrixStackDummy,
			immediate,
			LIGHT_VALUE_MAX,
			OverlayTexture.NO_OVERLAY,
			model
		);
		immediate.endBatch();
		RenderSystem.enableDepthTest();

		if (bl) {
			Lighting.setupFor3DItems();
		}

		matrixStack.popPose();
		RenderSystem.applyModelViewMatrix();
	}

	public void renderCustomPingIcon() {
		final var size = 12;
		final var offset = size / -2;

		RenderSystem.setShaderTexture(0, PING_TEXTURE_ID);
		RenderSystem.enableBlend();
		GuiComponent.blit(
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

	public void renderDefaultPingIcon() {
		matrices.pushPose();
		MathUtils.rotateZ(matrices, (float)(Math.PI / 4f));
		matrices.translate(-2.5, -2.5, 0);
		GuiComponent.fill(matrices, 0, 0, 5, 5, WHITE);
		matrices.popPose();
	}

	public void renderArrow(boolean antialias) {
		if (antialias) {
			GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
		}

		var bufferBuilder = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		var mat = matrices.last().pose();
		bufferBuilder.vertex(mat, 5f, 0f, 0f).color(1f, 1f, 1f, 1f).endVertex();
		bufferBuilder.vertex(mat, -5f, -5f, 0f).color(1f, 1f, 1f, 1f).endVertex();
		bufferBuilder.vertex(mat, -3f, 0f, 0f).color(1f, 1f, 1f, 1f).endVertex();
		bufferBuilder.vertex(mat, -5f, 5f, 0f).color(1f, 1f, 1f, 1f).endVertex();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
		GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
	}
}
