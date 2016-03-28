package helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteConfig.JournalMode;
import org.sqlite.SQLiteConfig.SynchronousMode;
import org.sqlite.SQLiteConfig.TempStore;

import com.google.common.collect.Lists;
import com.opencsv.CSVWriter;
import com.sun.corba.se.spi.orbutil.fsm.Guard.Result;

import net.muelleml.NLP.SLQS.DBWrapper;

public class Util {

	private static final Logger logger = LogManager.getLogger(Util.class);

	private static boolean _useMsSql = false;

	private static PrintWriter _writer;

	public static void SampleExtractor(String source, String target, int lines) {
		Charset charset = Charset.forName("ISO-8859-1");
		try (BufferedReader reader = Files.newBufferedReader(Paths.get(source), charset)) {

			BufferedWriter writer = Files.newBufferedWriter(Paths.get(target), charset);

			String line = null;

			int i_Counter = 0;
			while ((line = reader.readLine()) != null && lines > i_Counter) {
				// System.out.println(line);
				writer.write(line + System.lineSeparator());

				if (line.startsWith("</sentence>") || line.startsWith("</text>")) {
					i_Counter++;
				}
			}

			reader.close();
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}
	}

	public static void ReadRelations(String relations, String rType, String enc) {

		logger.info("Opening relations: " + relations + " as " + rType + " with encoding: " + enc);

		boolean bless = rType.equalsIgnoreCase("bless");

		try {
			Charset charset = Charset.forName(enc);

			Connection c = DBWrapper.getInstance().getConnection();
			boolean useMsSql = DBWrapper.getInstance().isMsSql();

			String sql = "";

			logger.info("Preparing Tables");

			DropTable("RELATIONS", useMsSql, false);
			DropTable("RELTARGETS", useMsSql, false);

			c.commit();

			logger.info("Creating Tables");

			Statement batch = c.createStatement();

			if (useMsSql) {
				sql = "CREATE TABLE RELATIONS (" + " ID BIGINT IDENTITY(1,1) PRIMARY KEY," + " TOKEN1 VARCHAR(255),"
						+ " TOKEN2 VARCHAR(255)," + " SCORE INTEGER," + " TAG VARCHAR(10)," + " CATEGORY VARCHAR(10))";
			} else {
				sql = "CREATE TABLE RELATIONS (" + " ID INTEGER PRIMARY KEY NOT NULL," + " TOKEN1 VARCHAR(50),"
						+ " TOKEN2 VARCHAR(50)," + " SCORE INTEGER," + " TAG VARCHAR(10)," + " CATEGORY VARCHAR(10))";
			}

			batch.addBatch(sql);

			batch.executeBatch();
			batch.clearBatch();

			logger.info("Created Tables");

			logger.info("Importing Relations");

			String prepRelStmt = "INSERT INTO RELATIONS (TOKEN1, TOKEN2, SCORE, TAG, CATEGORY) VALUES(?,?,?,?,?)";
			PreparedStatement prepRel = c.prepareStatement(prepRelStmt);

			BufferedReader relReader = Files.newBufferedReader(Paths.get(relations), charset);

			String rel = "HYP";
			String t1 = "";
			String t2 = "";
			String tag = "NN";

			String[] record;
			String line;
			while ((line = relReader.readLine()) != null) {
				if (line != null) {

					record = line.split("\\s");

					if (bless) {
						if (record.length == 4) {

							if (record[2].equalsIgnoreCase("hyper")) {

								t1 = record[0].substring(0, record[0].length() - 2);
								t2 = record[3].substring(0, record[3].length() - 2);

								prepRel.setString(1, t1);
								prepRel.setString(2, t2);
								prepRel.setInt(3, 10);
								prepRel.setString(4, tag);
								prepRel.setString(5, rel);

								prepRel.addBatch();
							}

						} else {
							logger.warn("Found relation line with length != 4" + line);
						}

					} else {
						if (record.length == 5) {

							prepRel.setString(1, new String(record[0]));
							prepRel.setString(2, new String(record[1]));
							prepRel.setInt(3, Integer.parseInt(record[2]));
							prepRel.setString(4, new String(record[3]));
							prepRel.setString(5, new String(record[4]));

							prepRel.addBatch();
						} else {
							logger.warn("Found relation line with length != 5" + line);
						}
					}
				}
			}

			prepRel.executeBatch();

			c.commit();

			sql = "CREATE INDEX R1 ON RELATIONS (TOKEN1);";
			batch.addBatch(sql);
			sql = "CREATE INDEX R2 ON RELATIONS (TOKEN2);";
			batch.addBatch(sql);
			sql = "CREATE INDEX R3 ON RELATIONS (CATEGORY);";
			batch.addBatch(sql);
			sql = "CREATE INDEX R4 ON RELATIONS (TAG);";
			batch.addBatch(sql);
			sql = "CREATE INDEX R5 ON RELATIONS (SCORE);";
			batch.addBatch(sql);

			sql = GetScript("CREATE_TABLE_AS_RELTARGETS_SQLITE");
			batch.addBatch(sql);
			batch.executeBatch();

			c.commit();

			sql = "CREATE INDEX RT1 ON RELTARGETS (LEMMA);";
			batch.addBatch(sql);
			sql = "CREATE INDEX RT2 ON RELTARGETS (TAG);";
			batch.addBatch(sql);
			sql = "CREATE INDEX RT3 ON RELTARGETS (LEMMA,TAG);";
			batch.addBatch(sql);
			batch.executeBatch();
			c.commit();
			logger.info("Finished Importing Relations");
		} catch (Exception e) {
			logger.error("Couldnt import relations from: " + relations);
			e.printStackTrace();
		}

	}

	/**
	 * Calculates the Evalues
	 */
	public static void Evalues(List<Integer> lim) {

		try {

			logger.info("Computing Evalues");

			boolean useMsSql = DBWrapper.getInstance().isMsSql();

			Connection c = DBWrapper.getInstance().getConnection();

			PreparedStatement stmt = null;
			Statement batch = c.createStatement();
			String sql = "";

			logger.info("Preparing Tables");
			DropTable("EVALUES", useMsSql);
			DropTable("WIDTABLE", useMsSql);
			DropTable("LMI", useMsSql);

			c.commit();

			logger.info("Calculating  LMI");
			sql = GetScript("CREATE_TABLE_LMI_SQLITE");

			batch.addBatch(sql);

			batch.executeBatch();
			c.commit();

			logger.info("Calculated LMI");
			logger.info("Creating Indexes");

			sql = "CREATE INDEX L1 ON LMI(EXPID);";
			batch.addBatch(sql);
			sql = "CREATE INDEX L2 ON LMI(WID);";
			batch.addBatch(sql);
			sql = "CREATE INDEX L3 ON LMI(EXPID, WID);";
			batch.addBatch(sql);

			batch.executeBatch();
			c.commit();
			logger.info("Created Indexes");
			logger.info("Computed LMI");

			logger.info("Computing Entropies");
			logger.info("Preparing Tables");

			logger.info("Updating Entropies");
			sql = GetScript("UPDATE_ENTROPIES_SQLITE");

			batch.addBatch(sql);
			batch.executeBatch();

			c.commit();

			logger.info("Updated Entropies");

			logger.info("Computed Entropies");

			logger.info("Creating Temp Tables");

			sql = GetScript("CREATE_TABLES_EV_SQLITE");

			for (String s : sql.split(";")) {
				batch.addBatch(s + ";");
			}

			batch.executeBatch();

			logger.info("Calculating E Values");
			AnalyzeTable("");
			sql = GetScript("CREATE_TABLE_EVALUES_SQLITE");

			batch.addBatch(sql);
			batch.executeBatch();

			List<Integer> eList = new LinkedList<Integer>();

			if (lim.contains(-1)) {
				for (int i = 30; i <= 400; i += 5) {
					eList.add(i);
				}
				lim.remove(new Integer(-1));
			}
			for (int i : lim) {
				eList.add(i);
			}

			sql = GetScript("INSERT_EVALUES_FORMATTER_SQLITE");

			stmt = c.prepareStatement(sql);
			for (int i : eList) {
				stmt.setInt(1, i);
				stmt.setInt(2, i);
				stmt.addBatch();
			}

			stmt.executeBatch();
			c.commit();
			logger.info("Creating Indexes On E Values");
			sql = "CREATE INDEX EV1 ON EVALUES(LEMMA);";
			batch.addBatch(sql);
			sql = "CREATE INDEX EV2 ON EVALUES(TAG);";
			batch.addBatch(sql);
			sql = "CREATE INDEX EV3 ON EVALUES(LEMMA, TAG);";
			batch.addBatch(sql);
			batch.executeBatch();
			c.commit();
			logger.info("Calculated E Values");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Calculates the Cosine Similarity
	 */
	public static void Cosine() {

		try {

			logger.info("Cosine Similarity");

			boolean useMsSql = DBWrapper.getInstance().isMsSql();

			Connection c = DBWrapper.getInstance().getConnection();

			Statement stmt = c.createStatement();
			Statement batch = c.createStatement();
			String sql = "";

			logger.info("Preparing Tables");
			DropTable("COS_SIM", useMsSql);
			c.commit();

			sql = GetScript("CREATE_TEMP_EXPRELS_SQLITE");
			batch.addBatch(sql);
			batch.addBatch("CREATE INDEX EXPRELS3 ON EXPRELS(TOKEN1, TAG1);");
			batch.addBatch("CREATE INDEX EXPRELS2 ON EXPRELS(TOKEN2, TAG2);");
			batch.addBatch("CREATE INDEX EXPRELS1 ON EXPRELS(TOKEN1, TAG1, TOKEN2, TAG2);");

			batch.executeBatch();
			c.commit();

			Util.AnalyzeTable("");

			sql = GetScript("CREATE_TEMP_COSTEST_SQLITE");
			batch.addBatch(sql);

			// logger.info(GetQueryPlan(sql));

			batch.executeBatch();
			c.commit();
			sql = GetScript("CREATE_TEMP_CSUMS_SQLITE");
			batch.addBatch(sql);
			batch.addBatch("CREATE INDEX CSM1 ON CSUMS (EXPID, TOKEN, TAG);");
			batch.addBatch("CREATE INDEX CSM2 ON CSUMS (TOKEN, TAG);");
			batch.addBatch("CREATE INDEX CSM3 ON CSUMS (EXPID);");

			batch.executeBatch();
			c.commit();
			logger.info("Creating Table");

			sql = GetScript("CREATE_COS_SIM_SQLITE");
			batch.addBatch(sql);
			sql = "CREATE INDEX TC1 ON COS_SIM(EXPID)";
			batch.addBatch(sql);
			sql = "CREATE INDEX TC2 ON COS_SIM(TOKEN1,TAG1)";
			batch.addBatch(sql);
			sql = "CREATE INDEX TC3 ON COS_SIM(TOKEN2,TAG2)";
			batch.addBatch(sql);

			batch.executeBatch();
			c.commit();
			// Util.AnalyzeTable("COS_SIM");
			logger.info("Created Table");
			DropTable("CSUMS", _useMsSql);
			DropTable("EXPRELS", _useMsSql);
			DropTable("COSTEST", _useMsSql);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Calculates the AveragePrecicision for relation identification
	 */
	public static void AveragePrecicision() {

		try {

			logger.info("Average Precicision");

			boolean useMsSql = DBWrapper.getInstance().isMsSql();

			Connection c = DBWrapper.getInstance().getConnection();

			Statement stmt = c.createStatement();
			Statement batch = c.createStatement();
			Statement retr = c.createStatement();
			String sql = "";

			logger.info("Preparing Tables");
			DropTable("APRESULTS", useMsSql);
			DropTable("APORDER", useMsSql);
			c.commit();

			sql = GetScript("CREATE_TABLE_APRESULTS_SQLITE");
			batch.addBatch(sql);

			batch.addBatch("CREATE INDEX IF NOT EXISTS EV210 ON EVALUES(LIM, EXPID, LEMMA, TAG);");

			batch.executeBatch();

			List<Integer> limList = GetColumn("SELECT DISTINCT LIM FROM EVALUES;", Integer.class);
			List<Integer> expList = GetColumn("SELECT DISTINCT EXPID FROM EVALUES;", Integer.class);
			List<Integer> scrList = GetColumn("SELECT DISTINCT SCORE FROM RELATIONS WHERE CATEGORY = 'HYP';",
					Integer.class);

			List<String> tupleList;
			List<String> results;

			double ap;

			int totDistinctCnt = 0;

			// 1. lim
			// 2. expid
			// 3. score
			sql = GetScript("SELECT_APORDER_SQLITE");

			String insert = "INSERT INTO APRESULTS (LIM, EXPID, SCORE, AP, COMB, ORDERING)VALUES(?, ?, ?, ?, ?, ?);";

			PreparedStatement prepInsert = c.prepareStatement(insert);

			String[] combL = new String[] { " SLQS", " COSINE_SIM", " (COSINE_SIM * SLQS)", "(COSINE_SIM * (1-SLQS))" };
			String[] orderL = new String[] { " ASC", " DESC" };

			for (int lim : limList) {
				for (int expid : expList) {
					for (int score : scrList) {
						for (String order : orderL) {
							for (String comb : combL) {
								tupleList = GetColumn(
										"SELECT TOKEN1||TAG||TOKEN2 FROM RELATIONS WHERE CATEGORY = 'HYP' AND ( EXISTS (SELECT 1 FROM EVALUES WHERE TOKEN1 = LEMMA AND EVALUES.TAG = RELATIONS.TAG) AND EXISTS (SELECT 1 FROM EVALUES WHERE TOKEN2 = LEMMA AND EVALUES.TAG = RELATIONS.TAG) ) AND SCORE >= ?;"
												.replaceAll(Pattern.quote("?"), Integer.toString(score)),
										String.class);

								String aporder = sql;

								aporder = aporder.replaceFirst("\\?", Integer.toString(lim));
								aporder = aporder.replaceFirst("\\?", Integer.toString(expid));
								aporder = aporder.replaceFirst("\\?", Integer.toString(score));

								tupleList = GetColumn(aporder.replaceFirst("\\?", " AND RELATIONS.CATEGORY = 'HYP' ")
										.replaceFirst("\\?", comb).replaceFirst("\\?", order), String.class);

								aporder = aporder.replaceFirst("\\?", "");
								aporder = aporder.replaceFirst("\\?", comb);
								aporder = aporder.replaceFirst("\\?", order);

								results = GetColumn(aporder, String.class);

								totDistinctCnt = GetScalar(
										"SELECT count ( distinct( concat)) from ( " + aporder.replace(';', ' ') + " );",
										Integer.class);

								ap = GetAP(results, tupleList.size(), totDistinctCnt, new HashSet<String>(tupleList));

								// Write to DB
								prepInsert.setInt(1, lim);
								prepInsert.setInt(2, expid);
								prepInsert.setInt(3, score);
								prepInsert.setDouble(4, ap);
								prepInsert.setString(5, comb);
								prepInsert.setString(6, order);
								prepInsert.addBatch();

							}
						}
					}
					logger.info("AP EXPID: " + expid);
				}
				logger.info("AP LIM: " + lim);
			}

			prepInsert.executeBatch();

			logger.info("Finished AveragePrecicion");

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Calculates the Cosine Similarity
	 */
	public static void ExportTable(String tableName, String exportTarget) {

		try {

			logger.info("Exporting table: " + tableName);

			Connection c = DBWrapper.getInstance().getConnection();

			Statement stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + ";");

			WriteResultSet(rs, exportTarget);

			logger.info("Created Table");

		} catch (Exception e) {
			logger.error("Could not get data from table: " + tableName);
		}

	}

	public static boolean DropTable(String tableToDrop, boolean useMsSql) {
		return DropTable(tableToDrop, useMsSql, false);
	}

	public static boolean DropTable(String tableToDrop, boolean useMsSql, boolean isView) {
		boolean r = false;

		try {
			Connection c = DBWrapper.getInstance().getConnection();
			Statement stmt = c.createStatement();
			String sql = "";

			String obj = isView ? "VIEW" : "TABLE";

			// sqlserver branch
			if (useMsSql) {
				try {
					sql = String.format("IF EXISTS(select * from sysobjects where name='%s') drop " + obj + " %s;",
							tableToDrop, tableToDrop);

					stmt.execute(sql);
					c.commit();

					r = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// sqlite branch
				try {
					sql = String.format("DROP " + obj + " IF EXISTS %s;", tableToDrop);

					stmt.execute(sql);
					c.commit();

					r = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (SQLException e1) {
			logger.error("Couldnt drop table: " + tableToDrop);
			e1.printStackTrace();
		} catch (Exception e1) {
			logger.error("Couldnt access DB");
			e1.printStackTrace();
		}

		return r;
	}

	private static void WriteLine(String line) {

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd_HH:mm:ss").format(Calendar.getInstance().getTime());

		String t_line = String.format("%s: %s%s", timeStamp, line, System.lineSeparator());

		System.out.print(t_line);
		_writer.write(t_line);

		_writer.write(line + System.lineSeparator());
		_writer.flush();
	}

	private static void WriteResultSet(ResultSet rs, String expPath) {
		try {
			if (Files.exists(Paths.get(expPath))) {
				Files.delete(Paths.get(expPath));
			}

			BufferedWriter out = new BufferedWriter(new FileWriter(expPath));
			CSVWriter writer = new CSVWriter(out);

			ResultSetMetaData md = rs.getMetaData();

			List<String> row = new LinkedList<String>();

			int colCount = md.getColumnCount();

			for (int i = 1; i <= colCount; i++) {
				row.add(md.getColumnName(i));
			}

			String[] rowArr = row.toArray(new String[colCount]);

			writer.writeNext(rowArr);

			while (rs.next()) {
				row.clear();

				for (int i = 1; i <= colCount; i++) {
					String value = rs.getString(i);
					row.add(value);
				}
				rowArr = row.toArray(new String[colCount]);

				writer.writeNext(rowArr);
				writer.flush();
			}

			writer.close();
		} catch (Exception e) {
			logger.error("Could not write to: " + expPath);
		}
	}

	public static void PrintUsage() {
		int mb = 1024 * 1024;

		// Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();

		// logger.info("##### Heap utilization statistics [MB] #####");

		// Print used memory
		logger.info("Used Memory:" + (runtime.totalMemory() - runtime.freeMemory()) / mb);

		// Print free memory
		logger.info("Free Memory:" + runtime.freeMemory() / mb);

		// Print total available memory
		logger.info("Total Memory:" + runtime.totalMemory() / mb);

		// Print Maximum available memory
		logger.info("Max Memory:" + runtime.maxMemory() / mb);
		// procs
		logger.info("Available Processors:" + runtime.availableProcessors());
	}

	public static String GetScript(String scriptName) {
		String r = "";
		try {
			InputStream inputStream = Util.class.getClassLoader().getResourceAsStream("Scripts/" + scriptName + ".sql");

			StringWriter sw = new StringWriter();
			org.apache.commons.io.IOUtils.copy(inputStream, sw, Charset.forName("UTF-8"));
			r = sw.toString();
		} catch (Exception e) {
			logger.error("Couldnt load script: " + scriptName);
			e.printStackTrace();
		}

		return r;
	}

	public static void Cleanup() {

		try {
			Connection c = DBWrapper.getInstance().getConnection();
			Statement stmt = c.createStatement();
			DropTable("WINDOWS", DBWrapper.getInstance().isMsSql());
			c.commit();
			String sql = "vacuum;";
			c.commit();
			PreparedStatement prep = c.prepareStatement(sql);
			prep.execute();
			prep.close();
			c.commit();

		} catch (SQLException e1) {
			logger.error("SQL error");
			e1.printStackTrace();
		} catch (Exception e1) {
			logger.error("Couldnt access DB");
			e1.printStackTrace();
		}

	}

	public static void AnalyzeTable(String tableName) {

		try {
			Connection c = DBWrapper.getInstance().getConnection();
			Statement stmt = c.createStatement();
			logger.info("Analyzing table " + tableName);
			stmt.execute("ANALYZE " + tableName + ";");
			c.commit();
			logger.info("Finished analyzing table " + tableName);
		} catch (SQLException e1) {
			logger.error("SQL error");
			e1.printStackTrace();
		} catch (Exception e1) {
			logger.error("Couldnt access DB");
			e1.printStackTrace();
		}

	}

	public static String GetQueryPlan(String query) {

		StringBuilder sb = new StringBuilder();
		try {
			Connection c = DBWrapper.getInstance().getConnection();
			Statement stmt = c.createStatement();

			sb.append("explain query plan " + query + ";" + System.lineSeparator());

			ResultSet rs = stmt.executeQuery("explain query plan " + query + ";");
			int colC = rs.getMetaData().getColumnCount();
			List<String> iL = new LinkedList<String>();
			while (rs.next()) {
				for (int i = 1; i <= colC; i++) {
					iL.add(rs.getString(i));
				}
				sb.append(String.join("\t", iL) + System.lineSeparator());
				iL.clear();
			}

		} catch (SQLException e1) {
			logger.error("SQL error");
			e1.printStackTrace();
		} catch (Exception e1) {
			logger.error("Couldnt access DB");
			e1.printStackTrace();
		}
		return sb.toString();
	}

	private static <T> List<T> GetColumn(String query, Class<T> type) {
		List<T> r = new LinkedList<T>();
		try {
			Connection c = DBWrapper.getInstance().getConnection();
			Statement stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery(query);

			while (rs.next()) {
				r.add((T) rs.getObject(1));
			}
		} catch (Exception exp) {
		}

		return r;
	}

	private static <T> T GetScalar(String query, Class<T> type) {
		T r = null;
		try {
			Connection c = DBWrapper.getInstance().getConnection();
			Statement stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery(query);

			if (rs.next()) {
				r = (T) rs.getObject(1);
			}
		} catch (Exception exp) {
		}

		return r;
	}

	public static double GetAP(List<String> results, int totRelCount, int totDistCnt, Set<String> hyps) {

		// shortcut
		if ((totRelCount == 0) || (totDistCnt == 0)) {
			return 0.0f;
		}

		double r = 0.0f;
		int size = 10;

		// List<Double>>();
		double[] precs = new double[size + 1];
		Map<Integer, List<Double>> precsList = new HashMap<Integer, List<Double>>();

		for (int i = 0; i < precs.length; i++) {
			precs[i] = -1.0f;
			precsList.put(i, new LinkedList<Double>());
		}

		String prev = "";

		int cnt = 01;

		int hit = 0;

		double recall = 0.0f;
		double prec = 0.0f;

		int myCnt = 0;

		for (String s : results) {

			if ((!prev.equals(s))) {

				// hit
				if (hyps.contains(s)) {
					hit++;
					// calc recall and prec
					recall = hit / ((double) totRelCount);
					prec = hit / ((double) cnt);

					// update
					precs[(int) (Math.floor(recall * (double) size))] = prec;
					precsList.get((int) (Math.floor(recall * (double) size))).add(prec);
					// logger.info(s + " " + myCnt + prev);
				}
				// miss
				else {
				}

				cnt++;
			}

			myCnt++;

			prev = s;
		}

		// fill empty

		for (int i = precs.length - 1; i >= 0; i--) {

			List<Double> tPrecs = precsList.get(i);

			if (tPrecs.size() == 0) {
				precs[i] = -1.0f;
			} else {
				Double tPrec = 0.0d;
				for (Double d : tPrecs) {
					tPrec += d;
				}
				precs[i] = tPrec / tPrecs.size();
			}

			if (precs[i] < 0.0f) {
				precs[i] = precs[i + 1];
			}

		}

		// average
		for (Double d : precs) {
			r += d;
		}

		r = r / ((double) precs.length);

		return r;
	}

	public static void ClearEx() {
		try {
			logger.info("Clearing Experiments");
			Connection c = DBWrapper.getInstance().getConnection();

			DropTable("EXPERIMENTS", DBWrapper.getInstance().isMsSql());
			DropTable("TAGLISTS", DBWrapper.getInstance().isMsSql());

			String[] sql = GetScript("CREATE_TABLE_EXPERIMENTS_SQLITE").split(";");

			Statement stmt = c.createStatement();
			stmt.execute(sql[0]);
			stmt.execute(sql[1]);
			stmt.execute(sql[4]);
			stmt.execute(sql[5]);
			logger.info("Finished clearing Experiments");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void AddEx(String expName, String description, String[] tags) {
		Connection c;
		try {
			logger.info("Adding Experiment");
			c = DBWrapper.getInstance().getConnection();
			Statement stmt = c.createStatement();

			String insert = "INSERT INTO EXPERIMENTS(NAME, DESCRIPTION) VALUES ('" + expName + "', '" + description
					+ "');";
			// Long lastIns = GetScalar(insert, Long.class);
			stmt.execute(insert);
			Integer lastIns = GetScalar("select last_insert_rowid();", Integer.class);
			// lastIns = stmt.executeQuery(sql)("select last_insert_rowid();");

			for (String tag : tags) {
				String insTags = "INSERT INTO TAGLISTS(EXP_ID,TAG) VALUES(" + lastIns + ", '" + tag.trim() + "' );";
				stmt.execute(insTags);
			}
			logger.info("Finished adding Experiment");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}