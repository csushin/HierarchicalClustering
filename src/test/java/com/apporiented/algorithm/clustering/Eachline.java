package test.java.com.apporiented.algorithm.clustering;

public class Eachline implements Comparable<Eachline> {
	public Eachline(int index){
		this.index = index;
	}
	
	public int index;
	public String pointid;
	
	@Override
	public int compareTo(Eachline line) {
		int compareindex = ((Eachline)line).index;
		/* For Ascending order*/
		return this.index - compareindex;
		
		 /* For Descending order do like this */
        //return compareage-this.studentage;
	}
}