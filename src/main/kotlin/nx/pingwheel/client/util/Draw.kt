package nx.pingwheel.client.util

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.render.*
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Vec2f
import org.lwjgl.opengl.GL11
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

object Draw {

	// members
	private var matrixStack: MatrixStack = RenderSystem.getModelViewStack()

	@JvmStatic
	fun setContext(matrixStack: MatrixStack) {
		this.matrixStack = matrixStack
	}

	@JvmStatic
	@JvmOverloads
	fun line2D(
		x1: Float, y1: Float,
		x2: Float, y2: Float,
		width: Float = 2f,
		color: Long = 0xffffffff,
		antialiased: Boolean = false,
		matrixStack: MatrixStack = this.matrixStack
	) {
		matrixStack.push()

		if (antialiased)
			GL11.glEnable(GL11.GL_POLYGON_SMOOTH)

		val colorVec = Math.hexToVec(color)

		RenderSystem.enableBlend()
		RenderSystem.disableTexture()
		RenderSystem.defaultBlendFunc()
		RenderSystem.setShader { GameRenderer.getPositionShader() }
		//RenderSystem.setShader(GameRenderer::getPositionShader)
		RenderSystem.setShaderColor(colorVec.x, colorVec.y, colorVec.z, colorVec.w)

		val mag = sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
		val theta = atan2((y2 - y1), (x2 - x1))

		val reverseUIScale = 1.0 / Game.window.scaleFactor
		matrixStack.scale(reverseUIScale.toFloat(), reverseUIScale.toFloat(), reverseUIScale.toFloat())
		matrixStack.translate(x1.toDouble(), y1.toDouble(), 0.0)
		matrixStack.rotateZ(theta)

		val p1 = Vec2f(0f, -width * 0.5f)
		val p2 = Vec2f(mag, -width * 0.5f)
		val p3 = Vec2f(mag, width * 0.5f)
		val p4 = Vec2f(0f, width * 0.5f)
		val matrix = matrixStack.peek().positionMatrix

		val bufferBuilder = Tessellator.getInstance().buffer
		bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION)

		bufferBuilder.vertex(matrix, p4.x, p4.y, 0.0f).next()
		bufferBuilder.vertex(matrix, p3.x, p3.y, 0.0f).next()
		bufferBuilder.vertex(matrix, p2.x, p2.y, 0.0f).next()
		bufferBuilder.vertex(matrix, p1.x, p1.y, 0.0f).next()

		bufferBuilder.end()
		BufferRenderer.draw(bufferBuilder)

		GlStateManager._enableTexture()
		GlStateManager._disableBlend()

		if (antialiased)
			GL11.glDisable(GL11.GL_POLYGON_SMOOTH)

		matrixStack.pop()
	}
}
