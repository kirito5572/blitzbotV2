package me.kirito5572.listener;

import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class MuteListener extends ListenerAdapter {
    private final MySqlConnector mySqlConnector;

    private final Logger logger = LoggerFactory.getLogger(MuteListener.class);

    public MuteListener(MySqlConnector mySqlConnector) {
        this.mySqlConnector = mySqlConnector;
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.MuteTable WHERE userId = ?",
        new int[]{mySqlConnector.STRING}, new String[]{event.getMember().getId()})) {
            if(resultSet.next()) {
                long endTimeData;
                Date date = new Date();
                endTimeData = resultSet.getLong("endTime");
                if(date.getTime() < endTimeData) {
                    Role role = event.getGuild().getRoleById("827098219061444618");
                    assert role != null;
                    event.getGuild().addRoleToMember(event.getMember(), role).queue();
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            e.fillInStackTrace();
        }
    }
}
