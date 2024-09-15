package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class QueueDetectCommand implements ICommand {
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        new Thread(() -> {
            PlayerManager playerManager = PlayerManager.getInstance();
            GuildMusicManager musicManager = playerManager.getGuildMusicManager(Objects.requireNonNull(event.getGuild()));
            BlockingQueue<AudioTrack> queue = musicManager.scheduler.getQueue();

            OptionMapping opt = event.getOption("삭제할_수량");
            int joined = opt == null ? 0 : opt.getAsInt();

            Member selfMember = event.getGuild().getSelfMember();
            if (!selfMember.hasPermission(Permission.VOICE_CONNECT)) {
                event.reply("보이스채널 권한이 없습니다..").queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                return;
            }

            if (queue.isEmpty()) {
                event.reply("재생목록이 비었습니다.").queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

                return;
            }
            event.deferReply().queue();

            event.reply("재생목록을 비우는 중입니다.").setEphemeral(true).queue(v -> {
                if (joined == 0) {
                    musicManager.scheduler.getQueue().clear();
                    event.getHook().editOriginal("재생목록을 초기화 했습니다.").queue(v1 -> event.getHook().deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                } else {
                    for (int i = 0; i < joined; i++) {
                        musicManager.scheduler.nextTrack();
                        event.getHook().editOriginal("재생목록에서" + joined + "개의 노래를 삭제했습니다.").queue(v1 -> event.getHook().deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                    }
                }
            });

        }).start();
    }

    /** @noinspection unused*/
    @NotNull
    @Override
    public String getHelp() {
        return "재생목록을 정리합니다.";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "재생목록삭제";
    }

}
