package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class QueueMixCommand implements ICommand {
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        Random random = new Random();
        PlayerManager playerManager = PlayerManager.getInstance();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(Objects.requireNonNull(event.getGuild()));
        BlockingQueue<AudioTrack> queue = musicManager.scheduler.getQueue();
        List<AudioTrack> queueList = new ArrayList<>(queue);
        Collections.shuffle(queueList, random);
        queue.clear();
        queue.addAll(queueList);
        event.reply("재생 목록이 셔플되었습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
    }

    /** @noinspection unused*/
    @NotNull
    @Override
    public String getHelp() {
        return "재생목록을 셔플 합니다.\n" +
                "단축어: qm";
    }

    /** @noinspection unused*/
    @NotNull
    @Override
    public String getInvoke() {
        return "셔플";
    }


}
