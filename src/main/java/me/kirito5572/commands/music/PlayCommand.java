package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class PlayCommand implements ICommand {
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        
        AudioManager audioManager = Objects.requireNonNull(event.getGuild()).getAudioManager();
        GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();
        assert memberVoiceState != null;
        AudioChannelUnion voiceChannel = memberVoiceState.getChannel();
        PlayerManager playerManager = PlayerManager.getInstance();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(event.getGuild());
        AudioPlayer player = musicManager.player;
        String inputUrl = event.getOption("url", OptionMapping::getAsString);
        if(!memberVoiceState.inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어오세요").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        if(!audioManager.isConnected()) {

            Member selfMember = event.getGuild().getSelfMember();

            assert voiceChannel != null;
            if(!selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
                event.replyFormat("%s 채널에 들어올 권한이 없습니다.",voiceChannel).setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                return;
            }

        }
        if(player.isPaused()) {
            if(player.getPlayingTrack() != null) {
                player.setPaused(false);
                event.reply("일시정지 된 노래가 다시 재생됩니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                return;
            } else {
                player.setPaused(false);
            }
        }
        if(inputUrl == null) {
            event.reply("URL을 입력헤주세요").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        if(!isUrl(inputUrl) && !inputUrl.startsWith("ytsearch:")) {
            event.reply("""
                    아래 적혀져있는 플랫폼만 지원합니다..
                    Youtube / Youtube music
                    SoundCloud / Bandcamp
                    Vimeo / Twitch(stream only)
                    HTTP URL(지원 확장자: MP3, FLAC, WAV, MP4(with AAC), OGG/AAC streams)
                    """).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        if(inputUrl.contains("music.youtube.com")) {
            inputUrl = inputUrl.replaceFirst("music.", "");
        }

        PlayerManager manager = PlayerManager.getInstance();
        if(!audioManager.isConnected()) {
            audioManager.openAudioConnection(voiceChannel);
            AudioManager audioManager1 = event.getGuild().getAudioManager();
            PlayerManager playerManager1 = PlayerManager.getInstance();
            GuildMusicManager musicManager1 = playerManager1.getGuildMusicManager(event.getGuild());
            Timer timer = new Timer();
            TimerTask task = new TimerTask() {
            boolean isThreadStart = false;
                @Override
                public void run() {
                    if(!musicManager1.player.isPaused()) {
                        assert voiceChannel != null;
                        if (voiceChannel.getMembers().size() < 2) {
                            Timer timer1 = new Timer();
                            final int[] i = {0};
                            musicManager1.player.setPaused(true);
                            TimerTask timerTask = new TimerTask() {
                                @Override
                                public void run() {
                                    if (voiceChannel.getMembers().size() < 2) {
                                        i[0]++;
                                    } else {
                                        i[0] = 0;
                                    }
                                    if(i[0] > 120) {
                                        audioManager.closeAudioConnection();
                                        timer1.cancel();
                                    }
                                }
                            };
                            if(!isThreadStart) {
                                timer1.scheduleAtFixedRate(timerTask, 0, 750);
                                isThreadStart = true;
                            }
                        }
                        timer.cancel();
                    }
                }
            };
            timer.scheduleAtFixedRate(task, 0, 1000);
        }
        manager.loadAndPlay(event, inputUrl);

        musicManager = manager.getGuildMusicManager(Objects.requireNonNull(event.getGuild()));
        BlockingQueue<AudioTrack> queue = musicManager.scheduler.getQueue();

        if(queue.isEmpty()) {
            manager.getGuildMusicManager(event.getGuild()).player.setVolume(50);
        }
    }

    private boolean isUrl(@NotNull String input) {
        try {
            URI uri = new URI(input);
            return true;
        } catch (URISyntaxException ignored) {
            return false;
        }
    }
    
    @Override
    public String getHelp() {
        return "URL의 노래를 재생합니다";
    }
    
    @Override
    public String getInvoke() {
        return "재생";
    }
    
}
