package skinsrestorer.sponge.utils;

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

public class MySQL {

	/** Class by Blackfire62 **/

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
	}

	public void execute(final PreparedStatement ps) {
		if (isConnected())
			exe.execute(new Runnable() {

				@Override
				public void run() {
					try {
						ps.execute();
						ps.close();
					} catch (SQLException e) {
						System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
					}
				}

			});
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
						con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database,
								username, password);
						System.out.println("[SkinsRestorer] Connected to MySQL!");
						createTable();
					} catch (SQLException e) {
						System.out.println("[SkinsRestorer] Could NOT connect to MySQL: " + e.getMessage());
					}
				}

			});
	}

	public PreparedStatement prepareStatement(String query, Object... vars) {
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
			}
		} catch (SQLException e) {
			System.out.println("[SkinsRestorer] MySQL error: " + e.getMessage());
		}

		return null;
	}

	public CachedRowSet query(final PreparedStatement preparedStatement) {
		CachedRowSet rowSet = null;
		if (isConnected())
			try {

				Future<CachedRowSet> future = exe.submit(new Callable<CachedRowSet>() {
					@Override
					public CachedRowSet call() {
						try {
							ResultSet rs = preparedStatement.executeQuery();
							CachedRowSet crs = RowSetProvider.newFactory().createCachedRowSet();
							crs.populate(rs);
							rs.close();

							preparedStatement.close();

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
		return rowSet;
	}
}
