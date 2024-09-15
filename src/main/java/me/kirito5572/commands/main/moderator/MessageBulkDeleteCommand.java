package me.kirito5572.commands.main.moderator;

import me.kirito5572.objects.main.ICommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MessageBulkDeleteCommand implements ICommand {
    //https://github.com/DV8FromTheWorld/JDA/wiki/10)-FAQ#what-is-the-best-way-to-delete-messages-from-history

    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        MessageChannelUnion channel = event.getChannel();
        Member selfMember = Objects.requireNonNull(event.getGuild()).getSelfMember();
        Member member = event.getMember();
        if(!selfMember.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reply("봇이 메세지를 삭제할 권한이 없습니다.").setEphemeral(true).queue();
            return;
        }
        assert member != null;
        if(!member.hasPermission(Permission.MESSAGE_MANAGE)) {
            event.reply("이 명령어를 사용할 권한이 없습니다!").setEphemeral(true).queue();
            return;
        }
        OptionMapping opt = event.getOption("수량");
        int deleteCount = opt == null ? 1 : opt.getAsInt();
        if (deleteCount < 1) {
            event.reply("1보다 큰 숫자를 입력해주세요").setEphemeral(true).queue();
        } else if (deleteCount > 100) {
            event.reply("100보다 작은 숫자를 입력해주세요").setEphemeral(true).queue();
        }
        channel.getIterableHistory()
                .takeAsync(deleteCount)
                .thenApplyAsync((messages) -> {
                    List<Message> goodMessages = messages.stream()
                            .filter((m) -> m.getTimeCreated().isBefore(
                                    OffsetDateTime.now().plusWeeks(2)
                            ))
                            .collect(Collectors.toList());

                    channel.purgeMessages(goodMessages);

                    return goodMessages.size();
                })
                .whenCompleteAsync((count, _) -> event.replyFormat("`%d` 개의 채팅 삭제 완료", count).setEphemeral(true).queue())
                .exceptionally((thr) -> {
                    String cause = "";

                    if (thr.getCause() != null) {
                        cause = " 에러 발생 사유: " + thr.getCause().getMessage();
                    }

                    event.replyFormat("에러: %s%s", thr.getMessage(), cause).setEphemeral(true).queue();

                    return 0;
                });


    }

    @Override
    public String getHelp() {
        return "채팅을 대량으로 삭제시킵니다";
    }

    @Override
    public String getInvoke() {
        return "삭제";
    }

}
