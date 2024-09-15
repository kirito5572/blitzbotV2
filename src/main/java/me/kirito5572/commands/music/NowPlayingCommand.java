package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class NowPlayingCommand implements ICommand {
    /**
     * @noinspection unused
     */
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        PlayerManager playerManager = PlayerManager.getInstance();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(Objects.requireNonNull(event.getGuild()));
        AudioPlayer player = musicManager.player;

        Member selfMember = event.getGuild().getSelfMember();
        if (!selfMember.hasPermission(Permission.VOICE_CONNECT)) {
            event.reply("봇에게 보이스 채널 입장 권한을 부여해 주세요.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        if (player.getPlayingTrack() == null) {
            event.reply("아무 노래도 재생하고 있지 않습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        AudioTrackInfo info = player.getPlayingTrack().getInfo();

        event.replyEmbeds(EmbedUtils.embedMessage(String.format(
                "**" + (player.isPaused() ? "일시정지중" : "재생중") + ": ** [%s](%s)\n%s %s/%s",
                info.title,
                info.uri,
                player.isPaused() ? "⏸" : "▶",
                formatTime(player.getPlayingTrack().getPosition()),
                formatTime(player.getPlayingTrack().getDuration())
        )).build()).setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(60, TimeUnit.SECONDS));
    }

    /**
     * @noinspection unused
     */
    public String getHelp() {
        return "현재 재생중인 음악목록을 알려줍니다.";
    }

    @Override
    public String getInvoke() {
        return "재생중";
    }


    private String formatTime(long timeInMillis) {
        final long hours = timeInMillis / TimeUnit.HOURS.toMillis(1) % 24;
        final long minutes = timeInMillis / TimeUnit.MINUTES.toMillis(1) % 60;
        final long seconds = timeInMillis % TimeUnit.MINUTES.toMillis(1) / TimeUnit.SECONDS.toMillis(1);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}