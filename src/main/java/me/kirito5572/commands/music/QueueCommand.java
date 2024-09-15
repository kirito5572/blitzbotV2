package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class QueueCommand implements ICommand {

    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        PlayerManager playerManager = PlayerManager.getInstance();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(Objects.requireNonNull(event.getGuild()));
        BlockingQueue<AudioTrack> queue = musicManager.scheduler.getQueue();
        AudioPlayer player = musicManager.player;

        OptionMapping opt = event.getOption("재생목록회차");
        int joined = opt == null ? 1 : opt.getAsInt();


        if(queue.isEmpty()) {
            if(player.getPlayingTrack() == null) {
                event.reply("재생목록이 비었습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

                return;
            }
        }
        int maxTrackCount;
        int minTrackCount;
        if(joined == 1) {
            maxTrackCount = Math.min(queue.size(), (20 * joined) - 1) + 2;
            minTrackCount = 0;
        } else {
            maxTrackCount = Math.min(queue.size(), (20 * joined) - 1) - 1;
            minTrackCount = Math.min(queue.size(), (20 * (joined - 1)) + 1);
        }
        List<AudioTrack> tracks = new ArrayList<>(queue);
        if(queue.size() < maxTrackCount) {
            maxTrackCount = queue.size();
        }
        if(minTrackCount > queue.size()) {
            event.reply( "해당 번호의 재생목록은 비어있습니다.\n`" +
                     (int)Math.ceil((queue.size() + 1) / 20.0) +
                    "`까지 재생목록이 존재합니다.").queue();

            return;
        }
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                .setTitle("현재 재생목록 (총합: " + (queue.size() - 1) + ") 페이지: " + joined);
        if(!queue.isEmpty()) {
            AudioTrackInfo info = player.getPlayingTrack().getInfo();
            builder.appendDescription("현재 재생중: " + info.title + " - " + info.author + "\n");
            for (int i = minTrackCount; i < maxTrackCount; i++) {
                try {
                    AudioTrack track = tracks.get(i);
                    info = track.getInfo();
                    builder.appendDescription(String.format(
                            (i) + ". %s - %s\n",
                            info.title,
                            info.author
                    ));

                } catch (IndexOutOfBoundsException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        if(queue.size() > maxTrackCount) {
            builder.appendDescription("다음 재생목록 확인: `" + getInvoke() + " " + (joined + 1) + "`");
        }

        event.replyEmbeds(builder.build()).setEphemeral(true).queue(v -> v.deleteOriginal().queueAfter(60, TimeUnit.SECONDS));
    }

    /** @noinspection unused*/
    @NotNull
    @Override
    public String getHelp() {
        return "앞으로 재생될 남은 노래 목록";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return"재생목록";
    }
}
