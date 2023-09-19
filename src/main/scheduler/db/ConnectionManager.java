package scheduler.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private final String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private final String connectionUrl = "jdbc:sqlserver://" + System.getenv("Server") +
            ".database.windows.net:1433;database=" + System.getenv("DBName");
    private final String userName = System.getenv("UserID");
    private final String userPass = System.getenv("Password");

    private Connection con = null;

    public ConnectionManager() {
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            System.out.println(e.toString());
        }
    }

    public Connection createConnection() {
        try {
            con = DriverManager.getConnection(connectionUrl, userName, userPass);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return con;
    }

    public void closeConnection() {
        try {
            this.con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
