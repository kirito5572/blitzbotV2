package me.kirito5572.commands.music;

import me.kirito5572.objects.main.GoogleAPI;
import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.music.PlayerManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** @noinspection unused*/
public class SearchCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(SearchCommand.class);
    private final GoogleAPI googleAPI;

    public SearchCommand(GoogleAPI googleAPI) {
        this.googleAPI = googleAPI;
    }
    /** @noinspection unused*/
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        AudioManager audioManager = Objects.requireNonNull(event.getGuild()).getAudioManager();
        GuildVoiceState memberVoiceState = Objects.requireNonNull(event.getMember()).getVoiceState();
        if(!audioManager.isConnected()) {
            event.getChannel().sendMessage("음성 채널에 연결되어있지 않아 사용이 불가능합니다.").queue(message -> message.delete().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        assert memberVoiceState != null;
        AudioChannelUnion audioChannel = memberVoiceState.getChannel();
        if(!audioManager.isConnected()) {
            Member selfMember = event.getGuild().getSelfMember();

            if(audioChannel == null) {
                event.reply("먼저 보이스 채널에 들어오세요").setEphemeral(true)
                        .queue(v -> v.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                return;
            }
            if(!selfMember.hasPermission(audioChannel, Permission.VOICE_CONNECT)) {
                event.replyFormat("%s 보이스 채널에 들어올 권한이 없습니다.",audioChannel).setEphemeral(true)
                        .queue(v -> v.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
                return;
            }
        }
        if(!memberVoiceState.inAudioChannel()) {
            event.reply("먼저 보이스 채널에 들어오세요").setEphemeral(true)
                    .queue(v -> v.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        String name = event.getOption("검색", OptionMapping::getAsString);
        if(name == null) {
            event.reply("검색할 내용을 입력하세요.").setEphemeral(true)
                    .queue(v -> v.deleteOriginal().queueAfter(7, TimeUnit.SECONDS));
            return;
        }
        String[][] data = googleAPI.Search(name);
        if(data == null) {
            event.getChannel().sendMessage("youtube 검색에 문제가 발생했습니다").queue();
            return;
        }
        PlayerManager manager = PlayerManager.getInstance();
        event.reply("노래가 추가되었습니다.").setEphemeral(true).queue();
        manager.loadAndPlay(event, "https://youtu.be/" + data[0][1]);

    }

    /** @noinspection unused*/
    @NotNull
    @Override
    public String getHelp() {
        return "유튜브에서 노래를 검색합니다";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "검색";
    }

}
