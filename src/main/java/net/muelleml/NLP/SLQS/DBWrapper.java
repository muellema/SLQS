package net.muelleml.NLP.SLQS;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TempStore;

public class DBWrapper {

	private static final Logger logger = LogManager.getLogger(DBWrapper.class);

	private static DBWrapper instance;

	public static synchronized DBWrapper getInstance() throws Exception {
		if (instance == null) {
			throw new Exception("DB not initialized");
		}
		return instance;
	}

	public DBWrapper(String path, String name, boolean isMsSql) {
		super();
		this._path = path;
		this._name = name;
		this._useMsSql = isMsSql;
		open();
		instance = this;
	}

	private boolean open() {
		boolean r = false;

		_conn = getConnectionString(_path, _name, _useMsSql);

		logger.info("Opening DB with: " + _conn);

		try {
			if (_useMsSql) {
				Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

				Properties props = new Properties();

				_c = DriverManager.getConnection(_conn, props);

			} else {

				Class.forName("org.sqlite.JDBC");

				SQLiteConfig config = new SQLiteConfig();
				config.enableFullSync(false);
				config.setJournalMode(JournalMode.OFF);
				config.setSynchronous(SynchronousMode.OFF);
				config.setTempStore(TempStore.FILE);
				config.enableLoadExtension(true);
				_c = DriverManager.getConnection(_conn, config.toProperties());
			}

			if (!_useMsSql) {

				String path = DBWrapper.class.getProtectionDomain()
						.getCodeSource().getLocation().getPath();

				String sql = "";

				String decodedPath = URLDecoder.decode(path, "UTF-8");

				String libPath = decodedPath + "libsqlitefunctions.so";

				sql = "SELECT load_extension('" + libPath + "')";
				logger.info("Loaded Extensions");
			}

			_c.setAutoCommit(false);

			String sql = helper.Util.GetScript("PRAGMAS_SQLITE");

			Statement stmt = _c.createStatement();

			for (String s : sql.split(";")) {
				stmt.execute(s + ";");
				_c.commit();
			}
			_c.commit();
			logger.info("DB opened!");

			r = true;
		} catch (SQLException e) {
			e.printStackTrace();
			r = false;
			logger.fatal("Couldnt open DB");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			r = false;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			r = false;
			logger.error("Couldnt load SQLite extensions");
		}

		return r;
	}

	private String getConnectionString(String path, String name,
			boolean useMsSql) {
		String r = "";

		if (useMsSql) {
			r = String
					.format("jdbc:sqlserver:%1s;databaseName=%2s;integratedSecurity=true;",
							path, name);
		} else {
			r = String.format("jdbc:sqlite:%1s%2s", path, name);
		}

		return r;
	}

	public boolean close() {
		boolean r = false;

		try {
			_c.commit();
			_c.close();
			r = true;
		} catch (SQLException e) {
			e.printStackTrace();
			r = false;
		}
		return r;
	}

	private boolean _useMsSql = false;
	private boolean _isOpen = false;
	private String _conn = "";
	private Connection _c;
	private String _path = "";
	private String _name = "";

	public boolean isMsSql() {
		return _useMsSql;
	}

	public boolean isOpen() {
		return _isOpen;
	}

	public String getConnectionString() {
		return _conn;
	}

	public Connection getConnection() {
		return _c;
	}

	public String getDbPath() {
		return _path;
	}

	public String getDbName() {
		return _name;
	}
}
