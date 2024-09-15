package me.kirito5572.objects.main;

import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;

import static me.kirito5572.App.openFileData;

public class MySqlConnector {
    private static Connection connection;
    private static String url;
    private static String user;
    private static String password;

    public final int STRING = 0;
    public final int INT = 1;
    public final int BOOLEAN = 2;
    public final int LONG = 3;


    public MySqlConnector() throws ClassNotFoundException, SQLException {
        url = "jdbc:mysql://" + openFileData("endPoint") + "/blitz_bot?serverTimezone=UTC";
        user = "blitzbot";
        password = openFileData("SQLPassword");
        String driverName = "com.mysql.cj.jdbc.Driver";
        Class.forName(driverName);
        connection = DriverManager.getConnection(url, user, password);

        //start watchdogs(Connection link check)
        Timer timer = new Timer();
        TimerTask MySqlWatchdog = new TimerTask() {
            @Override
            public void run() {
                try {
                    if(isConnectionClosed()) {
                        reConnection();
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        timer.scheduleAtFixedRate(MySqlWatchdog, 0, 24 * 60 * 60 * 1000); //1 day period
    }

    /**
     * check SQL Connection is closed
     * @return {@code true} if this {@code Connection} object
     *      is closed; {@code false} if it is still open
     * @throws SQLException if a database access error occurs
     */

    /**
     * check SQL Connection is closed
     * @return {@code true} if this {@code Connection} object
     *      is closed; {@code false} if it is still open
     * @throws SQLException if a database access error occurs
     */

    public boolean isConnectionClosed() throws SQLException {
        return connection.isClosed();
    }

    /**
     * reconnecting with sql server
     * @throws SQLException - if a database access error occurs
     */
    public void reConnection() throws SQLException {
        if (!connection.isClosed()) {
            connection.close();
        }
        connection = DriverManager.getConnection(url, user, password);
    }

    /**
     * select query to sql server
     * @param query sql query
     * @param dataType the data types that input
     * @param data the data that input
     * @throws SQLException if query execution fail or database access error occurs
     * @return {@link java.sql.ResultSet}
     */
    public ResultSet Select_Query(@Language("MySQL") String query, int[] dataType, String[] data) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        Query(statement, dataType, data);
        return statement.executeQuery();
    }

    /**
     * input data to query
     * @param statement sql statement({@link java.sql.PreparedStatement})
     * @param dataType the data types that input
     * @param data the data that input
     * @throws SQLException - if query execution fail or database access error occurs
     */
    public void Query(@NotNull PreparedStatement statement, int[] dataType, String[] data) throws SQLException {
        if(statement.getConnection().isClosed()) {
            reConnection();
        }
        for(int i = 0; i < dataType.length; i++) {
            if(dataType[i] == STRING) {
                statement.setString(i + 1, data[i]);
            } else if(dataType[i] == INT) {
                statement.setInt(i + 1, Integer.parseInt(data[i]));
            } else if(dataType[i] == BOOLEAN) {
                statement.setBoolean(i + 1, Boolean.parseBoolean(data[i]));
            } else if(dataType[i] == LONG) {
                statement.setLong(i + 1, Long.parseLong(data[i]));
            }
        }
    }

    /**
     * insert query to sql server
     * @param Query sql query
     * @param dataType the data types that input
     * @param data the data that input
     * @return 1 = success, 0 = failed
     * @throws SQLException if query execution fail or database access error occurs
     */
    public int Insert_Query(@Language("MySQL") String Query, int[] dataType, String[] data) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(Query);
        Query(statement, dataType, data);
        boolean isEnd = statement.execute();
        statement.close();
        return isEnd ? 1 : 0;
    }
}
