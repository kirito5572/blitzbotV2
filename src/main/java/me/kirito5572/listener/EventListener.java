package me.kirito5572.listener;

import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class EventListener extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(EventListener.class);
    private final MySqlConnector mySQLConnector;

    public EventListener(MySqlConnector mySQLConnector) {
        this.mySQLConnector = mySQLConnector;
    }


    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("이벤트 처리부 시작 완료");
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(!event.isFromGuild()) {
            return;
        }
        Guild guild = event.getGuild();
        if (guild.getId().equals("826704284003205160")) {
            if (event.getMessageId().equals("1017430875480268820")) {
                try {
                    try {
                        mySQLConnector.Insert_Query("INSERT IGNORE INTO blitz_bot.event VALUES (?)",
                                new int[]{mySQLConnector.STRING},
                                new String[]{event.getMember().getId()});
                        event.getMember().getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("이벤트에 정상 참여되었습니다.")).queue();
                    } catch (SQLException sqlException) {
                        event.getMember().getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("""
                                이벤트에 정상 참여되지 않았습니다.
                                이미 이벤트에 참여되었거나 혹은 에러가 발생한 것입니다.
                                다시 한번 이모지를 클릭하여 보시고, 그래도 되지 않을 경우 문의 부탁드립니다.""")).queue();
                    }
                } catch (UnsupportedOperationException ignored) {
                }
            }
        }
    }
}
