package DISC.EpochBasedRtree;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Epoch_Based_Rtree implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static int DEFAULT_MAX_NODE_ENTRIES = 50;
	private final static int DEFAULT_MIN_NODE_ENTRIES = 20;

	int dim;
	int maxNodeEntries;
	int minNodeEntries;
	
	

	private final static int ENTRY_STATUS_ASSIGNED_TO_OLD = 1;
	private final static int ENTRY_STATUS_UNASSIGNED = 0;
	private final static int ENTRY_STATUS_ASSIGNED_TO_NEW = 2;

	private byte[] entryStatus = null;
	private byte[] initialEntryStatus = null;

	
	Node root = null;

	private Deque<Node> BFSdeque = new ArrayDeque<Node>();
	private Deque<Node> deque = new ArrayDeque<Node>();
	private Deque<Integer> dequeID = new ArrayDeque<Integer>();
	private Deque<Node> deleteNode = new ArrayDeque<Node>();
	public List<Element> Results = new ArrayList<Element>(); 
	public int size = 0;
	public int height = 0;



	public Epoch_Based_Rtree(int dim, int maxN, int minN) {
		this.dim = dim;
		if (maxN == -1 && minN == -1) {
			maxNodeEntries = DEFAULT_MAX_NODE_ENTRIES;
			minNodeEntries = DEFAULT_MIN_NODE_ENTRIES;
		} else {
			maxNodeEntries = maxN;
			minNodeEntries = minN;
			
		}
		entryStatus = new byte[maxNodeEntries];
		initialEntryStatus = new byte[maxNodeEntries];

		for (int i = 0; i < maxNodeEntries; i++) {
			initialEntryStatus[i] = ENTRY_STATUS_UNASSIGNED;
		}

		root = new Node(maxNodeEntries, dim);
		root.height = 0;
		root.isLeafNode = true;

	}


	
	public void preOrder (Node cur, BufferedWriter pointW, BufferedWriter mbrW) throws IOException
	{
		if(cur.isLeafNode)
		{
			for( int i = 0 ; i < dim; i++){
			mbrW.write(cur.mbr.lower[i]+" ");
			mbrW.write(cur.mbr.upper[i]+" ");
			}
			mbrW.write("\n");
		}
		else
		{
			for( int i = 0 ; i < dim; i++){
				mbrW.write(cur.mbr.lower[i]+" ");
				mbrW.write(cur.mbr.upper[i]+" ");
			}
			mbrW.write("\n");
			
			for( int i = 0 ; i < cur.EntryCount; i++){
				preOrder(cur.children[i], pointW, mbrW);
			}
		}	
	}


	public int  height() throws IOException
	{
		return height(root);
	}

	
	public int  height (Node cur) throws IOException
	{
		if(cur.isLeafNode)
		{
			return 1;
		}
		else
		{
			int max = -1;
			for( int i = 0 ; i < cur.EntryCount; i++){
				int h = height(cur.children[i]);
				if( max <  h )  max = h; 
			}
			return max+1;
		}	
	}

	
	
	public void BFS(BufferedWriter pointW, BufferedWriter mbrW) throws IOException
	{
		BFSdeque.clear();
		BFSdeque.add(root);
		while(!BFSdeque.isEmpty()){
			Node cur = BFSdeque.remove();
			
			if(cur.isLeafNode)
			{
			
				for( int i = 0 ; i < dim; i++){
				mbrW.write(cur.mbr.lower[i]+" ");
				mbrW.write(cur.mbr.upper[i]+" ");
				}
				mbrW.write("\n");
			}
			else
			{
				for( int i = 0 ; i < dim; i++){
					mbrW.write(cur.mbr.lower[i]+" ");
					mbrW.write(cur.mbr.upper[i]+" ");
				}
				mbrW.write("\n");
				
				for( int i = 0 ; i < cur.EntryCount; i++){
					BFSdeque.add(cur.children[i]);
				}
			}	
		}
	}
	

	private Node ChooseNode(MBR newBox, int height) {

		deque.clear();

		Node cur = root;

		
		while (true) {
			if (cur == null) {
				System.out.println("ChooseNode_ERROR");
			}

			if (cur.height == height+1 ) {
				return cur;
			}
			
			deque.push(cur);
			
			double leastEnlargement = enlargement(cur.children[0].mbr, newBox);

			int index = 0; 
			for (int i = 1; i < cur.EntryCount; i++) {
				double tempEnlargement = enlargement(cur.children[i].mbr,
						newBox);

				if ((tempEnlargement < leastEnlargement)
						|| ((tempEnlargement == leastEnlargement) && (cur.children[i].mbr
								.getArea() < cur.children[index].mbr.getArea()))) {
					index = i;
					leastEnlargement = tempEnlargement;
				}
			}

			cur = cur.children[index];
		}
	}

	
	
	
	
	public void ReInsert(Node l)
	{
		Node TargetNode = ChooseNode(l.mbr, l.height);
		Node splitedNode = null;
		if (TargetNode.EntryCount < maxNodeEntries) {
			TargetNode.addEntry(l);
		} else {
			splitedNode = splitNode(TargetNode, l);
		}
		Node newNode = adjustTree(TargetNode, splitedNode);
		if (newNode != null) {
			Node newRoot = new Node(maxNodeEntries, dim);
			newRoot.height = root.height+1;
			newRoot.addEntry(newNode);
			newRoot.addEntry(root);
			root = newRoot;
		}
		
	}
	

	public void Insert(double[] Ls, double[] Us, Element e) {

		Node LeafEntry = new Node(1, dim, Ls, Us, e);
		LeafEntry.isLeafEntry = true;

		
		Node LeafNode = ChooseLeaf(LeafEntry.mbr);

		Node newLeaf = null;
		if (LeafNode.EntryCount < maxNodeEntries) {
			LeafNode.addEntry(LeafEntry);
			
		} else {
			
			newLeaf = splitNode(LeafNode, LeafEntry);
			
		}

		
		Node newNode = adjustTree(LeafNode, newLeaf);
		
		if (newNode != null) {
			Node newRoot = new Node(maxNodeEntries, dim);
			newRoot.height = root.height+1;
			newRoot.addEntry(newNode);
			newRoot.addEntry(root);
			root = newRoot;
		}
		

		this.size++;

	}

	/**
	 * @see net.sf.jsi.SpatialIndex#delete(Rectangle, int)
	 */

	public boolean find(MBR box, Element e) {
		deque.clear();
		dequeID.clear();
		deque.push(root);

		return search(box,e); 
	}
	
	
	private boolean search(MBR box, Element e) {
		Node n = deque.peek();
		if (!n.isLeafNode) {
			for (int i = 0; i < n.EntryCount; i++) {
				boolean contains = true;
				for (int k = 0; k < dim; k++) {
					if (box.lower[k] < n.mbr.lower[k]
							|| box.upper[k] > n.mbr.upper[k]) {
						contains = false;
						break;
					}
				}

				if (contains) {
					deque.push(n.children[i]);
					dequeID.push(i);
					if (search(box, e)) {
						return true;
					}
					deque.pop();
					dequeID.pop();
				}				

			}
			return false;
		} else {
			for (int i = 0; i < n.EntryCount; i++) {

				if (e.equals(n.children[i].element)) {
					dequeID.push(i);
					return true;
				}
			}
			return false;
		}
		
		

	}
	
	private void subBFS( )
	{
		if(deque.isEmpty()) return;
		
		Node n = deque.remove();
		if(n.EntryCount<8)System.out.println("HEIGHT: "+n.height+" Entry#:" + n.EntryCount);
		if (!n.isLeafNode) {
			for (int i = 0; i < n.EntryCount; i++) {
				deque.add(n.children[i]);		
			}	
		}
		subBFS();
	}

	public void BFSearch() {
		
		deque.clear();
		deque.add(root);
		subBFS();			
	}

	
	public boolean delete(MBR box, Element e) {

		deleteNode.clear();
		deque.clear();
		dequeID.clear();
		deque.push(root);
		

		if(  search(box,e) )
		{
		
			
			DeleteAndCondenseTree();
			size--;
					
			while (root.EntryCount == 1 && root.height > 0) {
				root = root.children[0];
			}
	
			if (size == 0) {

				root.mbr.SetLEmpty();
				root.mbr.SetUEmpty();
		
			}
			return true;
			
		}
		else
		{
			System.out.println("ERROR");
			return false;
		}

	}
	

	
	private void subdelete(Node l, boolean check)
	{
		if( deque.isEmpty() )
		{
			if( check )
			{
				l.recalculateMBR();
			}
			 
			return;
		}
		
		Node cur = deque.pop();
		int index = dequeID.pop();
		boolean check2 = cur.checkIfInfluencedBy(l.mbr);
		
		if( check )
		{
			l.recalculateMBR();
		}
		
		if(l.EntryCount < minNodeEntries)
		{
			for(int j = 0 ; j< l.EntryCount ; j++)
			{
				deleteNode.add(l.children[j]);
			}
			cur.deleteEntry(index);
			subdelete(cur, check2);
		}
		else
		{
			subdelete(cur, check2);
		}		
	}
	
	private void DeleteAndCondenseTree() {
		
		
		
		Node cur = deque.pop();
		int deleteID = dequeID.pop();
		boolean check = cur.checkIfInfluencedBy(cur.children[deleteID].mbr);
		cur.deleteEntry(deleteID);
		subdelete(cur, check);
		
		for(Node e :  deleteNode)
		{
			ReInsert(e);
		}
		
	}


	private Node splitNode(Node oldNode, Node newEntry) {

		System.arraycopy(initialEntryStatus, 0, entryStatus, 0, maxNodeEntries);


		Node newNode = null;

		newNode = new Node(maxNodeEntries, dim);
		if (oldNode.isLeafNode) {
			newNode.isLeafNode = true;
			newNode.height = 0;
		}
		else
		{
			newNode.height = oldNode.height;
		}

		pickSeeds(oldNode, newEntry, newNode); 
		
		while (oldNode.EntryCount + newNode.EntryCount < maxNodeEntries + 1) {
			if (maxNodeEntries + 1 - newNode.EntryCount == minNodeEntries) {

				for (int i = 0; i < maxNodeEntries; i++) {
					if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {
						entryStatus[i] = ENTRY_STATUS_ASSIGNED_TO_OLD;

						for (int j = 0; j < dim; j++) {
							if (oldNode.mbr.lower[j] > oldNode.children[i].mbr.lower[j])
								oldNode.mbr.lower[j] = oldNode.children[i].mbr.lower[j];
							if (oldNode.mbr.upper[j] < oldNode.children[i].mbr.upper[j])
								oldNode.mbr.upper[j] = oldNode.children[i].mbr.upper[j];
						}

						oldNode.EntryCount++;
					}
				}
				break;
			}
			if (maxNodeEntries + 1 - oldNode.EntryCount == minNodeEntries) {
				for (int i = 0; i < maxNodeEntries; i++) {
					if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {
						entryStatus[i] = ENTRY_STATUS_ASSIGNED_TO_NEW;
						newNode.addEntry(oldNode.children[i]);
						oldNode.children[i] = null;

					}
				}
				break;
			}

			pickNext(oldNode, newNode);
		}

		
		oldNode.reorganize(maxNodeEntries);
		
		
		
		return newNode;
	}

	/**
	 * Linear-Cost Algorithm
	 */
	private void pickSeeds(Node oldN, Node newEntry, Node newN) {
		
		double maxNormalizedSeparation = -1;
		int highestLowIndex = -1;
		int lowestHighIndex = -1;

		MBR mbr = oldN.mbr;
		MBR box = newEntry.mbr;
		for (int i = 0; i < dim; i++) {
			if (mbr.lower[i] > box.lower[i])
				mbr.lower[i] = box.lower[i];
			if (mbr.upper[i] < box.upper[i])
				mbr.upper[i] = box.upper[i];
		}

		for (int z = 0; z < dim; z++) {

			double maxLen = mbr.upper[z] - mbr.lower[z];

			double tempHighestLow = box.lower[z];
			int tempHighestLowIndex = -1;

			double tempLowestHigh = box.upper[z];
			int tempLowestHighIndex = -1;

			for (int i = 0; i < oldN.EntryCount; i++) {
				double tempLow = oldN.children[i].mbr.lower[z];
				double tempHigh = oldN.children[i].mbr.upper[z];
				if (tempLow >= tempHighestLow) {
					tempHighestLow = tempLow;
					tempHighestLowIndex = i;
				} else if (tempHigh <= tempLowestHigh) {
					tempLowestHigh = tempHigh;
					tempLowestHighIndex = i;
				}

				double normalizedSeparation = maxLen == 0 ? -1
						: (tempHighestLow - tempLowestHigh) / maxLen;

				if (normalizedSeparation >= maxNormalizedSeparation) {
					highestLowIndex = tempHighestLowIndex;
					lowestHighIndex = tempLowestHighIndex;
					maxNormalizedSeparation = normalizedSeparation;
				}
			}
		}

		if (highestLowIndex == lowestHighIndex) {
			lowestHighIndex = 0;
			highestLowIndex = -1;
			
			
		}

		if (highestLowIndex == -1) {
			newN.addEntry(newEntry);
		} else {
			newN.addEntry(oldN.children[highestLowIndex]);

			oldN.children[highestLowIndex] = (newEntry);
		}

		if (lowestHighIndex == -1) {
			lowestHighIndex = highestLowIndex;
		}

		entryStatus[lowestHighIndex] = ENTRY_STATUS_ASSIGNED_TO_OLD;
		oldN.EntryCount = 1;

		for (int i = 0; i < dim; i++) {
			oldN.mbr.lower[i] = oldN.children[lowestHighIndex].mbr.lower[i];
			oldN.mbr.upper[i] = oldN.children[lowestHighIndex].mbr.upper[i];
		}
	}

	
	private int pickNext(Node oldNode, Node newNode) {
		Double maxDifference = Double.NEGATIVE_INFINITY;
		int next = 0;
		int nextGroup = 0;
		if (oldNode.isLeafEntry) {
			System.out.println("pickNext_ERROR!");
		}

		maxDifference = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < maxNodeEntries; i++) {
			if (entryStatus[i] == ENTRY_STATUS_UNASSIGNED) {

				double oldNodeInc = enlargement(oldNode.mbr,
						oldNode.children[i].mbr);
				double newNodeInc = enlargement(newNode.mbr,
						oldNode.children[i].mbr);

				double difference = Math.abs(oldNodeInc - newNodeInc);

				if (difference > maxDifference) {
					next = i;

					if (oldNodeInc < newNodeInc) {
						nextGroup = 0; // old
					} else if (newNodeInc < oldNodeInc) {
						nextGroup = 1; // new
					} else if (oldNode.mbr.getArea() < newNode.mbr.getArea()) {
						nextGroup = 0; // old
					} else if (newNode.mbr.getArea() < oldNode.mbr.getArea()) {
						nextGroup = 1;// new
					} else if (newNode.EntryCount < maxNodeEntries / 2) {
						nextGroup = 0;
					} else {
						nextGroup = 1;
					}
					maxDifference = difference;
				}
			}
		}

		;

		if (nextGroup == 0) {
			for (int j = 0; j < dim; j++) {
				if (oldNode.mbr.lower[j] > oldNode.children[next].mbr.lower[j])
					oldNode.mbr.lower[j] = oldNode.children[next].mbr.lower[j];
				if (oldNode.mbr.upper[j] < oldNode.children[next].mbr.upper[j])
					oldNode.mbr.upper[j] = oldNode.children[next].mbr.upper[j];
			}
			oldNode.EntryCount++;
			entryStatus[next] = ENTRY_STATUS_ASSIGNED_TO_OLD;

		} else {
			// move to new node.
			newNode.addEntry(oldNode.children[next]);
			oldNode.children[next] = null;
			entryStatus[next] = ENTRY_STATUS_ASSIGNED_TO_NEW;
		}

		return next;
	}

	
	public List<Element> intersects(MBR box, Node cur) {
	
		boolean flag = false;
		for( int j = 0; j < dim ; j++)
		{
			if( cur.getMBR().upper[j] < box.lower[j] || cur.getMBR().lower[j] > box.upper[j] )
			{
				flag = false ;
				break;
			}
			else 
			{
				flag = true;
			}
		}
		
		if( flag ){
			if (cur.isLeafNode) {
					Results.addAll(cur.SearchNodes(box));
			}
			else{
				for (int i = 0; i < cur.EntryCount; i++) {
					intersects(box, cur.children[i]);
				}
			}
		}
		
		return Results;
	}
	
	public List<Element> Epoch_based_Probe(MBR box, Node cur , double x, double y, double eps, int tick ) {
		
		boolean flag = false;
		if( cur.tick1 < tick ){
			cur.tick1 = tick; 
			cur.checkCount = 0;
		}
		
		if( cur.tick2 < tick){
		for( int j = 0; j < dim ; j++)
		{
			if( cur.getMBR().upper[j] < box.lower[j] || cur.getMBR().lower[j] > box.upper[j] )
			{
				flag = false ;
				break;
			}
			else 
			{
				flag = true;
			}
		}
		
		if( flag ){
			if (cur.isLeafNode) {
					Results.addAll(cur.SearchNodes(box, x, y, eps,tick));
			}
			else{
				for (int i = 0; i < cur.EntryCount; i++) {
					if( cur.children[i].tick2  < tick ) 
					{ 
						Epoch_based_Probe(box, cur.children[i], x, y, eps ,tick);
						if( cur.children[i].tick2 == tick )
						{
							cur.checkCount++;
						}
					}
					
				}
				if( cur.checkCount == cur.EntryCount)
				{
					cur.tick2 = tick;
				}
			}
		}
		
		}
		return Results;
	}
	
	
public List<Element> Epoch_based_Probe(MBR box, Node cur , double[] x , double eps, int tick ) {
		
		boolean flag = false;
		if( cur.tick1 < tick ){
			cur.tick1 = tick; 
			cur.checkCount = 0;
		}
		
		if( cur.tick2 < tick){
		for( int j = 0; j < dim ; j++)
		{
			if( cur.getMBR().upper[j] < box.lower[j] || cur.getMBR().lower[j] > box.upper[j] )
			{
				flag = false ;
				break;
			}
			else 
			{
				flag = true;
			}
		}
		
		if( flag ){
			if (cur.isLeafNode) {
					Results.addAll(cur.SearchNodes(box, x, eps,tick));
			}
			else{
				for (int i = 0; i < cur.EntryCount; i++) {
					if( cur.children[i].tick2  < tick ) 
					{ 
						Epoch_based_Probe(box, cur.children[i], x, eps ,tick);
						if( cur.children[i].tick2 == tick )
						{
							cur.checkCount++;
						}
					}
					
				}
				if( cur.checkCount == cur.EntryCount)
				{
					cur.tick2 = tick;
				}
			}
		}
		
		}
		return Results;
	}
	
	
	
	public List<Element> intersects(MBR box) {
		Results.clear();
		return intersects(box,root);
	}
	
	public List<Element> Epoch_based_Probe(MBR box, double x, double y, double eps, int tick ) {
		Results.clear();
		return Epoch_based_Probe(box,root,  x,  y,  eps , tick );
	}

	public List<Element> Epoch_based_Probe(MBR box, double x[], double eps, int tick ) {
		Results.clear();
		return Epoch_based_Probe(box,root,  x,  eps , tick );
	}

	

	private double enlargement(MBR baseBox, MBR newBox) {
		double size1 = 1;
		double size2 = 1;

		for (int i = 0; i < dim; i++) {
			size2 = size2 * (Math.max(baseBox.upper[i], newBox.upper[i]) - Math.min(baseBox.lower[i], newBox.lower[i]));
			size1 = size1 * (baseBox.upper[i] - baseBox.lower[i]);
		}
		
		return ( size2- size1 );
	}

	private Node ChooseLeaf(MBR newBox) {

		deque.clear();

		Node cur = root;

		while (true) {
			if (cur == null) {
				System.out.println("ChooseLeaf_ERROR");
			}

			if (cur.isLeafNode) {
				return cur;
			}
			
			
			deque.push(cur);

		
			
			double leastEnlargement = enlargement(cur.children[0].mbr, newBox);

			int index = 0; // index of rectangle in subtree
			for (int i = 1; i < cur.EntryCount; i++) {

				double tempEnlargement = enlargement(cur.children[i].mbr,
						newBox);

				if ((tempEnlargement < leastEnlargement)
						|| ((tempEnlargement == leastEnlargement) && (cur.children[i].mbr
								.getArea() < cur.children[index].mbr.getArea()))) {
					index = i;
					leastEnlargement = tempEnlargement;
				}
			}

			cur = cur.children[index];
		}
	}


	private Node adjustTree(Node oldNode, Node newNode) {
		if (deque.isEmpty()) {
			return newNode;
		} else {
			Node parent = deque.pop();
			parent.recalculateMBR();

			Node newNode2 = null;
			if (newNode != null) {
				if (parent.EntryCount < maxNodeEntries) {
					parent.addEntry(newNode);
				} else {
					newNode2 = splitNode(parent, newNode);
				}
			}

			return adjustTree(parent, newNode2);

		}
	}

	
}