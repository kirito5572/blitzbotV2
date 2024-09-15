package me.kirito5572.objects.main;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FilterSystem {
    private final Logger logger = LoggerFactory.getLogger(FilterSystem.class);
    private final List<String> filterList = new ArrayList<>();
    private final List<String[]> whiteFilterList = new ArrayList<>();
    private final MySqlConnector mySQLConnector;

    public FilterSystem(MySqlConnector mySQLConnector) {
        this.mySQLConnector = mySQLConnector;
    }


    /**
     * Refresh filter words from database
     * @return if True, refresh success, if False, refresh fail
     */
    public boolean filterRefresh() {
        boolean isRefreshEnd = false;
        try (ResultSet resultSet = mySQLConnector.Select_Query(
                "SELECT * FROM blitz_bot.filterWord;",
                new int[]{},
                new String[]{})) {
            filterList.clear();
            isRefreshEnd = true;
            while(resultSet.next()) {
                filterList.add(resultSet.getString("Word"));
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
            logger.error(sqlException.getSQLState());
            sqlException.fillInStackTrace();
        }
        return isRefreshEnd;
    }

    /**
     * Refresh filter's white list from database
     * @return if True, refresh success, if False, refresh fail
     */

    public boolean whiteFilterRefresh() {
        boolean isRefreshEnd = false;
        try (ResultSet resultSet = mySQLConnector.Select_Query(
                "SELECT * FROM blitz_bot.whiteListWord;",
                new int[]{},
                new String[]{})) {
            whiteFilterList.clear();
            isRefreshEnd = true;
            while(resultSet.next()) {
                String[] data = new String[2];
                data[0] = resultSet.getString("FilterWord");
                data[1] = resultSet.getString("Word");
                whiteFilterList.add(data);
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
            logger.error(sqlException.getSQLState());
            sqlException.fillInStackTrace();
        }

        return isRefreshEnd;
    }

    public void wordUpdate(boolean isWhiteList, boolean isInsert, String[] word) throws SQLException {
        if(isInsert) {
            //INSERT WORD
            if(isWhiteList) {
                mySQLConnector.Insert_Query("INSERT INTO blitz_bot.whiteListWord (FilterWord, Word)VALUES (?, ?);",
                        new int[]{mySQLConnector.STRING, mySQLConnector.STRING},
                        new String[]{word[0], word[1]});
                whiteFilterList.add(word);
            } else {
                mySQLConnector.Insert_Query("INSERT INTO blitz_bot.filterWord (Word) VALUES (?);",
                        new int[]{mySQLConnector.STRING},
                        new String[]{word[0]});
                filterList.add(word[0]);
            }
        } else {
            //DELETE WORD
            if(isWhiteList) {
                mySQLConnector.Insert_Query("DELETE FROM blitz_bot.whiteListWord WHERE FilterWord = ? AND Word = ?;",
                        new int[]{mySQLConnector.STRING, mySQLConnector.STRING},
                        new String[]{word[0], word[1]});
                whiteFilterList.remove(word);
            } else {
                mySQLConnector.Insert_Query("DELETE FROM blitz_bot.filterWord WHERE Word = ?;",
                        new int[]{mySQLConnector.STRING},
                        new String[]{word[0]});
                filterList.remove(word[0]);
            }
        }
    }

    public boolean commandAuthorityCheck(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        assert member != null;
        Guild guild = event.getGuild();
        if(guild == null) {
            return false;
        }
        boolean passRole = member.getRoles().contains(event.getGuild().getRoleById("827010848442548254")) ||    //R:모더레이터
                member.getRoles().contains(event.getGuild().getRoleById("827009999145926657")) ||               //R:Administrator
                member.getRoles().contains(event.getGuild().getRoleById("827011445187280906"));                 //R:컨트리뷰터
        if (!passRole) {
            event.getChannel().sendMessage("명령어를 사용할 권한이 없습니다.").queue();
            return false;
        }

        return true;
    }

    public @NotNull List<String> getFilterList() {
        return filterList;
    }

    public @NotNull List<String[]> getWhiteFilterList() {
        return whiteFilterList;
    }

}
