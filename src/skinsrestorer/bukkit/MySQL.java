package skinsrestorer.bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetProvider;

import org.bukkit.Bukkit;

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
			Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), () -> {
				try {
					con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username,
							password);
					Bukkit.getLogger().info("Connected to MySQL!");
				} catch (SQLException e) {
					Bukkit.getLogger().severe("Could NOT connect to MySQL: " + e.getMessage());
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
			Bukkit.getScheduler().runTaskAsynchronously(SkinsRestorer.getInstance(), new Runnable() {

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

	public CachedRowSet query(final PreparedStatement preparedStatement) {
		CachedRowSet rowSet = null;
		if (isConnected()) {
			try {
				ExecutorService exe = Executors.newCachedThreadPool();

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
}
