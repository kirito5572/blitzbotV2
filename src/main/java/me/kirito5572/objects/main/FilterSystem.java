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
    private final MySqlConnector mySqlConnector;

    public FilterSystem(MySqlConnector mySqlConnector) {
        this.mySqlConnector = mySqlConnector;
    }


    /**
     * Refresh filter words from database
     * @return if True, refresh success, if False, refresh fail
     */
    public boolean filterRefresh() {
        boolean isRefreshEnd = false;
        MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
        queryData.query = "SELECT * FROM blitz_bot.filterWord;";
        queryData.dataType = new int[] {};
        queryData.data = new String[] {};
        try (ResultSet resultSet = mySqlConnector.Select_Query(queryData)) {
            filterList.clear();
            isRefreshEnd = true;
            if(resultSet == null) {
                return false;
            }
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
        MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
        queryData.query = "SELECT * FROM blitz_bot.whiteListWord;";
        queryData.dataType = new int[] {};
        queryData.data = new String[] {};
        try (ResultSet resultSet = mySqlConnector.Select_Query(queryData)) {
            whiteFilterList.clear();
            isRefreshEnd = true;
            if(resultSet == null) {
                return false;
            }
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

    public void wordUpdate(boolean isWhiteList, boolean isInsert, String[] word) {
        if(isInsert) {
            //INSERT WORD
            if(isWhiteList) {
                MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
                queryData.query = "INSERT INTO blitz_bot.whiteListWord (FilterWord, Word)VALUES (?, ?);";
                queryData.dataType = new int[] {mySqlConnector.STRING, mySqlConnector.STRING};
                queryData.data = new String[] {word[0], word[1]};
                mySqlConnector.Insert_Query(queryData);
                whiteFilterList.add(word);
            } else {
                MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
                queryData.query = "INSERT INTO blitz_bot.filterWord (Word) VALUES (?);";
                queryData.dataType = new int[] {mySqlConnector.STRING};
                queryData.data = new String[] {word[0]};
                mySqlConnector.Insert_Query(queryData);
                filterList.add(word[0]);
            }
        } else {
            //DELETE WORD
            if(isWhiteList) {
                MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
                queryData.query = "DELETE FROM blitz_bot.whiteListWord WHERE FilterWord = ? AND Word = ?;";
                queryData.dataType = new int[] {mySqlConnector.STRING, mySqlConnector.STRING};
                queryData.data = new String[] {word[0], word[1]};
                mySqlConnector.Insert_Query(queryData);
                whiteFilterList.remove(word);
            } else {
                MySqlConnector.QueryData queryData = new MySqlConnector.QueryData();
                queryData.query = "DELETE FROM blitz_bot.filterWord WHERE Word = ?;";
                queryData.dataType = new int[] {mySqlConnector.STRING};
                queryData.data = new String[] {word[0]};
                mySqlConnector.Insert_Query(queryData);
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
