package me.kirito5572.commands.music;


import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.GuildMusicManager;
import me.kirito5572.objects.music.PlayerManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class StopCommand implements ICommand {
    /**
     * @noinspection unused
     */
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        PlayerManager playerManager = PlayerManager.getInstance();
        AudioManager audioManager = Objects.requireNonNull(event.getGuild()).getAudioManager();
        GuildMusicManager musicManager = playerManager.getGuildMusicManager(event.getGuild());

        if (!audioManager.isConnected()) {
            event.reply("음성 채널에 연결되어있지 않아 사용이 불가능합니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        musicManager.player.stopTrack();
        musicManager.player.setPaused(false);

        Member selfMember = event.getGuild().getSelfMember();
        if (!selfMember.hasPermission(Permission.VOICE_CONNECT)) {
            event.reply("보이스채널 권한이 없습니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        event.reply("노래 재생을 멈춥니다.").setEphemeral(true).queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

    }

    /**
     * @noinspection unused
     */
    @Override
    public String getHelp() {
        return "노래를 정지하고 봇이 나갑니다";
    }

    @Override
    public String getInvoke() {
        return "정지";
    }
}
