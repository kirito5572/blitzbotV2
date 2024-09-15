package me.kirito5572.commands.main;

import me.duncte123.botcommons.messaging.EmbedUtils;
import me.kirito5572.objects.main.ICommand;
import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

public class PingCommand implements ICommand {
    private final MySqlConnector mySqlConnector;

    public PingCommand(MySqlConnector mySqlConnector) {
        this.mySqlConnector = mySqlConnector;
    }

    @Override
    public void handle(SlashCommandInteractionEvent event) {
        long a;
        try {
            a = event.getJDA().getRestPing().submit().get();
        } catch (@NotNull InterruptedException | ExecutionException e) {
            e.fillInStackTrace();
            return;
        }
        long b = event.getJDA().getGatewayPing();
        long Start = System.currentTimeMillis();
        long mysqlEnd = System.currentTimeMillis();
        try (ResultSet ignored = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.ComplainBan", new int[]{}, new String[]{})) {
            mysqlEnd = System.currentTimeMillis();
        } catch (SQLException e){
            e.fillInStackTrace();
        }
        String mysqlTimeString;
        long mysqlTime = (mysqlEnd - Start);
        if(mysqlTime < 0) {
            mysqlTimeString = "접속 에러";
        } else if(mysqlTime == 1 || mysqlTime == 0) {
            mysqlTimeString = "<1";
        } else {
            mysqlTimeString = String.valueOf(mysqlTime);
        }
        EmbedBuilder embedBuilder = EmbedUtils.getDefaultEmbed()
                .setTitle("ping-pong!")
                .addField("API 응답 시간 (UDP/TCP)", b + "ms / " + a + "ms", false)
                .addField("SQL 명령어 처리 시간  (MySQL)", mysqlTimeString + "ms", false);

        event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();

    }

    @NotNull
    @Override
    public String getHelp() {
        return "BlitzBot의 핑을 조회합니다. 봇에 연결된 SQL 서버 또한 함께 조회합니다.";
    }

    @NotNull
    @Override
    public String getInvoke() {
        return "핑";
    }

}