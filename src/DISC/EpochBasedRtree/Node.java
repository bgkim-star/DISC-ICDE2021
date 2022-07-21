package DISC.EpochBasedRtree;


import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class Node implements Serializable {

	private static final long serialVersionUID = 3597704208751356660L;

	
	boolean isLeafNode = false;
	boolean isLeafEntry = false;
	int tick1 = 0;
	int tick2 = 0;
	int checkCount = 0 ; 
	int EntryCount = 0 ;

	int dim;
	int height = -1;
	MBR mbr  = null;
	Node[] children = null;
	Element element = null;
	
	public Node(int maxNodeEntries, int dim, double[] Ls, double[] Us, Element e) {
		this.dim = dim;
		children = new Node[maxNodeEntries];
		mbr = new MBR(dim);
		mbr.SetL(Ls);
		mbr.SetU(Us);
		element = e;
	}
	
	public Node(int maxNodeEntries, int dim) {
		this.dim = dim;
		children = new Node[maxNodeEntries];
		mbr = new MBR(dim);
		
	}
	
	public MBR getMBR()
	{
		return mbr;
	}
	
	void addEntry(Node child) {
		
		children[EntryCount] = child;
		MBR box = child.getMBR();
		
		for( int i = 0 ; i < dim ; i++ )
		{
			if( mbr.lower[i] > box.lower[i] ) mbr.lower[i] = box.lower[i];
			if( mbr.upper[i] < box.upper[i] ) mbr.upper[i] = box.upper[i];
		}
		EntryCount++;
	}

	
	List<Element> SearchNodes(MBR box) {
				
	List<Element> result = new LinkedList<Element>();

		for (int i = 0; i < EntryCount; i++) {
			boolean flag = false;
			for( int j = 0; j < dim ; j++)
			{
				if( (children[i].getMBR().upper[j] < box.lower[j]) || (children[i].getMBR().lower[j] > box.upper[j]) )
				{
					flag = false ;
					break;
				}
				else 
				{
					flag = true;
				}
			}
			
			
			
			if( flag ){ result.add(children[i].element);}
		}
				
		return result;
		
	}
	
	List<Element> SearchNodes(MBR box, double x, double y, double eps, int tick) {
		
		List<Element> result = new LinkedList<Element>();

		if( this.tick1 < tick) 
		{
			this.tick1 = tick;
			checkCount = 0;
		}
		
		for (int i = 0; i < EntryCount; i++) {
			
			if( children[i].tick2 < tick)
			{
				if((children[i].getMBR().upper[0]-x)*(children[i].getMBR().upper[0]-x)+(children[i].getMBR().upper[1]-y)*(children[i].getMBR().upper[1]-y) <= eps*eps )
				{
					result.add(children[i].element);
					checkCount++;
				
					children[i].tick2 = tick;
				}
			}
		}
		
		if(checkCount == EntryCount)
		{
			this.tick2 = tick;
		}
				
		return result;
		
	}
	

	List<Element> SearchNodes(MBR box, double[] x,  double eps, int tick) {
		
		List<Element> result = new LinkedList<Element>();

		if( this.tick1 < tick) 
		{
			this.tick1 = tick;
			checkCount = 0;
		}
		
		for (int i = 0; i < EntryCount; i++) {
			
			if( children[i].tick2 < tick)
			{
				double dist = 0;
				
				for( int d = 0 ; d < x.length ; d++)
				{
					dist += (children[i].getMBR().upper[d] - x[d])*(children[i].getMBR().upper[d] - x[d]);
				}
				
				if(dist <= eps*eps )
				{
					result.add(children[i].element);
					checkCount++;
					children[i].tick2 = tick;
				}
			}
		}
		
		if(checkCount == EntryCount)
		{
			this.tick2 = tick;
		}
				
		return result;
		
	}
	
	

	void deleteEntry(int i) {
		int lastIndex = --EntryCount;
		
		if (i != lastIndex) {
			children[i] = children[lastIndex];
		}
	}

	boolean checkIfInfluencedBy(MBR box) {
		for( int i = 0 ; i < dim ; i++)
		{
			if( box.lower[i] == mbr.lower[i]  ||  box.upper[i] == mbr.upper[i] )
			{
				return true;
			}
		}
		return false;
		
	}

	void recalculateMBR() {
		
		
		MBR box = children[0].getMBR();
		
		for( int i = 0 ; i < dim ; i++ )
		{
			mbr.lower[i] = box.lower[i];
			mbr.upper[i] = box.upper[i];
		}
		
		for (int j = 1; j < EntryCount; j++) {
			box = children[j].getMBR();
			for( int i = 0 ; i < dim ; i++ )
			{
				if( mbr.lower[i] > box.lower[i] ) mbr.lower[i] = box.lower[i];
				if( mbr.upper[i] < box.upper[i] ) mbr.upper[i] = box.upper[i];
			}
		}
	}

	

	void reorganize(int maxNodeEntries) {
		int position = 0;
		for (int index = 0; index < maxNodeEntries; index++) {
			if(children[index] != null)
			{
				children[position] = children[index]; 
				if( index != position ) children[index] = null;
				position++;
			}
		}
	}

	public int getEntryCount() {
		return EntryCount;
	}

	public Node getChild(int index) {
		if (index < EntryCount) {
			return children[index];
		}
		return null;
	}

		
	
	
}