package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import me.kirito5572.objects.music.TrackScheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class SkipCommand implements ICommand {
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        PlayerManager playerManager = PlayerManager.getInstance();
        AudioManager audioManager = Objects.requireNonNull(event.getGuild()).getAudioManager();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(Objects.requireNonNull(event.getGuild()));
        TrackScheduler scheduler = musicManager.scheduler;
        AudioPlayer player = musicManager.player;
        if(!audioManager.isConnected()) {
            event.getChannel().sendMessage("음성 채널에 연결되어있지 않아 사용이 불가능합니다.").queue(message -> message.delete().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        Member selfMember = event.getGuild().getSelfMember();
        if(!selfMember.hasPermission(Permission.VOICE_CONNECT)) {
            event.reply("보이스채널 권한이 없습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        if (player.getPlayingTrack() == null) {
            event.reply("노래를 재생하고 있지 않습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

            return;
        }
        if (scheduler.getQueue().size() < 2) {
            event.reply("스킵할 노래가 존재하지 않습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

            return;
        }

        try {
            scheduler.nextTrack();
        } catch (IllegalStateException e) {
            try {
                scheduler.nextTrack();
            } catch (IllegalStateException e1) {
                try {
                    scheduler.nextTrack();
                } catch (IllegalStateException e2) {
                    StackTraceElement[] element = e.getStackTrace();
                    StringBuilder builder = new StringBuilder();
                    for(StackTraceElement traceElement : element) {
                        builder.append(traceElement.toString()).append("\n");
                    }
                    event.reply("에러가 발생했습니다.\n" +
                            builder).setEphemeral(true).queue();
                    return;
                }
            }
        }

        event.reply("다음 노래로 넘깁니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
    }

    /** @noinspection unused*/
    @NotNull
    @Override
    public String getHelp() {
        return "이 노래를 그만 재생합니다";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "스킵";
    }
    
}
