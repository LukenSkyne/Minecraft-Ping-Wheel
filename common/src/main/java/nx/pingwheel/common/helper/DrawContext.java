package nx.pingwheel.common.helper;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec2;
import nx.pingwheel.common.Global;
import nx.pingwheel.common.config.ClientConfig;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import static nx.pingwheel.common.ClientGlobal.Game;
import static nx.pingwheel.common.ClientGlobal.PING_TEXTURE_ID;
import static nx.pingwheel.common.resource.ResourceReloadListener.hasCustomTexture;

public class DrawContext {

    private static final int WHITE = FastColor.ARGB32.color(255, 255, 255, 255);
    private static final int SHADOW_BLACK = FastColor.ARGB32.color(64, 0, 0, 0);

    private GuiGraphics guiGraphics;
    private PoseStack matrices;

    public DrawContext(GuiGraphics guiGraphics) {
        this.guiGraphics = guiGraphics;
        this.matrices = guiGraphics.pose();
    }

    public void renderLabel(Component text, float yOffset, PlayerInfo player) {
        var extraWidth = (player != null) ? 10 : 0;
        var textMetrics = new Vec2(
                (float) Game.font.width(text) + extraWidth,
                Game.font.lineHeight
        );
        var textOffset = textMetrics.scale(-0.5f).add(new Vec2(0f, textMetrics.y * yOffset));

        matrices.pushPose();
        matrices.translate(textOffset.x, textOffset.y, 0);
        guiGraphics.fill(-2, -2, (int) textMetrics.x + 1, (int) textMetrics.y, SHADOW_BLACK);
        guiGraphics.drawString(Game.font, text, extraWidth, 0, WHITE, false);

        if (player != null) {
            matrices.translate(-0.5, -0.5, 0);
            renderPlayerHead(player);
        }

        matrices.popPose();
    }

    public void renderPlayerHead(PlayerInfo player) {
        var texture = player.getSkin().texture();
        RenderSystem.enableBlend();
        guiGraphics.blit(texture, 0, 0, 0, 8, 8, 8, 8, 64, 64);
        guiGraphics.blit(texture, 0, 0, 0, 40, 8, 8, 8, 64, 64); // Overlay (hat)
        RenderSystem.disableBlend();
    }

    public void renderPing(Ping ping, ClientConfig config) {
        if (ping.getItemStack() != null) {
            renderGuiItemModel(ping, config);
        } else if (hasCustomTexture()) {
            renderCustomPingIcon();
        } else {
            renderDefaultPingIcon(ping, config);
        }
    }

    public void renderGuiItemModel(Ping ping, ClientConfig config) {
        switch (config.getItemIconVisible()) {
            case ITEM_RENDER -> guiGraphics.renderItem(ping.getItemStack(), -8, -8, 0, -150);
            case ITEM_ENTITY_RENDER -> renderEntity(config, ping, 45f);
            default -> {
                // Do nothing
            }
        }
    }

    public void renderCustomPingIcon() {
        final var size = 12;
        final var offset = size / -2;

        RenderSystem.enableBlend();
        guiGraphics.blit(
                PING_TEXTURE_ID,
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

    public void renderDefaultPingIcon(Ping ping, ClientConfig config) {
        matrices.pushPose();
        MathUtils.rotateZ(matrices, (float) (Math.PI / 4f));
        matrices.translate(-2.5, -2.5, 0);
        guiGraphics.fill(0, 0, 5, 5, WHITE);
        matrices.popPose();

        switch (config.getEntityIconVisible()) {
            case ROTATION_ENABLE -> renderEntity(config, ping, 20f);
            case ROTATION_DISABLE -> renderEntity(ping, 20f, 30f, 0f);
            default -> {
                // Do nothing
            }
        }
    }

    public void renderEntity(Ping ping, float scale, @Nullable Float yaw, @Nullable Float pitch) {
        if (!(ping.getEntity() instanceof ItemEntity)) {
            LivingEntity originalEntity = (LivingEntity) ping.getEntity();
            if (originalEntity != null && ping.getDistance() > 10) {
                LivingEntity entityCopy = (LivingEntity) originalEntity.getType().create(originalEntity.level());
                if (entityCopy != null) {
                    if (yaw != null) {
                        entityCopy.setYRot(yaw); // Rotación del cuerpo
                        entityCopy.setYBodyRot(yaw); // Rotación del cuerpo para animaciones
                        entityCopy.yHeadRot = yaw; // Rotación de la cabeza
                        entityCopy.yHeadRotO = yaw; // Rotación anterior de la cabeza
                    }
                    if (pitch != null) {
                        entityCopy.setXRot(pitch); // Rotación vertical (pitch)
                        entityCopy.xRotO = pitch; // Rotación vertical anterior (pitch)
                    }

                    renderEntityBase(entityCopy, scale);
                }
            }
        }
    }

    public void renderEntity(ClientConfig config, Ping ping, float scale) {
        if (ping.getEntity() instanceof ItemEntity && config.getItemIconVisible() == ItemRenderType.DISABLE) {
            return;
        }

        if (ping.getEntity() != null && ping.getDistance() > 10) {
            renderEntityBase(ping.getEntity(), scale);
        }
    }

    private void renderEntityBase(Entity entity, float scale) {
        matrices.pushPose();

        matrices.translate(-2.5, -20, 0);
        matrices.scale(scale, scale, scale);
        matrices.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(180)));

        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        dispatcher.setRenderShadow(false);
        try {
            dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, matrices,
                    Minecraft.getInstance().renderBuffers().bufferSource(), 15728880);
        } catch (Exception e) {
            Global.LOGGER.error("Failed to render entity in GUI", e);
        }
        dispatcher.setRenderShadow(true);
        matrices.popPose();
    }

    public void renderArrow(boolean antialias) {
        if (antialias) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        }

        var tesselator = Tesselator.getInstance();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var bufferBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        var mat = matrices.last().pose();
        bufferBuilder.addVertex(mat, 5f, 0f, 0f).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(mat, -5f, -5f, 0f).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(mat, -3f, 0f, 0f).setColor(1f, 1f, 1f, 1f);
        bufferBuilder.addVertex(mat, -5f, 5f, 0f).setColor(1f, 1f, 1f, 1f);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        RenderSystem.disableBlend();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }
}
