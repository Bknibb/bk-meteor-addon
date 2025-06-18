package org.bknibb.bk_meteor_addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.commands.arguments.PlayerArgumentType;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec2f;
import org.bknibb.bk_meteor_addon.modules.PlayerTracers;
import org.joml.Vector2f;
import org.joml.Vector3d;

import java.time.Instant;

public class LocatePlayerCommand extends Command {
    public LocatePlayerCommand() {
        super("locate-player", "Will temporarily show a tracer to the player for 5 seconds.");
    }

    private PlayerEntity targetPlayer;
    private boolean running = false;
    private Instant startTimer;
    private final long showTime = 5;

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
            targetPlayer = PlayerArgumentType.get(context);
            startTimer = Instant.now();
            if (!running) {
                running = true;
                MeteorClient.EVENT_BUS.subscribe(this);
            }
            return SINGLE_SUCCESS;
        }));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (Modules.get().get(PlayerTracers.class).style.get() != PlayerTracers.TracerStyle.Offscreen && Instant.now().isAfter(startTimer.plusSeconds(showTime))) {
            running = false;
            MeteorClient.EVENT_BUS.unsubscribe(this);
            return;
        }

        if (mc.options.hudHidden || Modules.get().get(PlayerTracers.class).style.get() == PlayerTracers.TracerStyle.Offscreen) return;
        Color color =  Modules.get().get(PlayerTracers.class).getEntityColor(targetPlayer);

        double x = targetPlayer.lastX + (targetPlayer.getX() - targetPlayer.lastX) * event.tickDelta;
        double y = targetPlayer.lastY + (targetPlayer.getY() - targetPlayer.lastY) * event.tickDelta;
        double z = targetPlayer.lastZ + (targetPlayer.getZ() - targetPlayer.lastZ) * event.tickDelta;

        double height = targetPlayer.getBoundingBox().maxY - targetPlayer.getBoundingBox().minY;
        if (Modules.get().get(PlayerTracers.class).target.get() == Target.Head) y += height;
        else if (Modules.get().get(PlayerTracers.class).target.get() == Target.Body) y += height / 2;

        event.renderer.line(RenderUtils.center.x, RenderUtils.center.y, RenderUtils.center.z, x, y, z, color);
        if (Modules.get().get(PlayerTracers.class).stem.get()) event.renderer.line(x, targetPlayer.getY(), z, x, targetPlayer.getY() + height, z, color);
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (Modules.get().get(PlayerTracers.class).style.get() == PlayerTracers.TracerStyle.Offscreen && Instant.now().isAfter(startTimer.plusSeconds(showTime))) {
            running = false;
            MeteorClient.EVENT_BUS.unsubscribe(this);
            return;
        }
        if (mc.options.hudHidden || Modules.get().get(PlayerTracers.class).style.get() != PlayerTracers.TracerStyle.Offscreen) return;

        Renderer2D.COLOR.begin();

        Color color = Modules.get().get(PlayerTracers.class).getEntityColor(targetPlayer);

        if (Modules.get().get(PlayerTracers.class).blinkOffscreen.get())
            color.a *= Modules.get().get(PlayerTracers.class).getAlpha();

        Vec2f screenCenter = new Vec2f(mc.getWindow().getFramebufferWidth() / 2.f, mc.getWindow().getFramebufferHeight() / 2.f);

        Vector3d projection = new Vector3d(targetPlayer.lastX, targetPlayer.lastY, targetPlayer.lastZ);
        boolean projSucceeded = NametagUtils.to2D(projection, 1, false, false);

        if (projSucceeded && projection.x > 0.f && projection.x < mc.getWindow().getFramebufferWidth() && projection.y > 0.f && projection.y < mc.getWindow().getFramebufferHeight())
            return;

        projection = new Vector3d(targetPlayer.lastX, targetPlayer.lastY, targetPlayer.lastZ);
        NametagUtils.to2D(projection, 1, false, true);

        Vector2f angle = Modules.get().get(PlayerTracers.class).vectorAngles(new Vector3d(screenCenter.x - projection.x, screenCenter.y - projection.y, 0));
        angle.y += 180;

        float angleYawRad = (float) Math.toRadians(angle.y);

        Vector2f newPoint = new Vector2f(screenCenter.x + Modules.get().get(PlayerTracers.class).distanceOffscreen.get() * (float) Math.cos(angleYawRad),
                screenCenter.y + Modules.get().get(PlayerTracers.class).distanceOffscreen.get() * (float) Math.sin(angleYawRad));

        Vector2f[] trianglePoints = {
                new Vector2f(newPoint.x - Modules.get().get(PlayerTracers.class).sizeOffscreen.get(), newPoint.y - Modules.get().get(PlayerTracers.class).sizeOffscreen.get()),
                new Vector2f(newPoint.x + Modules.get().get(PlayerTracers.class).sizeOffscreen.get() * 0.73205f, newPoint.y),
                new Vector2f(newPoint.x - Modules.get().get(PlayerTracers.class).sizeOffscreen.get(), newPoint.y + Modules.get().get(PlayerTracers.class).sizeOffscreen.get())
        };

        Modules.get().get(PlayerTracers.class).rotateTriangle(trianglePoints, angle.y);

        Renderer2D.COLOR.triangle(trianglePoints[0].x, trianglePoints[0].y, trianglePoints[1].x, trianglePoints[1].y, trianglePoints[2].x,
                trianglePoints[2].y, color);

        Renderer2D.COLOR.render();
    }
}
