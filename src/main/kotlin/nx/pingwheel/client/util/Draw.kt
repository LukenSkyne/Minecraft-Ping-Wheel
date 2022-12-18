package nx.pingwheel.client.util

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.DiffuseLighting
import net.minecraft.client.render.OverlayTexture
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.texture.SpriteAtlasTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.item.ItemStack

object Draw {

	@JvmStatic
	fun renderGuiItemModel(
		stack: ItemStack?,
		x: Double,
		y: Double,
		model: BakedModel,
		matrices: MatrixStack,
		scale: Float
	) {
		Game.textureManager.getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).setFilter(false, false)
		RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)
		RenderSystem.enableBlend()
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
		val matrixStack = RenderSystem.getModelViewStack()
		matrixStack.push()

		matrixStack.translate(x, y, (100.0f + Game.itemRenderer.zOffset).toDouble())

		matrixStack.scale(scale, scale, scale)

		//matrixStack.translate(8.0, 8.0, 0.0)

		matrixStack.scale(1.0f, -1.0f, 1.0f)
		matrixStack.scale(16.0f, 16.0f, 16.0f)

		//matrixStack.multiplyPositionMatrix(matrices.peek().positionMatrix)

		RenderSystem.applyModelViewMatrix()
		val matrixStack2 = MatrixStack()
		val immediate = MinecraftClient.getInstance().bufferBuilders.entityVertexConsumers
		val bl = !model.isSideLit
		if (bl) {
			DiffuseLighting.disableGuiDepthLighting()
		}
		Game.itemRenderer.renderItem(
			stack,
			ModelTransformation.Mode.GUI,
			false,
			matrixStack2,
			immediate,
			15728880,
			OverlayTexture.DEFAULT_UV,
			model
		)
		immediate.draw()
		RenderSystem.enableDepthTest()
		if (bl) {
			DiffuseLighting.enableGuiDepthLighting()
		}
		matrixStack.pop()
		RenderSystem.applyModelViewMatrix()
	}
}
