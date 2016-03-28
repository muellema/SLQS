import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import net.muelleml.NLP.SLQS.DBWriter;
import net.muelleml.NLP.SLQS.Entry;
import net.muelleml.NLP.SLQS.EntryTuple;

public class HashImporter {
	private static final Logger logger = LogManager
			.getLogger(HashImporter.class);

	private static Set<String> ignoreSet = null;
	private static Set<String> tagSet = null;
	private static Map<String, String> tagMap = null;

	private static BufferedReader GetReaderForFile(String f, boolean isZip,
			Charset charset) {
		BufferedReader r = null;

		if (isZip) {
			logger.info("Reading Corpus as ZIP file: " + f);
			try {
				File initialFile = new File(f);
				InputStream inputStream = new FileInputStream(initialFile);
				BufferedInputStream targetStream = new BufferedInputStream(
						inputStream);

				CompressorInputStream input = new CompressorStreamFactory()
						.createCompressorInputStream(targetStream);

				r = new BufferedReader(new InputStreamReader(input, charset));

			} catch (FileNotFoundException | CompressorException e) {
				logger.error("Couldnt open file: " + f);
				e.printStackTrace();
			}

		} else {
			logger.info("Reading Corpus as flat text file: " + f);
			try {
				r = Files.newBufferedReader(Paths.get(f), charset);
			} catch (IOException e) {
				logger.error("Couldnt open file: " + f);
				e.printStackTrace();
			}
		}

		return r;
	}

	public static void HashToDB(List<String> corplList, String dbPath,
			int size, int sents, String enc, String format, boolean ac,
			boolean isZip) {

		DBWriter db = new DBWriter();

		Thread dbThread = new Thread(db);
		dbThread.setName("DBWriterThread");
		dbThread.start();

		int lineLength = 0;

		ignoreSet = new HashSet<String>();
		tagSet = new HashSet<String>();
		tagMap = new HashMap<String, String>(50);

		int idTag = 0;
		int idToken = 0;
		int idLemma = 0;

		Queue<Entry> queue = new ArrayBlockingQueue<Entry>(size);

		for (int i = 0; i < size; i++) {
			queue.add(new Entry("", 0));
		}

		if (format.toLowerCase().contains("wacky")) {
			lineLength = 6;

			tagMap.put("JJ", "ADJ");
			tagMap.put("JJR", "ADJ");
			tagMap.put("JJS", "ADJ");
			tagMap.put("NNS", "NOUN");
			tagMap.put("NP", "NOUN");
			tagMap.put("NPS", "NOUN");
			tagMap.put("NN", "NOUN");
			tagMap.put("VV", "VERB");
			tagMap.put("VVD", "VERB");
			tagMap.put("VVG", "VERB");
			tagMap.put("VVN", "VERB");
			tagMap.put("VVP", "VERB");
			tagMap.put("VBZ", "VERB");

			idTag = 2;
			idToken = 0;
			idLemma = 1;

			ignoreSet.add("<nullLemma>");
			ignoreSet.add("<unknown>");
		} else {
			lineLength = 3;

			tagMap.put("ADJ", "ADJ");
			tagMap.put("ADJA", "ADJ");
			tagMap.put("ADJD", "ADJ");
			tagMap.put("NE", "NOUN");
			tagMap.put("NN", "NOUN");
			tagMap.put("VVIMP", "VERB");
			tagMap.put("VVINF", "VERB");
			tagMap.put("VVFIN", "VERB");
			tagMap.put("VVIZU", "VERB");
			tagMap.put("VVPP", "VERB");

			idTag = 1;
			idToken = 0;
			idLemma = 2;

			ignoreSet.add("<nullLemma>");
			ignoreSet.add("<unknown>");
		}

		Charset charset = Charset.forName(enc);

		for (Object s : tagMap.values().toArray()) {
			tagSet.add((String) s);
		}

		try {

			logger.info("Started importing");

			int timeToWrite = 60;

			int mb = 1024 * 1024;

			// Getting the runtime reference from system
			Runtime runtime = Runtime.getRuntime();

			if ((runtime.maxMemory() / mb) > 16000) {
				timeToWrite = 10000;
			}

			Multiset<String> ms = ConcurrentHashMultiset.create();

			EvictingQueue<Float> perfQ = EvictingQueue.create(3);

			int c = 0;

			int sc = 0;

			StopWatch sw = new StopWatch();

			sw.start();

			StopWatch wd = new StopWatch();
			wd.start();

			String[] record;

			boolean even = false;

			int sid = 0;

			boolean insert = true;

			String line = "";

			BufferedReader reader = null;

			String tTag = "";

			for (String corp : corplList) {

				reader = GetReaderForFile(corp, isZip, charset);

				while ((line = reader.readLine()) != null && sents >= sid) {
					Entry tEntry;

					record = line.split("\\s");

					// LIKE 'N%' OR new.TAG LIKE 'VV%' OR new.TAG LIKE 'ADJ%' "

					if (record.length == lineLength) {
						insert = true;
						tTag = tagMap.get(record[idTag]);
						if (!ac) {
							insert = (tTag != null);
						}
						if (insert) {
							tEntry = new Entry(record[idToken], tTag,
									record[idLemma], c);

							for (String s : GetEntryTuples(queue, tEntry, size,
									c, ac)) {
								// ms.addAll(GetEntryTuples(queue, tEntry, size,
								// c, ac));
								ms.add(s);
							}
							c++;
							queue.poll();
							queue.add(tEntry);
						}
					} else {

						if (record[0].startsWith("<s>")) {

							sid++;

							queue.clear();
							for (int i = 0; i < size; i++) {
								queue.add(new Entry("", 0));
							}

							sc++;
							c = 0;
						} else if (record[0].startsWith("</s>")) {
							queue.clear();
							for (int i = 0; i < size; i++) {
								queue.add(new Entry("", 0));
							}
						}
					}
				}
				reader.close();
			}

			db.writeDequeue.add(ms.entrySet());
			logger.info("Imported: " + sc);
			logger.info("Finished!");
			logger.info("Now waiting for DB to finish up");
			db.shutdown();
			while (db.isRunning()) {
				Thread.sleep(1000);
			}

		} catch (IOException | InterruptedException e) {
			logger.error("Couldnt import data!");
			e.printStackTrace();
			db.shutdown();
			try {
				while (db.isRunning()) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		} finally {
		}

	}

	private static List<String> GetEntryTuples(Collection<Entry> eArr,
			Entry pivot, int size, int count, boolean ac) {
		List<String> r = new LinkedList<String>();

		if (!ignoreSet.contains(pivot.lemma) && tagSet.contains(pivot.tag)) {
			for (Entry val : eArr) {
				if (!ignoreSet.contains(val.lemma) && tagSet.contains(val.tag)) {
					r.add(pivot.toString() + "\t" + val.toString());
					r.add(val.toString() + "\t" + pivot.toString());
				}
			}
		}
		return r;
	}
}
