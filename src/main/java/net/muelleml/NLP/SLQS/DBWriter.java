package net.muelleml.NLP.SLQS;

import helper.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multiset.Entry;

public class DBWriter implements Runnable {

	private static final Logger logger = LogManager.getLogger(DBWriter.class);

	private boolean _run = true;
	private boolean _busy = false;
	private String _connString = "";
	private boolean _useMsSql = false;
	public ConcurrentLinkedDeque<Set<Entry<String>>> writeDequeue;
	private Connection _c;

	private boolean _isrunning = true;

	private void setupDb() {

		logger.info("Opening DB with:" + _connString);

		try {

			_c = DBWrapper.getInstance().getConnection();
			_useMsSql = DBWrapper.getInstance().isMsSql();

			String sql = "";

			Util.DropTable("WINDOWS", _useMsSql);
			Util.DropTable("WINDOWCOUNT", _useMsSql);
			Util.DropTable("GLOBALTEMP", _useMsSql);
			Util.DropTable("GCOUNT", _useMsSql);
			Util.DropTable("ENTVIEW", _useMsSql, true);
			Util.DropTable("EXPERIMENTS", _useMsSql);
			Util.DropTable("TAGLISTS", _useMsSql);

			// create tables
			Statement batch = _c.createStatement();

			if (_useMsSql) {
				sql = "CREATE TABLE WINDOWS (ID BIGINT IDENTITY(1,1) PRIMARY KEY,"
						// + " TOKEN1ID INTEGER,"
						+ " TOKEN1TAG VARCHAR(10),"
						+ " TOKEN1LEMMA VARCHAR(200),"
						// + " TOKEN2ID INTEGER,"
						+ " TOKEN2TAG VARCHAR(10),"
						+ " TOKEN2LEMMA VARCHAR(200),"
						// + " DISTANCE INT,"
						// + " SID INT,"
						+ " TUPLECOUNT INT)";
			} else {
				sql = "CREATE TABLE WINDOWS (ID INTEGER PRIMARY KEY  NOT NULL,"
						// + " TOKEN1ID INTEGER,"
						+ " TOKEN1TAG VARCHAR(10),"
						+ " TOKEN1LEMMA VARCHAR(50),"
						// + " TOKEN2ID INTEGER,"
						+ " TOKEN2TAG VARCHAR(10),"
						+ " TOKEN2LEMMA VARCHAR(50),"
						// + " DISTANCE INT,"
						// + " SID INT,"
						+ " TUPLECOUNT INTEGER)";
			}

			batch.addBatch(sql);

			if (_useMsSql) {
				sql = "CREATE TABLE WINDOWCOUNT ("
						+ " ID BIGINT IDENTITY(1,1) PRIMARY KEY,"
						+ " TOKEN1TAG VARCHAR(10),"
						+ " TOKEN1LEMMA VARCHAR(200),"
						+ " TOKEN2TAG VARCHAR(10),"
						+ " TOKEN2LEMMA VARCHAR(200),"
						+ " DISTANCE INT, "
						+ " TUPLECOUNT INT, LMI REAL, ENTROPY REAL, ENTPART REAL,"
						+ " NORMALIZED REAL)";
			} else {
				sql = "CREATE TABLE WINDOWCOUNT ("
						+ " ID INTEGER PRIMARY KEY  NOT NULL,"
						+ " TOKEN1TAG VARCHAR(10),"
						+ " TOKEN1LEMMA VARCHAR(50),"
						+ " TOKEN2TAG VARCHAR(10),"
						+ " TOKEN2LEMMA VARCHAR(50)," + " TUPLECOUNT INT"
						+ " )";
			}

			batch.addBatch(sql);

			batch.executeBatch();
			batch.clearBatch();

			_c.commit();

		} catch (Exception e) {
			logger.error("Could not Import Corpus!");
			e.printStackTrace();
		}
	}

	public void run() {

		try {
			setupDb();

			Statement stmt = _c.createStatement();

			String windowing = "INSERT INTO WINDOWCOUNT "
					+ " (TOKEN1TAG , TOKEN1LEMMA , TOKEN2TAG , TOKEN2LEMMA , TUPLECOUNT) VALUES (?,?,?,?,?);";

			String sql;

			String t1Lemma = "";
			String t2Lemma = "";
			String t1Tag = "";
			String t2Tag = "";
			int count = 0;
			String[] rArr;

			HashSet<String> hs = new HashSet<String>();

			while (_run || !writeDequeue.isEmpty()) {

				if (writeDequeue.isEmpty()) {
					Thread.sleep(1000);
				} else {
					_busy = true;
					PreparedStatement batch = _c.prepareStatement(windowing);

					int i = 1;
					for (Entry<String> ent : writeDequeue.pop()) {

						if (!hs.add(ent.getElement())) {
							logger.info(ent.getElement());
						}

						rArr = ent.getElement().split("\t");
						t1Lemma = rArr[0];
						t1Tag = rArr[1];
						t2Lemma = rArr[2];
						t2Tag = rArr[3];

						if (t1Lemma.startsWith("<unkown")
								|| t1Tag.startsWith("<unkown")
								|| t2Lemma.startsWith("<unkown")
								|| t1Tag.startsWith("<unkown")) {
						} else {

							batch.setString(1, t1Tag);
							batch.setString(2, t1Lemma);
							batch.setString(3, t2Tag);
							batch.setString(4, t2Lemma);
							batch.setInt(5, ent.getCount());

							batch.addBatch();
							i++;
						}

						if (i % 1000000 == 0) {
							batch.executeBatch();
							batch.clearBatch();
						}
					}
					if (i % 1000000 > 0) {
						batch.executeBatch();
						batch.clearBatch();
					}

					_busy = false;
					logger.info("Finished writing to DB");
				}
			}

			_c.commit();

			writeDequeue.clear();

			System.gc();

			logger.info("Creating Indexes");

			// sql = "CREATE INDEX W6 ON WINDOWCOUNT (TOKEN1LEMMA);";
			// stmt.addBatch(sql);
			// sql = "CREATE INDEX W7 ON WINDOWCOUNT (TOKEN2LEMMA);";
			// stmt.addBatch(sql);
			//
			// sql = "CREATE INDEX W8 ON WINDOWCOUNT (TOKEN1TAG);";
			// stmt.addBatch(sql);
			// sql = "CREATE INDEX W9 ON WINDOWCOUNT (TOKEN2TAG);";
			// stmt.addBatch(sql);

			sql = "CREATE INDEX W11 ON WINDOWCOUNT (TOKEN1LEMMA, TOKEN1TAG);";
			stmt.addBatch(sql);
			sql = "CREATE INDEX W12 ON WINDOWCOUNT (TOKEN2LEMMA, TOKEN2TAG);";
			stmt.addBatch(sql);
			stmt.executeBatch();

			_c.commit();

			// Util.AnalyzeTable("WINDOWCOUNT");

			logger.info("Created Indexes");

			logger.info("Setting up Experiments!");

			sql = helper.Util.GetScript("CREATE_TABLE_EXPERIMENTS_SQLITE");

			for (String tempSql : sql.split(";")) {
				stmt.addBatch(tempSql + ";");
			}
			stmt.executeBatch();

			_c.commit();

			// Util.AnalyzeTable("EXPERIMENTS");

			logger.info("Experiments set up");

			logger.info("Creating additional Temp Tables");
			sql = helper.Util.GetScript("CREATE_TABLE_GCOUNT_SQLITE");
			stmt.addBatch(sql);

			sql = helper.Util.GetScript("CREATE_TEMP_GLOBALTEMP_SQLITE");
			stmt.addBatch(sql);

			sql = "CREATE INDEX G1 ON GCOUNT(EXPID);";
			stmt.addBatch(sql);
			sql = "CREATE INDEX G2 ON GCOUNT(TAG);";
			stmt.addBatch(sql);
			sql = "CREATE INDEX G3 ON GCOUNT(LEMMA);";
			stmt.addBatch(sql);
			sql = "CREATE INDEX G4 ON GCOUNT(LEMMA,TAG);";
			stmt.addBatch(sql);
			sql = "CREATE INDEX G5 ON GCOUNT(LEMMA, TAG,EXPID);";
			stmt.addBatch(sql);

			sql = "CREATE INDEX GT1 ON GLOBALTEMP(EXPID);";
			stmt.addBatch(sql);

			stmt.executeBatch();
			_c.commit();
			logger.info("Created additional Temp Tables");
			logger.info("Counted windows");
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			_isrunning = false;
			_busy = false;
		}
	}

	public void shutdown() {
		_run = false;
	}

	public DBWriter() {
		writeDequeue = new ConcurrentLinkedDeque<Set<Entry<String>>>();
	}

	public boolean isRunning() {
		return _isrunning;
	}

	public boolean isBusy() {
		return _busy;
	}
}
