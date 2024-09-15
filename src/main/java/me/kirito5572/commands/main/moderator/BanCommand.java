package me.kirito5572.commands.main.moderator;

import me.kirito5572.objects.main.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class BanCommand implements ICommand {
    private final Logger logger = LoggerFactory.getLogger(BanCommand.class);

    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("이 명령어를 사용하기 위한 권한이 없습니다").setEphemeral(true).queue();
            return;
        }
        Member selfMember = Objects.requireNonNull(event.getGuild()).getSelfMember();
        if (!selfMember.hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("봇이 밴할 권한이 부여되어 있지 않습니다.").setEphemeral(true).queue();
            return;
        }
        Member member = event.getMember();
        User targetUser = event.getOption("유저", OptionMapping::getAsUser);
        Member target = event.getOption("유저", OptionMapping::getAsMember);
        String reason = event.getOption("사유", OptionMapping::getAsString);

        if((targetUser == null) || (target == null)) {
            event.reply("해당 유저를 찾을 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        if (!member.canInteract(target)) {
            event.reply("해당 사용자를 밴할 수 없습니다.").setEphemeral(true).queue();
            return;
        }

        if (!selfMember.canInteract(target)) {
            event.reply("봇이 해당 사용자를 밴할 수 없습니다.").setEphemeral(true).queue();
            return;
        }
        event.deferReply().queue();

        AuditableRestAction<Void> action = event.getGuild().ban(target, 0, TimeUnit.SECONDS);
        if (reason != null) {
            action = action.reason(reason);
        }
        action.queue(_ -> event.getHook().editOriginal("**" + event.getUser().getName() + "** 에게 **" + targetUser.getName() + "** 가 밴되었습니다!").queue(), _ -> {
            // Tell the user we encountered some error
            event.getHook().editOriginal("에러가 발생했습니다. 다시 시도해주세요.").queue();
            logger.warn("에러 발생 BanCommand:62");
        });
    }

    @NotNull
    @Override
    public String getHelp() {
        return "유저를 서버에서 밴합니다.";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "밴";
    }
}
