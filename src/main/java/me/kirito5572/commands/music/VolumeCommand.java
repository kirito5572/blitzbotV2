package me.kirito5572.commands.music;

import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.PlayerManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class VolumeCommand implements ICommand {
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        MessageChannel channel = event.getMessageChannel();
        PlayerManager manager = PlayerManager.getInstance();
        AudioManager audioManager = Objects.requireNonNull(event.getGuild()).getAudioManager();

        OptionMapping opt = event.getOption("볼륨");
        int joined = opt == null ? 0 : opt.getAsInt();

        Member selfMember = event.getGuild().getSelfMember();
        if(!selfMember.hasPermission(Permission.VOICE_CONNECT)) {
            event.reply("보이스채널 권한이 없습니다..").setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        if(!audioManager.isConnected()) {
            event.reply("봇을 먼저 보이스채널에 들어오게 하세요.").setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }

        GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();

        assert memberVoiceState != null;
        if(!memberVoiceState.inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어오세요").setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        if(joined < 10) {
            event.reply("최소 볼륨은 10입니다. 10보다 큰 수를 입력해주세요.").setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

            return;
        } else if(joined > 100) {
            event.reply("최대 볼륨은 100입니다. 100보다 작은 수를 입력해주세요.").setEphemeral(true)
                    .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));

            return;
        }

        manager.getGuildMusicManager(event.getGuild()).player.setVolume(joined);

        event.reply("볼륨이 " + joined + "으로 변경되었습니다.").setEphemeral(true)
                .queue(message -> message.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
    }

    /** @noinspection unused*/
    @NotNull
    @Override
    public String getHelp() {
        return "노래 소리 조절";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "볼륨";
    }

}
