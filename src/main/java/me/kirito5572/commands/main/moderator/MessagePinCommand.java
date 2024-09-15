package me.kirito5572.commands.main.moderator;

import me.duncte123.botcommons.messaging.EmbedUtils;
import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MessagePinCommand implements ICommand {
    private final MySqlConnector mySqlConnector;

    public MessagePinCommand(MySqlConnector mySqlConnector) {
        this.mySqlConnector = mySqlConnector;
    }

    @Override
    public void handle(@NotNull SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        assert member != null;
        if (!member.getRoles().contains(Objects.requireNonNull(event.getGuild()).getRoleById("827009999145926657"))) {
            if (!member.getRoles().contains(event.getGuild().getRoleById("827010848442548254"))) {
                return;
            }
        }
        String channelId = event.getChannel().getId();
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.Pin WHERE channelId=?;", new int[]{mySqlConnector.STRING}, new String[]{channelId})) {
            if(resultSet.next()) {
                event.getChannel().deleteMessageById(resultSet.getString("messageId")).queue();
                mySqlConnector.Insert_Query("DELETE FROM blitz_bot.Pin WHERE channelId=?;" , new int[]{mySqlConnector.STRING}, new String[]{channelId});
                event.getChannel().sendMessage("핀이 해제되었습니다.").queue(message -> message.delete().queueAfter(10, TimeUnit.SECONDS));
            } else {
                OptionMapping opt = event.getOption("메세지");
                OptionMapping opt2 = event.getOption("파일");
                if (opt == null) {
                    event.reply("처리중 에러가 발생했습니다.").setEphemeral(true).queue();
                    return;
                }
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                        .setTitle("고정된 메세지")
                        .setColor(Color.GREEN)
                        .setDescription(opt.getAsString());
                if(opt2 != null) {
                    Message.Attachment attachment = opt2.getAsAttachment();
                    builder.setImage(attachment.getUrl());
                }
                event.getChannel().sendMessageEmbeds(builder.build()).queue(message -> {
                    String messageId = message.getId();
                    try {
                        mySqlConnector.Insert_Query("INSERT INTO blitz_bot.Pin (channelId, messageId) VALUES (?, ?)",
                                new int[]{mySqlConnector.STRING, mySqlConnector.STRING},
                                new String[]{channelId, messageId});
                    } catch (SQLException sqlException) {
                        sqlException.fillInStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.fillInStackTrace();
        }
        event.reply("처리가 완료되었습니다.").setEphemeral(true).queue();
    }

    @Override
    public @NotNull String getHelp() {
        return "(관리자 전용)메세지를 고정합니다.";
    }

    @Override
    public String getInvoke() {
        return "핀";
    }
}
