package me.kirito5572.listener;

import me.kirito5572.objects.main.MySqlConnector;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class GiveRoleListener extends ListenerAdapter {

    private final Logger logger = LoggerFactory.getLogger(GiveRoleListener.class);
    private final MySqlConnector mySqlConnector;
    private static final String Chatting = "830514311939751967";

    public GiveRoleListener(MySqlConnector mySqlConnector) {
        this.mySqlConnector = mySqlConnector;
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        logger.info("역할 지급 기능부 준비 완료");
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if(!event.isFromGuild()) {
            return;
        }
        Guild guild = event.getGuild();
        if (!guild.getId().equals("826704284003205160")) {
            return;
        }
        if (!event.getMessageId().equals(Chatting)) {
            return;
        }
        Role role = guild.getRoleById("827207197183180821");
        Member member = event.retrieveMember().complete();
        long banTime = isBan(member);
        if(banTime != 0) {
            String Date = getString(banTime);
            member.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("현재 쿨타임 중입니다.\n 쿨타임 해제 시간: " + Date)).queue();
            return;
        }
        String confirmBan = confirmCoolDown(member);
        if(confirmBan.contains("#")) {
            switch (confirmBan.split("#")[0]) {
                case "true/10" -> member.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("10초 동안 " + confirmBan.split("#")[1] + "회 이상 역할 부여를 시도하여 5분간 쿨타임에 걸렸습니다.")).queue();
                case "true/3600" -> member.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("1시간 동안 " + confirmBan.split("#")[1] + "회 이상 역할 부여를 시도하여 6시간동안 쿨타임에 걸렸습니다.")).queue();
                case "true/86400" -> member.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("하루 동안 " + confirmBan.split("#")[1] + "회 이상 역할 부여를 시도하여 7일동안 쿨타임에 걸렸습니다.")).queue();
            }
        } else {
            if(confirmBan.contains("ban")) {
                event.getGuild().ban(UserSnowflake.fromId(member.getId()), 0, TimeUnit.SECONDS).reason("역할 스팸으로 밴").queue();
                member.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessage("30일 동안 40회 이상 역할 부여를 시도하여 서버에서 밴되었습니다. 관련 문의는 <@284508374924787713> 에게 부탁드립니다.")).queue();
            }
        }
        assert role != null;
        guild.addRoleToMember(UserSnowflake.fromId(member.getId()), role).queue();
        try {
            mySqlConnector.Insert_Query("INSERT INTO blitz_bot.JoinDataTable (userId, approveTime, rejectTime) VALUES(?, ? ,?);",
                    new int[] {mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.STRING},
                    new String[] {member.getId(), String.valueOf(System.currentTimeMillis() / 1000), "0"});
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
    }

    @NotNull
    private static String getString(long banTime) {
        long time = banTime * 1000;
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);

        int mYear = calendar.get(Calendar.YEAR), mMonth = calendar.get(Calendar.MONTH) + 1, mDay = calendar.get(Calendar.DAY_OF_MONTH),
                mHour = calendar.get(Calendar.HOUR_OF_DAY), mMin = calendar.get(Calendar.MINUTE), mSec = calendar.get(Calendar.SECOND);
        return mYear + "년 " + mMonth + "월 " + mDay + "일 " + mHour + "시 " + mMin + "분 " + mSec + "초";
    }

    @Override
    public void onMessageReactionRemove(@NotNull MessageReactionRemoveEvent event) {
        Guild guild = event.getGuild();
        if (guild.getId().equals("826704284003205160")) {
            if (event.getMessageId().equals(Chatting)) {
            Role role = guild.getRoleById("827207197183180821");
            Member member = event.retrieveMember().complete();
                assert role != null;
                assert member != null;
                guild.removeRoleFromMember(UserSnowflake.fromId(member.getId()), role).queue();
                try {
                     mySqlConnector.Insert_Query("UPDATE blitz_bot.JoinDataTable SET rejectTime =? WHERE userId = ? AND rejectTime = ?",
                            new int[] {mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.STRING},
                            new String[] {String.valueOf(System.currentTimeMillis() / 1000), member.getId(), "0"});
                } catch (SQLException sqlException) {
                    logger.error(sqlException.getMessage());
                }
            }
        }
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        if (guild.getId().equals("826704284003205160")) {
            Member member = event.getMember();
            String id;
            id = Objects.requireNonNullElseGet(member, event::getUser).getId();
            try {
                mySqlConnector.Insert_Query("UPDATE blitz_bot.JoinDataTable SET rejectTime =? WHERE userId = ? AND rejectTime = ?",
                        new int[]{mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.STRING},
                        new String[] {String.valueOf(System.currentTimeMillis() / 1000),  id, "1"});
            } catch (SQLException sqlException) {
                logger.error(sqlException.getMessage());
            }
        }
    }

    /**
     * Check if the member is on cooldown(쿨타임)
     * @param member the member who check
     *
     * @return ban/error/true/false
     *     if banned, need ban
     *     if error, Unknown Member
     *     if true, Member is cooldown now, and return with time data
     *     if false, Member is clear
     */
    @NotNull
    private String confirmCoolDown(@NotNull Member member) {
        long time = System.currentTimeMillis() / 1000;
        int min = 60, hour = 3600, day = 86400;
        //[0] = 확인할 시간, [1] = 이모지 반복 횟수 [2] = 처벌시간  3*day = 3일 6*hour = 6시간
        int[][] check_time = {{10, 3, 0}, {hour, 10, 0}, {day, 20, 0}, {30*day, 40, 0}};
        check_time[0][2] = 5*min;
        check_time[1][2] = 6*hour;
        check_time[2][2] = 7*day;
        for(int i = 0; i < 4; i++) {
            try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT COUNT(*) FROM blitz_bot.JoinDataTable where approveTime > ? AND approveTime < ? AND userId = ?;;",
                    new int[]{mySqlConnector.STRING, mySqlConnector.STRING, mySqlConnector.STRING},
                    new String[]{String.valueOf(time - check_time[i][0]), String.valueOf(time), member.getId()})) {
                resultSet.next();
                if (resultSet.getInt(1) >= check_time[i][1]) {
                    if(i == 3) {
                        return "ban";
                    }
                    long end_time = (System.currentTimeMillis() / 1000) + check_time[i][2];
                    mySqlConnector.Insert_Query("INSERT INTO blitz_bot.GiveRoleBanTable (userId, endTime) VALUES(?,?);",
                            new int[]{mySqlConnector.STRING, mySqlConnector.STRING},
                            new String[]{member.getId(), String.valueOf(end_time)});
                    return "true/" + check_time[i][0] + "#" + check_time[i][1];
                }
            } catch (SQLException sqlException) {
                sqlException.fillInStackTrace();
                logger.error("에러발생!! giveRoleListener#onGuildMessageReactionAdd#cool-time");
                return "error";
            }
        }
        return "false";
    }

    /**
     * Check if the member is on cooldown(쿨타임)
     * @param member the member who need check
     *
     * @return 0 or timeData(unix time)
     * if 0, no cooldown
     * if timeData, the time the cooldown ended
     */

    private long isBan(@NotNull Member member) {
        try (ResultSet resultSet = mySqlConnector.Select_Query("SELECT * FROM blitz_bot.GiveRoleBanTable where userId = ?;",
                    new int[]{mySqlConnector.STRING},
                    new String[]{member.getId()})) {
            if (resultSet.next()) {
                return resultSet.getLong("endTime");
            }
        } catch (SQLException sqlException) {
            sqlException.fillInStackTrace();
            logger.error("에러발생!! giveRoleListener#isBan#cool-time");
        }
        return 0;
    }
}


