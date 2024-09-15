package me.kirito5572.objects.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PlayerManager {
    private static PlayerManager INSTANCE;
    @NotNull
    private final AudioPlayerManager playerManager;
    @NotNull
    private final Map<Long, GuildMusicManager> musicManagers;

    private PlayerManager() {
        this.musicManagers = new HashMap<>();

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public synchronized GuildMusicManager getGuildMusicManager(@NotNull Guild guild) {
        long guildID = guild.getIdLong();
        GuildMusicManager musicManager = musicManagers.get(guildID);

        if(musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildID, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }

    public void loadAndPlay(@NotNull SlashCommandInteractionEvent event, String trackUrl) {
        GuildMusicManager musicManager = getGuildMusicManager(Objects.requireNonNull(event.getGuild()));

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(@NotNull AudioTrack track) {
                event.getHook().editOriginal("재생목록에 추가:" + track.getInfo().title).queue(v -> event.getHook().deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

                play(musicManager, track);
            }

            @Override
            public void playlistLoaded(@NotNull AudioPlaylist playlist) {
                AudioTrack firstTrack = playlist.getSelectedTrack();

                if (firstTrack == null) {
                    firstTrack = playlist.getTracks().remove(0);
                }

                event.getHook().editOriginal("재생목록에 추가:" + firstTrack.getInfo().title +
                        " ("+ playlist.getName() + "의 첫번째 곡)").queue(v -> event.getHook().deleteOriginal().queueAfter(15, TimeUnit.SECONDS));

                play(musicManager, firstTrack);

                playlist.getTracks().forEach(musicManager.scheduler::queue);
            }

            @Override
            public void noMatches() {
                event.reply(trackUrl + "과 같은 결과를 찾지 못했습니다").setEphemeral(true).queue(v -> v.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            }

            @Override
            public void loadFailed(@NotNull FriendlyException exception) {
                event.reply("로딩 실패: " + exception.getMessage()).setEphemeral(true).queue(v -> v.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            }
        });
    }

    private void play(@NotNull GuildMusicManager musicManager, @NotNull AudioTrack track) {
        musicManager.scheduler.queue(track);
    }

    public static synchronized PlayerManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new PlayerManager();
        }

        return INSTANCE;
    }
}
