package me.kirito5572.commands.main.moderator;

import me.duncte123.botcommons.messaging.EmbedUtils;
import me.kirito5572.objects.main.ICommand;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserInfoCommand implements ICommand {
    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        OptionMapping userOption = event.getOption("유저");

        if (userOption == null) {
            event.reply("해당 유저를 찾을 수 없습니다.").setEphemeral(true).queue();
            return;
        }
        Member member = userOption.getAsMember();
        if(member == null) {
            event.reply("해당 유저를 찾을 수 없습니다.").setEphemeral(true).queue();
            return;
        }
        User user = member.getUser();
        StringBuilder serverRole = new StringBuilder();
        List<Role> role = member.getRoles();
        for (Role value : role) {
            serverRole.append(value.getAsMention()).append("\n");
        }

        MessageEmbed embed = EmbedUtils.getDefaultEmbed()
                .setColor(member.getColor())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .addField("유저이름#번호", String.format("%#s", user), false)
                .addField("서버 표시 이름", member.getEffectiveName(), false)
                .addField("서버 소유 여부 ", member.isOwner() ? "예" : "아니요", false)
                .addField("유저 ID + 언급 멘션", String.format("%s (%s)", user.getId(), member.getAsMention()), false)
                .addField("디스코드 가입 일자", user.getTimeCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())), false)
                .addField("서버 초대 일자", member.getTimeJoined().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())), false)
                .addField("서버 부여 역할", serverRole.toString(), false)
                .addField("온라인 상태", member.getOnlineStatus().name().toLowerCase().replaceAll("_", " "), false)
                .addField("봇 여부", user.isBot() ? "예" : "아니요", false)
                .build();

        event.replyEmbeds(embed).setEphemeral(true).queue();
    }

    @NotNull
    @Override
    public String getHelp() {
        return "(관리자 전용) 서버에 있는 유저의 정보를 불러옵니다.";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "유저정보";
    }
}
