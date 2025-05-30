package org.bknibb.bk_meteor_addon.modules;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec2f;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class PlayerTracers extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgAppearance = settings.createGroup("Appearance");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    public enum TracerStyle {
        Lines,
        Offscreen
    }

    public final Setting<Boolean> showInvis = sgGeneral.add(new BoolSetting.Builder()
        .name("show-invisible")
        .description("Shows invisible entities.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("list-mode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<String>> blacklist = sgWhitelist.add(new StringListSetting.Builder()
        .name("blacklist")
        .description("The players you don't want to see.")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<Boolean> includeFriends = sgWhitelist.add(new BoolSetting.Builder()
        .name("include-friends")
        .description("Include meteor friends in the whitelist.")
        .defaultValue(true)
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<List<String>> whitelist = sgWhitelist.add(new StringListSetting.Builder()
        .name("whitelist")
        .description("The players you want to see.")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    public final Setting<TracerStyle> style = sgAppearance.add(new EnumSetting.Builder<TracerStyle>()
        .name("style")
        .description("What display mode should be used")
        .defaultValue(TracerStyle.Lines)
        .build()
    );

    public final Setting<Target> target = sgAppearance.add(new EnumSetting.Builder<Target>()
        .name("target")
        .description("What part of the entity to target.")
        .defaultValue(Target.Body)
        .visible(() ->  style.get() == TracerStyle.Lines)
        .build()
    );

    public final Setting<Boolean> stem = sgAppearance.add(new BoolSetting.Builder()
        .name("stem")
        .description("Draw a line through the center of the tracer target.")
        .defaultValue(true)
        .visible(() ->  style.get() == TracerStyle.Lines)
        .build()
    );

    private final Setting<Integer> maxDist = sgAppearance.add(new IntSetting.Builder()
        .name("max-distance")
        .description("Maximum distance for tracers to show.")
        .defaultValue(256)
        .min(0)
        .sliderMax(256)
        .build()
    );

    public final Setting<Integer> distanceOffscreen = sgAppearance.add(new IntSetting.Builder()
        .name("distance-offscreen")
        .description("Offscreen's distance from center.")
        .defaultValue(200)
        .min(0)
        .sliderMax(500)
        .visible(() ->  style.get() == TracerStyle.Offscreen)
        .build()
    );

    public final Setting<Integer> sizeOffscreen = sgAppearance.add(new IntSetting.Builder()
        .name("size-offscreen")
        .description("Offscreen's size.")
        .defaultValue(10)
        .min(2)
        .sliderMax(50)
        .visible(() ->  style.get() == TracerStyle.Offscreen)
        .build()
    );

    public final Setting<Boolean> blinkOffscreen = sgAppearance.add(new BoolSetting.Builder()
        .name("blink-offscreen")
        .description("Make offscreen Blink.")
        .defaultValue(true)
        .visible(() ->  style.get() == TracerStyle.Offscreen)
        .build()
    );

    private final Setting<Double> blinkOffscreenSpeed = sgAppearance.add(new DoubleSetting.Builder()
        .name("blink-offscreen-speed")
        .description("Offscreen's blink speed.")
        .defaultValue(4)
        .min(1)
        .sliderMax(15)
        .visible(() ->  style.get() == TracerStyle.Offscreen && blinkOffscreen.get())
        .build()
    );

    public final Setting<Boolean> distance = sgColors.add(new BoolSetting.Builder()
        .name("distance-colors")
        .description("Changes the color of tracers depending on distance.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> friendOverride = sgColors.add(new BoolSetting.Builder()
        .name("show-friend-colors")
        .description("Whether or not to override the distance color of friends with the friend color.")
        .defaultValue(true)
        .visible(distance::get)
        .build()
    );

    private final Setting<SettingColor> playersColor = sgColors.add(new ColorSetting.Builder()
        .name("players-colors")
        .description("The player's color.")
        .defaultValue(new SettingColor(205, 205, 205, 127))
        .visible(() -> !distance.get())
        .build()
    );

    private int count;
    private final Instant initTimer = Instant.now();

    public PlayerTracers() {
        super(BkMeteorAddon.CATEGORY, "player-tracers", "Tracers for specific players.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WHorizontalList list = theme.horizontalList();
        list.add(theme.button("Copy List Settings")).widget().action = () -> {
            NbtCompound tag = new NbtCompound();
            tag.put("listMode", listMode.toTag());
            tag.put("blacklist", blacklist.toTag());
            tag.put("includeFriends", includeFriends.toTag());
            tag.put("whitelist", whitelist.toTag());
            NbtUtils.toClipboard(tag);
        };
        list.add(theme.button("Paste List Settings")).widget().action = () -> {
            NbtCompound tag = NbtUtils.fromClipboard();
            if (tag == null) return;
            if (tag.contains("listMode")) {
                listMode.fromTag(tag.getCompound("listMode").get());
            }
            if (tag.contains("blacklist")) {
                blacklist.fromTag(tag.getCompound("blacklist").get());
            }
            if (tag.contains("includeFriends")) {
                includeFriends.fromTag(tag.getCompound("includeFriends").get());
            }
            if (tag.contains("whitelist")) {
                whitelist.fromTag(tag.getCompound("whitelist").get());
            }
        };
        return list;
    }

    private boolean shouldBeIgnored(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return true;
        if (entity == mc.player) return true;
        if (listMode.get() == ListMode.Blacklist) {
            if (blacklist.get().contains(entity.getName().getString())) {
                return true;
            }
        } else {
            if (!(whitelist.get().contains(entity.getName().getString()) || (includeFriends.get() && Friends.get().get(entity.getName().getString()) != null))) {
                return true;
            }
        }
        return !PlayerUtils.isWithin(entity, maxDist.get()) || (!showInvis.get() && entity.isInvisible()) | !EntityUtils.isInRenderDistance(entity);
    }

    public Color getEntityColor(Entity entity) {
        Color color;

        if (distance.get()) {
            if (friendOverride.get() && entity instanceof PlayerEntity && Friends.get().isFriend((PlayerEntity) entity)) {
                color = Config.get().friendColor.get();
            }
            else color = EntityUtils.getColorFromDistance(entity);
        }
        else {
            color = PlayerUtils.getPlayerColor(((PlayerEntity) entity), playersColor.get());
        }

        return new Color(color);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.options.hudHidden || style.get() == TracerStyle.Offscreen) return;
        if (mc.world == null) return;
        count = 0;

        for (Entity entity : mc.world.getEntities()) {
            if (shouldBeIgnored(entity)) continue;

            Color color = getEntityColor(entity);

            double x = entity.lastX + (entity.getX() - entity.lastX) * event.tickDelta;
            double y = entity.lastY + (entity.getY() - entity.lastY) * event.tickDelta;
            double z = entity.lastZ + (entity.getZ() - entity.lastZ) * event.tickDelta;

            double height = entity.getBoundingBox().maxY - entity.getBoundingBox().minY;
            if (target.get() == Target.Head) y += height;
            else if (target.get() == Target.Body) y += height / 2;

            event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y, z, color);
            if (stem.get()) event.renderer.line(x, entity.getY(), z, x, entity.getY() + height, z, color);

            count++;
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.options.hudHidden || style.get() != TracerStyle.Offscreen) return;
        if (mc.world == null) return;
        count = 0;

        Renderer2D.COLOR.begin();

        for (Entity entity : mc.world.getEntities()) {
            if (shouldBeIgnored(entity)) continue;

            Color color = getEntityColor(entity);

            if (blinkOffscreen.get())
                color.a *= getAlpha();

            Vec2f screenCenter = new Vec2f(mc.getWindow().getFramebufferWidth() / 2.f, mc.getWindow().getFramebufferHeight() / 2.f);

            Vector3d projection = new Vector3d(entity.lastX, entity.lastY, entity.lastZ);
            boolean projSucceeded = NametagUtils.to2D(projection, 1, false, false);

            if (projSucceeded && projection.x > 0.f && projection.x < mc.getWindow().getFramebufferWidth() && projection.y > 0.f && projection.y < mc.getWindow().getFramebufferHeight())
                continue;

            projection = new Vector3d(entity.lastX, entity.lastY, entity.lastZ);
            NametagUtils.to2D(projection, 1, false, true);

            Vector2f angle = vectorAngles(new Vector3d(screenCenter.x - projection.x, screenCenter.y - projection.y, 0));
            angle.y += 180;

            float angleYawRad = (float) Math.toRadians(angle.y);

            Vector2f newPoint = new Vector2f(screenCenter.x + distanceOffscreen.get() * (float) Math.cos(angleYawRad),
                screenCenter.y + distanceOffscreen.get() * (float) Math.sin(angleYawRad));

            Vector2f[] trianglePoints = {
                new Vector2f(newPoint.x - sizeOffscreen.get(), newPoint.y - sizeOffscreen.get()),
                new Vector2f(newPoint.x + sizeOffscreen.get() * 0.73205f, newPoint.y),
                new Vector2f(newPoint.x - sizeOffscreen.get(), newPoint.y + sizeOffscreen.get())
            };

            rotateTriangle(trianglePoints, angle.y);

            Renderer2D.COLOR.triangle(trianglePoints[0].x, trianglePoints[0].y, trianglePoints[1].x, trianglePoints[1].y, trianglePoints[2].x,
                trianglePoints[2].y, color);

            count++;
        }

        Renderer2D.COLOR.render();
    }

    public void rotateTriangle(Vector2f[] points, float ang) {
        Vector2f triangleCenter = new Vector2f(0, 0);
        triangleCenter.add(points[0]).add(points[1]).add(points[2]).div(3.f);
        float theta = (float)Math.toRadians(ang);
        float cos = (float)Math.cos(theta);
        float sin = (float)Math.sin(theta);
        for (int i = 0; i < 3; i++) {
            Vector2f point = new Vector2f(points[i].x, points[i].y).sub(triangleCenter);

            Vector2f newPoint = new Vector2f(point.x * cos - point.y * sin, point.x * sin + point.y * cos);
            newPoint.add(triangleCenter);

            points[i] = newPoint;
        }
    }

    public Vector2f vectorAngles(final Vector3d forward) {
        float tmp, yaw, pitch;

        if (forward.x == 0 && forward.y == 0) {
            yaw = 0;
            if (forward.z > 0)
                pitch = 270;
            else
                pitch = 90;
        } else {
            yaw = (float)(Math.atan2(forward.y, forward.x) * 180 / Math.PI);
            if (yaw < 0)
                yaw += 360;

            tmp = (float)Math.sqrt(forward.x * forward.x + forward.y * forward.y);
            pitch = (float)(Math.atan2(-forward.z, tmp) * 180 / Math.PI);
            if (pitch < 0)
                pitch += 360;
        }

        return new Vector2f(pitch, yaw);
    }

    public float getAlpha() {
        double speed = blinkOffscreenSpeed.get() / 4.0;
        double duration = Math.abs(Duration.between(Instant.now(), initTimer).toMillis()) * speed;

        return (float)Math.abs((duration % 1000) - 500) / 500.f;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(count);
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
