package org.bknibb.bk_meteor_addon.mixin.meteor_rejects;

import anticope.rejects.modules.ChatBot;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.collection.ArrayListDeque;
import org.bknibb.bk_meteor_addon.BkMeteorAddon;
import org.bknibb.bk_meteor_addon.ConfigModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(ChatBot.class)
public class ChatBotMixin {
    @Shadow(remap = false) @Final
    private SettingGroup sgGeneral;
    @Unique private Setting<Integer> messageDelay;
    private int timer;
    @Unique private final ArrayListDeque<String> messageQueue = new ArrayListDeque<>();
    @Unique private static final ThreadLocal<String> currentCmd = new ThreadLocal<>();
    @Redirect(method = "onMessageRecieve", at = @At(value = "INVOKE", target = "Lmeteordevelopment/meteorclient/utils/player/ChatUtils;sendPlayerMsg(Ljava/lang/String;)V"), remap = false)
    private void sendPlayerMsg(String message, ReceiveMessageEvent event) {
        if (ConfigModifier.get().chatBotSender.get() && message.contains("<sender>")) {
            Pattern pattern = Pattern.compile(
                "^" +
                    "(?:<\\d{1,2}:\\d{2}>\\s*)?" +                          // optional <timestamp>
                    "(?:\\[\\s*([a-zA-Z0-9_.]+)\\s*->\\s*[a-zA-Z0-9_.]+\\s*\\]\\s*)?" + // [sender -> target] → group 1
                    "(?:\\[[^\\]]*\\]\\s*)*" +                              // skip other bracketed tags like [M]
                    "(?:<\\s*([a-zA-Z0-9_.]+)\\s*>\\s*|([a-zA-Z0-9_.]+):|[a-zA-Z0-9_.]+\\s+([a-zA-Z0-9_.]+):)?"
                // group 2 = <sender>, group 3 = sender:, group 4 = tag sender:
            );

            Matcher matcher = pattern.matcher(event.getMessage().getString());
            if (matcher.find()) {
                String name = matcher.group(1) != null ? matcher.group(1) : (matcher.group(2) != null ? matcher.group(2) : (matcher.group(3) != null ? matcher.group(3) : matcher.group(4)));
                message = message.replace("<sender>", name).replace("<pms>", "<pms:" + name + ">");
                if (matcher.group(1) != null) {
                    message = message.replace("<pmsipm>", "<pms:" + name + ">");
                } else {
                    message = message.replace("<pmsipm>", "");
                }
            }
        }
        if (ConfigModifier.get().chatBotArgs.get() && message.contains("<args>")) {
            int index = event.getMessage().getString().indexOf(currentCmd.get()) + currentCmd.get().length()+1;
            if (index < event.getMessage().getString().length()) {
                message = message.replace("<args>", event.getMessage().getString().substring(index));
            } else {
                //message = message.replace("<args>", "");
                if (messageDelay != null) {
                    messageQueue.addLast("Please provide an argument for this command!");
                } else {
                    ChatUtils.sendPlayerMsg("Please provide an argument for this command!");
                }
                return;
            }
        }
        if (messageDelay != null) {
            messageQueue.addLast(message);
        } else {
            if (message.contains("<pms:")) {
                ChatUtils.sendPlayerMsg("/msg " + message.substring(message.indexOf("<pms:") + 5, message.indexOf(">")) + " " + message.substring(0, message.indexOf("<pms:")) + message.substring(message.indexOf(">") + 1));
            } else {
                ChatUtils.sendPlayerMsg(message);
            }
        }
    }
    @Redirect(method = "onMessageRecieve", at = @At(value = "INVOKE", target = "Ljava/lang/String;endsWith(Ljava/lang/String;)Z"), remap = false)
    private boolean endsWith(String message, String command) {
        currentCmd.set(command);
        if (ConfigModifier.get().chatBotArgs.get()) {
            return message.endsWith(command) || message.contains(command + " ");
        }
        return message.endsWith(command);
    }
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void onInit(CallbackInfo ci) {
        if (ConfigModifier.get().chatBotMessageDelayOption.get()) {
            messageDelay = sgGeneral.add(new IntSetting.Builder()
                .name("message-delay")
                .description("How long to wait in ticks before replying to a command.")
                .range(0, 1000)
                .sliderRange(0, 100)
                .defaultValue(0)
                .build()
            );
            //MeteorClient.EVENT_BUS.subscribe(this);
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (messageDelay == null) return;
        if (messageQueue.isEmpty()) return;
        timer++;
        while (timer >= messageDelay.get() && !messageQueue.isEmpty()) {
            timer = 0;
            String message = messageQueue.removeFirst();
            if (message.contains("<pms:")) {
                ChatUtils.sendPlayerMsg("/msg " + message.substring(message.indexOf("<pms:") + 5, message.indexOf(">")) + " " + message.substring(0, message.indexOf("<pms:")) + message.substring(message.indexOf(">") + 1));
            } else {
                ChatUtils.sendPlayerMsg(message);
            }
        }
    }
    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        timer = 0;
    }
    @EventHandler
    private void onGameLeave(GameLeftEvent event) {
        messageQueue.clear();
    }
}
