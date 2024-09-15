package me.kirito5572.commands.main.moderator;

import me.kirito5572.objects.main.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

public class KickCommand implements ICommand {
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        OptionMapping opt = event.getOption("유저");
        OptionMapping opt2 = event.getOption("사유");
        if(opt == null) {
            return;
        }
        if (!event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS)) {
            event.reply("봇이 이 명령어를 사용할 권한이 없습니다.").setEphemeral(true).queue();
            return;
        }
        Member target = opt.getAsMember();
        if(target == null) {
            event.reply("해당 유저가 존재하지 않습니다.").setEphemeral(true).queue();
            return;
        }
        if(opt2 != null) {
            String reason = opt2.getAsString();
            event.getGuild().kick(target).reason(reason).queue();
        } else {
            event.getGuild().kick(target).queue();
        }
        event.reply("추방 성공!").setEphemeral(true).queue();
    }

    @NotNull
    @Override
    public String getHelp() {
        return "서버에서 유저를 추방합니다.";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "추방";
    }
}