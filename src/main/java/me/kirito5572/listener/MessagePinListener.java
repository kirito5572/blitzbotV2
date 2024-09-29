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
        MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
        queryData.query = "SELECT * FROM blitz_bot.Pin WHERE channelId=?;";
        queryData.dataType = new int[] {mySqlConnector.STRING};
        queryData.data = new String[] {event.getChannel().getId()};

        try (ResultSet resultSet = mySqlConnector.Select_Query(queryData)) {
            if(resultSet == null) {
                return;
            }
            if(resultSet.next()) {
                try {
                    event.getChannel().retrieveMessageById(resultSet.getString("messageId")).queue(message -> {
                        MessageEmbed embed = message.getEmbeds().getFirst();
                        message.delete().queue();
                        event.getChannel().sendMessageEmbeds(embed).queue(message1 -> {
                            MySqlConnector.QueryData queryData2 = new MySqlConnector.QueryData();
                            queryData2.query = "UPDATE blitz_bot.Pin SET messageId =? WHERE channelId = ?;";
                            queryData2.dataType = new int[] {mySqlConnector.STRING, mySqlConnector.STRING};
                            queryData2.data = new String[] {message1.getId(), event.getChannel().getId()};
                            mySqlConnector.Insert_Query(queryData2);
                        });
                    });
                } catch (ErrorResponseException e) {
                    MySqlConnector.QueryData queryData2 = new MySqlConnector.QueryData();
                    queryData2.query = "DELETE FROM blitz_bot.Pin WHERE channelId=?";
                    queryData2.dataType = new int[] {mySqlConnector.STRING};
                    queryData2.data = new String[] {event.getChannel().getId()};
                    mySqlConnector.Insert_Query(queryData2);
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
