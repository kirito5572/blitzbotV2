package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class LeaveCommand implements ICommand {
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        AudioManager audioManager = Objects.requireNonNull(event.getGuild()).getAudioManager();
        PlayerManager playerManager = PlayerManager.getInstance();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(event.getGuild());
        Member selfMember = event.getGuild().getSelfMember();
        if(!selfMember.hasPermission(Permission.VOICE_CONNECT)) {
            event.reply("보이스채널 권한이 없습니다..").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        AudioChannelUnion voiceChannel = audioManager.getConnectedChannel();
        if((voiceChannel == null) || (!audioManager.isConnected())) {
            event.reply("봇이 보이스 채널에 있지 않습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        if(!voiceChannel.getMembers().contains(event.getMember())) {
            event.reply("봇과 같은 보이스 채널에 있어야 합니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        audioManager.closeAudioConnection();
        musicManager.scheduler.getQueue().clear();

        event.reply("보이스채널을 떠납니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
    }

    /** @noinspection unused*/
    @Override
    public String getHelp() {
        return "노래를 정지하고 나갑니다";
    }
    
    @Override
    public String getInvoke() {
        return "퇴장";
    }
    
}
