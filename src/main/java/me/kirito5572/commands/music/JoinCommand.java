package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** @noinspection unused*/
public class JoinCommand implements ICommand {
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        AudioManager audioManager = Objects.requireNonNull(event.getGuild()).getAudioManager();

        if(audioManager.isConnected()) {
            event.reply("이미 보이스채널에 들어왔습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();

        assert memberVoiceState != null;
        if(!memberVoiceState.inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어오세요").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        AudioChannelUnion voiceChannel = memberVoiceState.getChannel();
        Member selfMember = event.getGuild().getSelfMember();

        assert voiceChannel != null;
        if(!selfMember.hasPermission(voiceChannel, Permission.VOICE_CONNECT)) {
            event.replyFormat("%s 보이스 채널에 들어올 권한이 없습니다.",voiceChannel).setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        audioManager.openAudioConnection(voiceChannel);
        event.reply("보이스채널에 들어왔습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
        AudioManager audioManager1 = event.getGuild().getAudioManager();
        PlayerManager playerManager = PlayerManager.getInstance();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(event.getGuild());
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(!musicManager.player.isPaused()) {
                    autoPaused(event, audioManager, voiceChannel, musicManager);
                    timer.cancel();
                }
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    static void autoPaused(@NotNull SlashCommandInteractionEvent event, @NotNull AudioManager audioManager, @NotNull AudioChannelUnion voiceChannel, @NotNull GuildMusicManager musicManager) {
        if (voiceChannel.getMembers().size() < 2) {
            musicManager.player.isPaused();
            musicManager.player.setPaused(true);
            int sleep = 750;
            try(final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1)) {
                AtomicInteger i = new AtomicInteger();

                executor.scheduleAtFixedRate(() -> {
                    if (voiceChannel.getMembers().size() < 2) {
                        i.getAndIncrement();
                    } else {
                        executor.shutdown();
                    }
                    if (i.get() > 120) {
                        audioManager.closeAudioConnection();
                        executor.shutdown();
                    }
                }, 0, sleep, TimeUnit.MILLISECONDS);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }


    /** @noinspection unused*/
    @NotNull
    @Override
    public String getHelp() {
        return "음성 채널에 봇이 들어옵니다.";
    }

    @NotNull
    @Override
    public String getInvoke() { return "입장";
    }


}
