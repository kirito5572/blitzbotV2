package me.kirito5572.listener;

import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MessagePinListener extends ListenerAdapter {
    private final MySqlConnector mySqlConnector;
    private final Logger logger = LoggerFactory.getLogger(MessagePinListener.class);

    public MessagePinListener(MySqlConnector sqliteConnector) {
        this.mySqlConnector = sqliteConnector;
    }

    public void onReady(@NotNull ReadyEvent event) {
        logger.info(" 메세지 고정 기능부 준비 완료");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(!event.isFromGuild()) {
            return;
        }
        if(event.getAuthor().getId().equals(event.getGuild().getSelfMember().getId())) {
            return;
        }
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.Pin WHERE channelId=?;", new int[]{mySqlConnector.STRING}, new String[]{event.getChannel().getId()})) {
            if(resultSet.next()) {
                try {
                    event.getChannel().retrieveMessageById(resultSet.getString("messageId")).queue(message -> {
                        MessageEmbed embed = message.getEmbeds().getFirst();
                        message.delete().queue();
                        event.getChannel().sendMessageEmbeds(embed).queue(message1 -> {
                            try {
                                mySqlConnector.Insert_Query("UPDATE blitz_bot.Pin SET messageId =? WHERE channelId = ?;",
                                        new int[]{mySqlConnector.STRING, mySqlConnector.STRING},
                                        new String[]{message1.getId(), event.getChannel().getId()});
                            } catch (SQLException sqlException) {
                                logger.error(sqlException.getMessage());
                                sqlException.fillInStackTrace();
                            }
                        });
                    });
                } catch (ErrorResponseException e) {
                    mySqlConnector.Insert_Query("DELETE FROM blitz_bot.Pin WHERE channelId=?", new int[]{mySqlConnector.STRING}, new String[]{event.getChannel().getId()});
                }
            }
        } catch (Exception e) {
            e.fillInStackTrace();
            try {
                mySqlConnector.reConnection();
            } catch (SQLException sqlException) {
                logger.error(sqlException.getMessage());
            }
        }
    }
}
