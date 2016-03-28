import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import helper.Util;
import log.UtilLogger;
import net.muelleml.NLP.SLQS.DBWrapper;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.omg.CORBA.TCKind;

import com.google.common.base.Joiner;

public class SLQS {

	public static void main(String[] args) {
		UtilLogger.SetupLogger();
		Logger logger = LogManager.getLogger(SLQS.class);

		logger.info("Java Runtime Environment: " + System.getProperty("java.version"));

		Util.PrintUsage();

		logger.info("Started Jar: \""
				+ new java.io.File(SLQS.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName()
				+ "\" with arguments: " + Joiner.on(' ').join(args));

		logger.info("Welcome to SLQS");

		Options opt = GetOptions();
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(opt, args);

			if (cmd.getOptions().length == 0) {
				throw new ParseException("No arguments found! Please have a look at the available arguments!");
			}

			String db = "";
			String dn = "";
			String dp = "";
			String rels = "";
			String enc = "UTF-8";
			String format = "sdeWaC";
			String rFormat = "IMS";
			String expt = "";
			String exp = "";
			String addex = "";

			boolean useMsSql = false;
			boolean ac = false;
			boolean iZip = false;

			int n = 50;
			int sents = 0;
			int window = 0;

			List<Integer> entList = new LinkedList<Integer>();
			List<String> corpList = new LinkedList<String>();

			for (Option o : cmd.getOptions()) {
				logger.info(o.toString());
			}

			if (cmd.hasOption("ms")) {
				useMsSql = true;
			}

			if (cmd.hasOption("iz")) {
				iZip = true;
			}

			if (cmd.hasOption("ac")) {
				ac = true;
			}

			if (cmd.hasOption("e")) {
				enc = cmd.getOptionValue("e");
			}

			if (cmd.hasOption("rf")) {
				rFormat = cmd.getOptionValue("rf");
			}

			if (cmd.hasOption("f")) {
				format = cmd.getOptionValue("f");
			}

			if (cmd.hasOption("addex")) {
				addex = cmd.getOptionValue("addex");
			}

			if (cmd.hasOption("p") && cmd.hasOption("n")) {
				dp = cmd.getOptionValue("p");
				dn = cmd.getOptionValue("n");
				db = dp + dn;

				new DBWrapper(dp, dn, useMsSql);
			}

			if (cmd.hasOption("c")) {
				for (String s : cmd.getOptionValues("c")) {
					try {
						File f = new File(s);
						if (f.exists() && f.isFile()) {
							corpList.add(s);
						} else {
							// throw new Exception();
						}
					} catch (Exception e) {
						throw new ParseException(String.format("File does not exist: %1s", s));
					}
				}
			}

			if (cmd.hasOption("r")) {
				rels = cmd.getOptionValue("r");
			}

			if (cmd.hasOption("dp")) {
				dp = cmd.getOptionValue("dp");
			}

			if (cmd.hasOption("w")) {
				String tW = cmd.getOptionValue("w").trim();
				try {
					window = Integer.parseInt(tW.trim());
				} catch (Exception e) {
					throw new ParseException(String.format("Couldnt parse number: %1s", tW));
				}
			}

			if (cmd.hasOption("N")) {
				for (String s : cmd.getOptionValues("N")) {
					try {
						entList.add(Integer.parseInt(s));
					} catch (Exception e) {
						throw new ParseException(String.format("Not a parsable argument for \"N\": %1s", s));
					}
				}
			}

			if (cmd.hasOption("s")) {
				sents = Integer.parseInt(cmd.getOptionValue("s"));

			} else {
				sents = Integer.MAX_VALUE;
			}

			if (cmd.hasOption("expt")) {
				expt = cmd.getOptionValue("expt");
			}
			if (cmd.hasOption("exp")) {
				exp = cmd.getOptionValue("exp");
			}

			if (cmd.hasOption("i")) {
				if (db.isEmpty() || corpList.isEmpty() || sents == 0) {
					throw new ParseException("You have to specify a Corpus and DB");
				} else {
					HashImporter.HashToDB(corpList, db, window, sents, enc, format, ac, iZip);
				}
			}

			if (cmd.hasOption("ir")) {
				if (rels.isEmpty()) {
					throw new ParseException("You have to specify Relations Data");
				} else {
					Util.ReadRelations(rels, rFormat, enc);
				}
			}

			if (cmd.hasOption("clearex")) {
				if (db.isEmpty()) {
					throw new ParseException("You have to specify a DB.");
				} else {
					Util.ClearEx();
				}
			}

			if (cmd.hasOption("addex")) {
				if (db.isEmpty()) {
					throw new ParseException("You have to specify a DB.");
				} else {
					String[] expCmd = addex.split(";");
					String expName = expCmd[0];
					String expDesc = expCmd[1];

					Util.AddEx(expName, expDesc, Arrays.copyOfRange(expCmd, 2, expCmd.length));
				}
			}

			if (cmd.hasOption("ev")) {
				if (db.isEmpty()) {
					throw new ParseException(
							"You have to specify a DB. Make sure you have already imported the Relations Data");
				} else {
					Util.Evalues(entList);
				}
			}

			if (cmd.hasOption("cos")) {
				if (db.isEmpty()) {
					throw new ParseException(
							"You have to specify a DB. Make sure you have already imported the Relations Data");
				} else {
					Util.Cosine();
				}
			}

			if (cmd.hasOption("ap")) {
				Util.AveragePrecicision();
			}

			if (cmd.hasOption("exp")) {
				if (db.isEmpty()) {
					throw new ParseException(
							"You have to specify a DB. Make sure you have already imported the Relations Data");
				} else {
					if (exp.isEmpty()) {
						throw new ParseException("You have to specify an table");
					} else if (expt.isEmpty()) {
						throw new ParseException("You have to specify an exportpath / file");
					} else {
						Util.ExportTable(exp, expt);
					}

				}
			}

			DBWrapper.getInstance().close();
		} catch (ParseException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("SLQS", opt);

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	private static Options GetOptions() {
		Options opts = new Options();
		Option o;

		o = new Option("c", "Corpus Data to read");
		o.setArgs(Option.UNLIMITED_VALUES);
		opts.addOption(o);

		opts.addOption("r", true, "Semantic Realations Data to read");
		opts.addOption("p", true, "DB path to save to. e.g. c:\\myDbDirectory\\");
		opts.addOption("n", true, "DB to save to. e.g. myLittleDb.sqlite");
		opts.addOption("w", true, "Windowsize to consider");
		opts.addOption("s", true, "Number of Sentences to import. Ommit to import all");

		opts.addOption("i", false, "Import corpus to db");
		opts.addOption("ir", false, "Import Relations Data to db");

		opts.addOption("e", true, "Encoding of the data. Either UTF-8 or ISO-8859-1. Ommit to use UTF-8");
		opts.addOption("f", true, "Format of the Corpus. either sdeWaC or wacky");

		opts.addOption("rf", true, "Format of the Relations. Either IMS or BLESS");

		o = new Option("N", true,
				"N most associated contexts by LMI. Ommit to use the default: 50. You can have also multiple arguments, eg: 1 2 5 10 etc");
		o.setArgs(Option.UNLIMITED_VALUES);
		opts.addOption(o);

		opts.addOption("ev", false, "Calculate E-Values");
		opts.addOption("ac", false, "Consider all tokens as content");
		opts.addOption("iz", false, "The Corpus to import is in a compressed file. Supported are BZ2 and GZIP.");
		opts.addOption("cos", false, "Calculate Cosine Similarity.");
		opts.addOption("exp", true, "Export this table to \"expt\"");
		opts.addOption("expt", true, "Exporttarget. Export a table to this file.");

		opts.addOption("ap", false, "Calculate Average Precision for IS-A Relation vs Other discrimination.");
		opts.addOption("clearex", false, "Clears all Tag Experiments and defaults to all Tags considered");
		opts.addOption("addex", true,
				"Adds an Tag Experiment. Add Experiments before you calculate E-Values and CosineSim");
		return opts;
	}
}
