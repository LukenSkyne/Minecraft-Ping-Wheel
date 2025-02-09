package nx.pingwheel.common.core;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.PlayerTeam;
import nx.pingwheel.common.compat.Component;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.helper.*;
import nx.pingwheel.common.networking.PingLocationC2SPacket;
import nx.pingwheel.common.networking.PingLocationS2CPacket;
import nx.pingwheel.common.screen.SettingsScreen;
import nx.pingwheel.common.sound.DirectionalSoundInstance;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static nx.pingwheel.common.ClientGlobal.*;
import static nx.pingwheel.common.Global.LOGGER;
import static nx.pingwheel.common.Global.NetHandler;
import static nx.pingwheel.common.config.ClientConfig.*;

public class ClientCore {
    private ClientCore() {
    }

    private static final ClientConfig Config = ConfigHandler.getConfig();
    private static final ArrayList<Ping> pingRepo = new ArrayList<>();
    private static boolean pingQueued = false;
    private static ClientLevel lastWorld = null;
    private static int dimension = 0;
    private static int lastPing = 0;
    private static int pingSequence = 0;

    public static void onDisconnect() {
        pingRepo.clear();
    }

    public static void onTick() {
        if (InputUtils.consumePingHotkey()) {
            pingQueued = true;
        }

        if (KEY_BINDING_SETTINGS.consumeClick()) {
            Game.setScreen(new SettingsScreen());
        }
    }

    public static void onPingLocation(PingLocationS2CPacket packet) {
        if (packet.isCorrupt()) {
            LOGGER.warn("received invalid ping location from server");
            return;
        }

        final var connection = Game.getConnection();

        if (Game.player == null || Game.level == null || connection == null) {
            return;
        }

        if (!packet.channel().equals(Config.getChannel())) {
            return;
        }

        if (Config.getPingDistance() < MAX_PING_DISTANCE) {
            var vecToPing = Game.player.position().vectorTo(packet.pos());

            if (vecToPing.length() > Config.getPingDistance()) {
                return;
            }
        }

        final var authorInfo = connection.getPlayerInfo(packet.author());

        Game.execute(() -> {
            addOrReplacePing(new Ping(
                    packet.pos(),
                    packet.entity(),
                    authorInfo,
                    packet.sequence(),
                    packet.dimension(),
                    (int) Game.level.getGameTime()
            ));

            if (packet.dimension() == dimension) {
                Game.getSoundManager().play(
                        new DirectionalSoundInstance(
                                PING_SOUND_EVENT,
                                SoundSource.MASTER,
                                Config.getPingVolume() / 100f,
                                1f,
                                packet.pos()
                        )
                );
            }
        });
    }

    public static void onRenderWorld(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta) {
        if (Game.level == null) {
            return;
        }

        if (lastWorld != Game.level) {
            lastWorld = Game.level;
            dimension = lastWorld.dimension().location().hashCode();
        }

        var time = (int) Game.level.getGameTime();
        processPings(modelViewMatrix, projectionMatrix, tickDelta, time);

        if (pingQueued) {
            if (Config.getCorrectionPeriod() < MAX_CORRECTION_PERIOD && time - lastPing > Config.getCorrectionPeriod() * TPS) {
                ++pingSequence;
            }

            lastPing = time;
            pingQueued = false;
            executePing(tickDelta);
        }
    }

    public static void onRenderGUI(PoseStack poseStack, float tickDelta) {
        if (Game.player == null || pingRepo.isEmpty()) {
            return;
        }

        var drawContext = new DrawContext(poseStack);
        var window = Game.getWindow();
        var screenSize = new Vec2(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        var safeZoneTopLeft = new Vec2(Config.getSafeZoneLeft(), Config.getSafeZoneTop());
        var safeZoneBottomRight = new Vec2(screenSize.x - Config.getSafeZoneRight(), screenSize.y - Config.getSafeZoneBottom());
        var safeScreenCenter = new Vec2((safeZoneBottomRight.x - safeZoneTopLeft.x) * 0.5f, (safeZoneBottomRight.y - safeZoneTopLeft.y) * 0.5f);
        final var showDirectionIndicator = Config.isDirectionIndicatorVisible();
        final var showNameLabels = Config.isNameLabelForced() || KEY_BINDING_NAME_LABELS.isDown();

        poseStack.pushPose();
        poseStack.translate(0f, 0f, -pingRepo.size() * 16f);

        for (var ping : pingRepo) {
            var screenPos = ping.getScreenPos();

            if (screenPos == null || ping.getDimension() != dimension || (screenPos.isBehindCamera() && !showDirectionIndicator)) {
                continue;
            }

            poseStack.translate(0f, 0f, 16f);

            var pingSize = Config.getPingSize() / 100f;
            var pingScale = getDistanceScale(ping.getDistance()) * pingSize * 0.4f;

            var pingDirectionVec = new Vec2(screenPos.x - safeZoneTopLeft.x - safeScreenCenter.x, screenPos.y - safeZoneTopLeft.y - safeScreenCenter.y);
            var behindCamera = screenPos.isBehindCamera();

            if (behindCamera) {
                pingDirectionVec = pingDirectionVec.scale(-1);
            }

            var pingAngle = (float) Math.atan2(pingDirectionVec.y, pingDirectionVec.x);
            var isOffScreen = behindCamera || !screenPos.isInBounds(Vec2.ZERO, screenSize);

            if (isOffScreen && showDirectionIndicator) {
                var indicator = MathUtils.calculateAngleRectIntersection(pingAngle, safeZoneTopLeft, safeZoneBottomRight);

                poseStack.pushPose();
                poseStack.translate(indicator.x, indicator.y, 0f);

                poseStack.pushPose();
                poseStack.scale(pingScale, pingScale, 1f);
                var indicatorOffsetX = Math.cos(pingAngle + Math.PI) * 12;
                var indicatorOffsetY = Math.sin(pingAngle + Math.PI) * 12;
                poseStack.translate(indicatorOffsetX, indicatorOffsetY, 0);
                drawContext.renderPing(ping, Config);
                poseStack.popPose();

                poseStack.pushPose();
                MathUtils.rotateZ(poseStack, pingAngle);
                poseStack.scale(pingSize, pingSize, 1f);

                poseStack.scale(0.25f, 0.25f, 1f);
                poseStack.translate(-5f, 0f, 0f);
                drawContext.renderArrow(true);
                poseStack.scale(0.9f, 0.9f, 1f);
                drawContext.renderArrow(false);
                poseStack.popPose();

                poseStack.popPose();
            }

            if (!behindCamera) {
                poseStack.pushPose();
                poseStack.translate(screenPos.x, screenPos.y, 0);
                poseStack.scale(pingScale, pingScale, 1f);

                var text = LanguageUtils.UNIT_METERS.get("%,.1f".formatted(ping.getDistance()));
                drawContext.renderLabel(text, -1.5f, null);
                drawContext.renderPing(ping, Config);

                var author = ping.getAuthor();

                if (showNameLabels && author != null) {
                    var displayName = PlayerTeam.formatNameForTeam(author.getTeam(), Component.literal(author.getProfile().getName()));
                    drawContext.renderLabel(displayName, 1.75f, author);
                }

                poseStack.popPose();
            }
        }

        poseStack.popPose();
    }

    private static void processPings(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float tickDelta, int time) {
        if (Game.player == null || pingRepo.isEmpty()) {
            return;
        }

        var cameraPos = Game.player.getEyePosition(tickDelta);
        Ping target = null;

        for (var iter = pingRepo.iterator(); iter.hasNext(); ) {
            var ping = iter.next();

            if (ping.getUuid() != null) {
                var entity = getEntity(ping.getUuid());

                if (entity != null) {
                    if (entity.getType() == EntityType.ITEM && !Config.getItemIconVisible().equals(ItemRenderType.DISABLE)) {
                        ping.setItemStack(((ItemEntity) entity).getItem().copy());
                    }
                    ping.setEntity(entity);
                    ping.setPos(entity.getPosition(tickDelta).add(0, entity.getBoundingBox().getYsize(), 0));
                }
            }

            ping.setDistance(cameraPos.distanceTo(ping.getPos()));
            ping.setScreenPos(MathUtils.worldToScreen(ping.getPos(), modelViewMatrix, projectionMatrix));
            ping.setAge(time - ping.getSpawnTime());

            if (ping.isExpired()) {
                iter.remove();
            } else if (pingQueued && ping.isRemovable() && ping.isCloserToCenter(target)) {
                target = ping;
            }
        }

        if (target != null && pingRepo.remove(target)) {
            pingQueued = false;
        }

        pingRepo.sort((a, b) -> Double.compare(b.getDistance(), a.getDistance()));
    }

    private static void executePing(float tickDelta) {
        var cameraEntity = Game.cameraEntity;

        if (cameraEntity == null || Game.level == null) {
            return;
        }

        var cameraDirection = cameraEntity.getViewVector(tickDelta);
        var hitResult = Raycast.traceDirectional(
                cameraDirection,
                tickDelta,
                Math.min(Config.getRaycastDistance(), Config.getPingDistance()),
                cameraEntity.isCrouching());

        if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        UUID uuid = null;

        if (hitResult.getType() == HitResult.Type.ENTITY) {
            uuid = ((EntityHitResult) hitResult).getEntity().getUUID();
        }

        NetHandler.sendToServer(new PingLocationC2SPacket(Config.getChannel(), hitResult.getLocation(), uuid, pingSequence, dimension));
    }

    private static void addOrReplacePing(Ping newPing) {
        int index = -1;

        for (int i = 0; i < pingRepo.size(); i++) {
            var entry = pingRepo.get(i);

            if (Objects.equals(entry.getAuthor(), newPing.getAuthor()) && entry.getSequence() == newPing.getSequence()) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            pingRepo.set(index, newPing);
        } else {
            pingRepo.add(newPing);
        }
    }

    private static Entity getEntity(UUID uuid) {
        if (Game.level == null) {
            return null;
        }

        for (var entity : Game.level.entitiesForRendering()) {
            if (entity.getUUID().equals(uuid)) {
                return entity;
            }
        }

        return null;
    }

    private static float getDistanceScale(double distance) {
        var scale = 2.0 / Math.pow(distance, 0.3);

        return (float) Math.max(1.0, scale);
    }
}
