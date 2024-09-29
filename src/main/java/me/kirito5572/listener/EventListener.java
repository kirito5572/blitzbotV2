package me.kirito5572.listener;

import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventListener extends ListenerAdapter {
    private final Logger logger = LoggerFactory.getLogger(EventListener.class);
    private final MySqlConnector mySqlConnector;

    public EventListener(MySqlConnector mySQLConnector) {
        this.mySqlConnector = mySQLConnector;
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
        Member member = event.getMember();
        if(member == null) {
            return;
        }
        if (guild.getId().equals("826704284003205160")) {
            if (event.getMessageId().equals("1017430875480268820")) {
                try {
                    MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
                    queryData.query = "INSERT IGNORE INTO blitz_bot.event VALUES (?)";
                    queryData.dataType = new int[]{mySqlConnector.STRING};
                    queryData.data = new String[]{member.getId()};
                    mySqlConnector.Insert_Query(queryData);
                    member.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("이벤트에 정상 참여되었습니다.")).queue();
                } catch (UnsupportedOperationException ignored) {
                }
            }
        }
    }
}
