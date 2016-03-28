package net.muelleml.NLP.SLQS;

import org.apache.commons.lang3.builder.ToStringStyle;

public class Entry implements Comparable<Entry> {

	public String tag = "";
	public String lemma = "";

	private String stringRep;

	public int id = 0;

	public Entry(String line, int id) {
		String[] tc = line.split("\\s");
		if (tc.length == 3) {
			tag = tc[1];
			lemma = tc[2];
			this.id = id;
		} else {
			tag = "<nullTAG>";
			lemma = "<nullLEMMA>";
			this.id = id;
		}

		//stringRep = lemma + "\t" + tag;
	}

	public Entry(String token, String tag, String lemma, int id) {
		super();
		this.tag = tag;
		this.lemma = lemma;
		this.id = id;
		
		//stringRep = lemma + "\t" + tag;
	}

	@Override
	public String toString() {
		//return stringRep;
		return lemma + "\t" + tag;
	}

	public String toString(Boolean verbose) {
		String r = "";
		if (verbose) {
			return "Entry [tag=" + tag + ", lemma="
					+ lemma + ", id=" + id + "]";
		}
		//return stringRep;
		return toString();
	}

	public int compareTo(Entry o) {
		return this.toString().compareTo(o.toString());
	}

}
