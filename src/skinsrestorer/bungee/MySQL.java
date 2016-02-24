package skinsrestorer.bungee;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

import net.md_5.bungee.api.ProxyServer;
import skinsrestorer.shared.storage.ConfigStorage;

public class MySQL {

	private Connection con;
	private String host, port, database, username, password;

	public MySQL(String host, String port, String database, String username, String password) {
		this.host = host;
		this.port = port;
		this.database = database;
		this.username = username;
		this.password = password;
		openConnection();
	}

	public void openConnection() {
		if (!isConnected()) {
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				@Override
				public void run() {
					try {
						con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database,
								username, password);
						ProxyServer.getInstance().getLogger().info("[SkinsRestorer] Connected to MySQL!");
					createTable();
					} catch (SQLException e) {
						ProxyServer.getInstance().getLogger().severe("[SkinsRestorer] Could NOT connect to MySQL: " + e.getMessage());
					}
				}

			});
		}
	}

	public void closeConnection() {
		if (isConnected()) {
			try {
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean isConnected() {
		try {
			return con != null && !con.isClosed();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void execute(final PreparedStatement ps) {
		if (isConnected()) {
			ProxyServer.getInstance().getScheduler().runAsync(SkinsRestorer.getInstance(), new Runnable() {
				@Override
				public void run() {
					try {
						ps.execute();
						ps.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}

			});
		}
	}

	public PreparedStatement prepareStatement(String query, String... vars) {
		try {
			if (isConnected()) {
				PreparedStatement ps = con.prepareStatement(query);
				int x = 0;
				if (query.contains("?") && vars.length != 0) {
					for (String var : vars) {
						x++;
						ps.setString(x, var);
					}
				}
				return ps;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings("deprecation")
	public CachedRowSet query(final PreparedStatement preparedStatement) {
		CachedRowSet rowSet = null;
		if (isConnected()) {
			try {
				ExecutorService exe = SkinsRestorer.getInstance().getExecutorService();

				Future<CachedRowSet> future = exe.submit(new Callable<CachedRowSet>() {
					@Override
					public CachedRowSet call() {
						try {
							ResultSet rs = preparedStatement.executeQuery();
							CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
							crs.populate(rs);
							rs.close();

							preparedStatement.close();

							if (crs.next()) {
								return crs;
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}

						return null;
					}
				});

				if (future.get() != null) {
					rowSet = future.get();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return rowSet;
	}
	public void createTable() {
		execute(prepareStatement(
				"create table if not exists "+ConfigStorage.getInstance().MYSQL_TABLE+" (Nick varchar(255), Value text, Signature text)", ""));

	}
}
