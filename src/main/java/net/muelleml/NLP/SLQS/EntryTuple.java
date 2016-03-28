package net.muelleml.NLP.SLQS;

public class EntryTuple implements Comparable<EntryTuple> {
	public Entry e1;
	public Entry e2;

	private String stringRep;

	public EntryTuple(Entry e1, Entry e2) {
		super();
		this.e1 = e1;
		this.e2 = e2;
		stringRep = e1.toString() + "\t" + e2.toString();
	}

	public String toString() {
		return stringRep;
	}

	public int compareTo(EntryTuple o) {
		return stringRep.compareTo(o.toString());
	}

}
