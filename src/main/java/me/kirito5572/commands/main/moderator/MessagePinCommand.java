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
        MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
        queryData.query = "SELECT * FROM blitz_bot.Pin WHERE channelId=?;";
        queryData.dataType = new int[]{mySqlConnector.STRING};
        queryData.data = new String[]{channelId};
        try (ResultSet resultSet = mySqlConnector.Select_Query(queryData)) {
            if(resultSet == null) {
                return;
            }
            if(resultSet.next()) {
                event.getChannel().deleteMessageById(resultSet.getString("messageId")).queue();
                MySqlConnector.QueryData queryData1 = new MySqlConnector.QueryData();
                queryData1.query = "DELETE FROM blitz_bot.Pin WHERE channelId=?";
                queryData1.dataType = new int[]{mySqlConnector.STRING};
                queryData1.data = new String[]{channelId};
                mySqlConnector.Insert_Query(queryData1);
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
                    MySqlConnector.QueryData queryData2 = new MySqlConnector.QueryData();
                    queryData2.query = "INSERT INTO blitz_bot.Pin (channelId, messageId) VALUES (?, ?)";
                    queryData2.dataType = new int[]{mySqlConnector.STRING, mySqlConnector.STRING};
                    queryData2.data = new String[]{channelId, messageId};
                    mySqlConnector.Insert_Query(queryData2);
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

    @NotNull
    @Override
    public String getInvoke() {
        return "핀";
    }
}
