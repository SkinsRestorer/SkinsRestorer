package skinsrestorer.shared.utils;

import skinsrestorer.shared.storage.Config;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;
import java.sql.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MySQL {

    /**
     * Class by Blackfire62
     **/

    private Connection con;
    private String host, port, database, username, password;
    private ExecutorService exe;

    public MySQL(String host, String port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        exe = Executors.newCachedThreadPool();
        openConnection();
    }

    public void closeConnection() {
        if (isConnected())
            try {
                con.close();
            } catch (SQLException e) {
                System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
            }
    }

    public void createTable() {
        execute("CREATE TABLE IF NOT EXISTS `" + Config.MYSQL_PLAYERTABLE + "` ("
                + "`Nick` varchar(16) COLLATE utf8_unicode_ci NOT NULL,"
                + "`Skin` varchar(16) COLLATE utf8_unicode_ci NOT NULL,"
                + "PRIMARY KEY (`Nick`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci");
        execute("CREATE TABLE IF NOT EXISTS `" + Config.MYSQL_SKINTABLE + "` ("
                + "`Nick` varchar(16) COLLATE utf8_unicode_ci NOT NULL,"
                + "`Value` text COLLATE utf8_unicode_ci,"
                + "`Signature` text COLLATE utf8_unicode_ci,"
                + "`timestamp` text COLLATE utf8_unicode_ci,"
                + "PRIMARY KEY (`Nick`)) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci");
        try {
            addColumn();
        } catch (SQLException e) {
            if (e.getMessage().contains("doesn't exist")) {
                execute("ALTER TABLE `" + Config.MYSQL_SKINTABLE + "` ADD `timestamp` text COLLATE utf8_unicode_ci;");
            }
        }
    }

    public void addColumn() throws SQLException {
        execute("ALTER TABLE `" + Config.MYSQL_SKINTABLE + "` ADD `timestamp` text COLLATE utf8_unicode_ci;");
    }

    public void execute(final String query, final Object... vars) {
        if (isConnected())
            exe.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        PreparedStatement ps = prepareStatement(query, vars);
                        ps.execute();
                        ps.close();
                    } catch (SQLException e) {
                        if (e.getMessage().contains("Duplicate column name")) {
                            return;
                        }
                        e.printStackTrace();
                        System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
                    }
                }

            });
        else {
            openConnection();
            execute(query, vars);
        }
    }

    public boolean isConnected() {
        try {
            return con != null && !con.isClosed();
        } catch (SQLException e) {
            System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
        }
        return false;
    }

    public void openConnection() {
        if (!isConnected())
            exe.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?verifyServerCertificate=false&useSSL=false",
                                username, password);
                        System.out.println("[SkinsRestorer] Connected to MySQL!");
                        createTable();
                    } catch (SQLException e) {
                        System.out.println("[SkinsRestorer] Could NOT connect to MySQL: " + e.getMessage());
                    }
                }

            });
    }

    private PreparedStatement prepareStatement(String query, Object... vars) {
        try {
            if (isConnected()) {
                PreparedStatement ps = con.prepareStatement(query);
                int i = 0;
                if (query.contains("?") && vars.length != 0)
                    for (Object obj : vars) {
                        i++;
                        ps.setObject(i, obj);
                    }
                return ps;
            } else {
                openConnection();
                return prepareStatement(query, vars);
            }
        } catch (SQLException e) {
            System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
        }

        return null;
    }

    public CachedRowSet query(final String query, final Object... vars) {
        CachedRowSet rowSet = null;
        if (isConnected())
            try {

                Future<CachedRowSet> future = exe.submit(new Callable<CachedRowSet>() {
                    @Override
                    public CachedRowSet call() {
                        try {
                            PreparedStatement ps = prepareStatement(query, vars);

                            ResultSet rs = ps.executeQuery();
                            CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
                            crs.populate(rs);
                            rs.close();
                            ps.close();

                            if (crs.next())
                                return crs;

                        } catch (SQLException e) {
                            System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
                        }

                        return null;
                    }
                });

                if (future.get() != null)
                    rowSet = future.get();

            } catch (Exception e) {
                System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
            }
        else {
            openConnection();
            query(query, vars);
        }

        return rowSet;
    }
}