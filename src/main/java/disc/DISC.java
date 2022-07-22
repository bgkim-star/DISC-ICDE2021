package disc;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import disc.epochbasedrtree.Element;
import disc.epochbasedrtree.Epoch_Based_Rtree;
import disc.epochbasedrtree.MBR;
import disc.Point;
import disc.unionfind.UnionFind;



public class DISC implements Serializable{
	private static final long serialVersionUID = 1L;
	public static final int CORE = 1;
	public static final int NOISE = -1;
	public static final int UNCLASSIFIED = 0;
	public static final int DELETED = -2;

	int tick = 1;
	double eps;
	int minPts;
	int cluster_check;
	Iterable<Point> data;
	Epoch_Based_Rtree rtree;
	public UnionFind uf; 
	public int range_searches=0;

		
	Set<Point> Potential_Noises ;
	HashSet<Point> excore;
	HashSet<Point> neocore;
	Set<Point> C_out ;



	int ClusterID = 1;
	int dim;
	double[] ls ;
	double[] us ;
	MBR mbr ;


	public DISC(Iterable<Point> data, DBSCAN_options param_options, int dimension) {

		this.data = data ;
		eps = param_options.get_eps();
		minPts = param_options.get_minPts();
		dim = dimension;
		rtree = new Epoch_Based_Rtree(dim,50,20);
		
		excore = new HashSet<Point>();
		neocore = new HashSet<Point>();
		
		C_out	= new HashSet<Point>();
		Potential_Noises = new HashSet<Point>();
		
		ls = new double[dim];
		us = new double[dim];
		mbr  = new MBR(dim);
		uf = new UnionFind();

	}

	/** 
	 *  
	 *  Read stride_in and stride_out and update the N (= number of neighbors within epsilon distance) value for each corresponding point.
	 *  Return excores and neocores.
	 *  
	 *  */
	public void  COLLECT(List<Point> list_in, List<Point> list_out)
	{
		
		C_out.clear();
		excore.clear();
		neocore.clear();
		Potential_Noises.clear();
		HashSet<Point> INneighbors = new HashSet<Point>();
		HashSet<Point> OUTneighbors =  new HashSet<Point>();
		
		/* Update R-tree */
		
		if( list_out != null)
		for (Point out : list_out){
			if( out.isCore )
			{
				C_out.add(out);	//  To remove from rtree later  
			}
			else{
				Element e = new Element(out);
				for(int d = 0 ; d < dim ; d++)
				{
					ls[d] = out.p[d];
					us[d] = out.p[d];
				}
				mbr.SetL(ls);
				mbr.SetU(us);
				rtree.delete(mbr, e);
			}
			out.mark = DELETED;
		}
		
		if( list_out != null)
		for (Point out : list_out){
			Deque<Point> neighbors_of_p = neighbor_search(out.p);
			for (Point pp : neighbors_of_p) {
				if( pp.mark != DELETED )
				{
					pp.neighbors_count--;
				}
			}
			out.neighbors_count = 1;
			OUTneighbors.addAll(neighbors_of_p);
		}
		
		
		if( list_in != null)
		for(Point in : list_in ){
			Element e = new Element(in);
			for(int d = 0 ; d < dim ; d++)
			{
				ls[d] = in.p[d];
				us[d] = in.p[d];
			}
			rtree.Insert(ls, us, e);
		}
		
		if( list_in != null)
		for(Point in : list_in ){
			Deque<Point> neighbors_of_p = neighbor_search(in.p);
			/* UPDATE neighbors counter */

			int nc = 1;
			for (Point pp : neighbors_of_p) {
				if( pp.mark != UNCLASSIFIED && pp.mark != DELETED){
					pp.neighbors_count++;
				}
				if( pp.mark != DELETED ) nc++;
			}
			in.neighbors_count = nc;
			INneighbors.addAll(neighbors_of_p);

		}
		
		
		/* Find excores and neocores */

		for(Point p : OUTneighbors)
		{
			if( p.isCore && p.neighbors_count < minPts)
			{
				excore.add(p);
				if( p.mark != DELETED) p.mark = UNCLASSIFIED;
			}
		}

		for(Point p : INneighbors)
		{
			if( !p.isCore && p.neighbors_count >= minPts )
			{
				p.mark = UNCLASSIFIED;
				neocore.add(p);
			}
			else if ( !p.isCore && p.neighbors_count < minPts )
			{
				p.mark = NOISE;
			}
		}
		
	}
	
	
	

	/** 
	 *  
	 *  Using excores and neocores from COLLECT, update clusters.  
	 *  
	 *  */
	
	public void CLUSTER( )
	{
		cluster_check = ClusterID;	
		for (Point p : excore) {
			
			/* Prune R-(p) */
			if( p.isCore ){ 
					
				/* Find M(p) */
				HashSet<Point> minimal_bonding_cores = new HashSet<Point>(); 
				find_mimal_bonding_core(minimal_bonding_cores, p);				
				
				if( minimal_bonding_cores.size() > 1 )
				{
					/* run Multi-Starter BFS */
					MS_BFS(minimal_bonding_cores);
				}
			}
		}


		for (Point out : C_out){
			Element e = new Element(out);
			for(int d = 0 ; d < dim ; d++)
			{
				ls[d] = out.p[d];
				us[d] = out.p[d];
			}
				mbr.SetL(ls);
				mbr.SetU(us);
				rtree.delete(mbr, e);
		}


			
		for( Point Newcore : neocore)	
		{
			/* Prune R+(p) */
			if(!Newcore.isCore )
			{
				CreateOrMergeClusters(Newcore);
			}
		}
		
		
		/* Process potential noises */
		for(Point potential_noise : Potential_Noises)
		{
			if( potential_noise.mark == UNCLASSIFIED){
				Deque<Point> neighbors = neighbor_search(potential_noise.p);
				potential_noise.mark = NOISE;
				for(Point neighbor : neighbors){
					if( neighbor.isCore)
					{
						potential_noise.mark = neighbor.mark;
						break;
					}
				}
			}
		}
	}
	
	

	/**
	 * 
	 * Multi-Starter BFS 
	 * 
	 */
	public void MS_BFS(HashSet<Point> minimal_bonding_cores ){
				
		int OldId = minimal_bonding_cores.iterator().next().mark ;
		int L = ClusterID;

		HashMap<Integer, ArrayDeque<Point>> id2n = new HashMap<Integer, ArrayDeque<Point>>(); 
		Set<Point> copied_minimal_bonding_cores = new HashSet<Point>(minimal_bonding_cores);
		HashMap<Integer, Point> clusterID2point = new HashMap<Integer, Point>();
		HashMap<Point, Integer> point2clusterID = new HashMap<Point, Integer>();
		

		for( Point mbc : minimal_bonding_cores)
		{
			if( copied_minimal_bonding_cores.contains( mbc) ){
				/* Start BFS for each point respectively with unique id */
				uf.create(ClusterID);
				
				/* Initialize Queues*/
				id2n.put(ClusterID, new ArrayDeque<Point>(Arrays.asList(mbc)));
				
				/* temporarily mapping clusterId to point  */  
				clusterID2point.put(ClusterID, mbc);
				point2clusterID.put(mbc, ClusterID);
				mbc.mark = ClusterID;
				/* Increase tick for epoch-based probe */
				tick++;
				
				BFS(ClusterID, L, id2n,  OldId, copied_minimal_bonding_cores, clusterID2point);
				ClusterID++;
			}
		}
		
		/* Iterate BFSs until all minimum bonding cores are accessed by BFSs or all queues are exhausted. */  
		while( copied_minimal_bonding_cores.size() > 0 ){
			
			for(Point trj : minimal_bonding_cores)
			{
				if( copied_minimal_bonding_cores.contains(trj))
				{
					int cid = point2clusterID.get(trj);
					BFS(cid, L, id2n,  OldId, copied_minimal_bonding_cores,clusterID2point);
				
				}
			}
		}
		
	}
	
	

	
	public void CreateOrMergeClusters(Point p)
	{
		tick++;
		Set<Integer> ClustersToMerge = new HashSet<Integer>();
		Deque<Point> Queue_of_neocores_for_BFS =  new ArrayDeque<Point>();		
		Deque<Point> Queue_of_neocores_for_labeling = new ArrayDeque<Point>();

		Queue_of_neocores_for_BFS.add(p);

		
		Set<Point> noise_nearNewCores = new HashSet<Point>();
		
		
		while( !Queue_of_neocores_for_BFS.isEmpty())
		{
			
			Point x = Queue_of_neocores_for_BFS.pop();
			Deque<Point> neighbor = Epoch_Based_find_neighbors(x.p,tick);
			for( Point pp : neighbor)
			{
				if(pp.isCore && pp.mark != UNCLASSIFIED)
				{
					ClustersToMerge.add(pp.mark);
				}
				else if( pp.neighbors_count >= minPts && !pp.isCore){
					Queue_of_neocores_for_labeling.add(pp);
					Queue_of_neocores_for_BFS.push(pp);
					pp.isCore = true;
				}
				else if( pp.mark == NOISE || pp.mark == UNCLASSIFIED)
				{
					noise_nearNewCores.add(pp);
				}
			}
		}
		
		
		
		
		if(ClustersToMerge.size() == 0) // New cluster emerges
		{
			int NEWclusterID = ClusterID;
			ClusterID++;
			for( Point c : Queue_of_neocores_for_labeling)
			{
				c.mark = NEWclusterID;
			}
			for(Point n : noise_nearNewCores)
			{
				
				n.mark = NEWclusterID;
			}
			uf.create(NEWclusterID);
		}
		else if(ClustersToMerge.size() == 1) 			// Cluster expands
		{
			int NEWclusterID = ClustersToMerge.iterator().next();
			for( Point c : Queue_of_neocores_for_labeling)
			{
				c.mark = NEWclusterID;
			}
			for(Point n : noise_nearNewCores)
			{
				
				n.mark = NEWclusterID;
			}
		}
		else 			// Merge clusters into one cluster
		{
			int NEWclusterID = ClustersToMerge.iterator().next();
			for( Integer id : ClustersToMerge)
			{
				uf.union(id, NEWclusterID);
			}
						
			for( Point c : Queue_of_neocores_for_labeling)
			{
				c.mark = NEWclusterID;
			}
			for(Point n : noise_nearNewCores)
			{
				n.mark = NEWclusterID;
			}
			
		}
		
		
	}
	
	
	

	public void find_mimal_bonding_core(Set<Point> minimal_bonding_cores, Point p)
	{
		tick++;
		Deque<Point> neighbors = Epoch_Based_find_neighbors(p.p,tick);
		
		while ( !neighbors.isEmpty()) {
			Point pp = neighbors.pop();
			if( pp.neighbors_count >= minPts && pp.isCore  ){
					minimal_bonding_cores.add(pp);
			}
			else if( pp.neighbors_count < minPts && pp.isCore)
			{			
				Deque<Point> neighbors2 = Epoch_Based_find_neighbors(pp.p,tick);
				pp.isCore = false;
				if( pp.mark != DELETED)
				{
					Potential_Noises.add(pp);
					pp.mark = UNCLASSIFIED;
				}
				
				for(Point ppp : neighbors2)
				{
					if( ppp.neighbors_count >= minPts && ppp.isCore  ){ minimal_bonding_cores.add(ppp); }
					else if( ppp.neighbors_count < minPts && ppp.isCore){ neighbors.add(ppp); 	}
					else if( ppp.neighbors_count < minPts && !ppp.isCore && ppp.mark != UNCLASSIFIED && ppp.mark != DELETED && ppp.mark < cluster_check)
					{
						Potential_Noises.add(ppp);
						ppp.mark = UNCLASSIFIED;
					}
				}			
			}
			else if( pp.neighbors_count < minPts && !pp.isCore && pp.mark != UNCLASSIFIED && pp.mark != DELETED && pp.mark < cluster_check)
			{
					Potential_Noises.add(pp);
					pp.mark = UNCLASSIFIED;
			}
		}
	}
	
	
	
	
	public int BFS(
			int newID, int L, 	
			HashMap<Integer, ArrayDeque<Point>> set_of_Queues, 
			int oid,
			Set<Point> minimum_bonding_cores, HashMap<Integer, Point> id2seed
			 ){
		
		tick++;
		ArrayDeque<Point> neighbors = set_of_Queues.get(uf.find(newID));

		
		ArrayDeque<Point> partialQueue = new ArrayDeque<Point>();
		set_of_Queues.put(uf.find(newID), partialQueue);
				
		
		if (minimum_bonding_cores.size() == 1){
			minimum_bonding_cores.clear();
			uf.union(oid,newID);
			
			return 0;
		}
		
		



		while(!neighbors.isEmpty()){
			
			Point pp =	neighbors.remove();			
			Deque<Point> A = Epoch_Based_find_neighbors(pp.p,tick);
			for(Point ppp : A)
			{
				if (ppp.neighbors_count >= minPts && ppp.isCore && ppp.mark != UNCLASSIFIED && ppp.mark != newID) // CORE
				{
					/* Visit a core point  */
					if(L > ppp.mark ){
						ppp.mark = newID;
						partialQueue.add(ppp);
								
						if (minimum_bonding_cores.remove(ppp)){
							if(minimum_bonding_cores.size() == 1){
								minimum_bonding_cores.clear();//remove(id2seed.get(newID));
								uf.union(newID, oid);
								return -1;
							}
						}
					}
					else if( uf.find(newID) != uf.find(ppp.mark))
					{
						/* Merge two queues */
						partialQueue.add(ppp);
						

//						for( int id : id2seed.keySet())
//						{
//							if( uf.find(ppp.mark) == uf.find(id) ){
//								if( minimum_bonding_cores.remove(id2seed.get(id)) ){
//									break;
//								}
//							}
//						}

						for( Point mbc : minimum_bonding_cores)
						{
							if( uf.find(ppp.mark) == uf.find(mbc.mark) ){
								if( minimum_bonding_cores.remove(mbc) ){
									break;
								}
							}
						}
						

						partialQueue.addAll(set_of_Queues.get(uf.find(ppp.mark)));
						uf.union(newID, ppp.mark);
						set_of_Queues.put(uf.find(newID), partialQueue);
						
						
						if (minimum_bonding_cores.size() == 1){
								minimum_bonding_cores.clear();
								uf.union(oid, newID);
								return 0;
						}
					}
			
				}
				else if (ppp.neighbors_count < minPts && ppp.isCore && ppp.mark != newID) {
					/* An excore outside the minimum bonding cores  
					 * We consider it as core since it will be processed eventually in later. 
					 * */
					partialQueue.add(ppp);
						ppp.mark = newID;				
				}
				else if (ppp.neighbors_count < minPts && ppp.mark < cluster_check) {
					if( ppp.mark != DELETED){
						Potential_Noises.remove(ppp);
						ppp.mark = newID;
					}
				}
			}
		}
		
		if( partialQueue.size() == 0)
		{ 
			minimum_bonding_cores.remove(id2seed.get(newID));
			return 0;
		}
				
		return 0;
	}
	
	
	

	
	public Deque<Point> Epoch_Based_find_neighbors(double[] point, int tick) {
		range_searches++;
		double[] ls = new double[dim];
		double[] us = new double[dim];
		
		for( int d = 0; d < dim; d++)
		{
			ls[d] = point[d] - eps;
		
		}
		for( int d = 0; d < dim; d++)
		{
			us[d] = point[d] + eps;
		}

		MBR box = new MBR(dim);
		box.SetL(ls);
		box.SetU(us);
		Deque<Point> result = new ArrayDeque<Point>();
		
		
		List<Element> le = rtree.Epoch_based_Probe(box,point,eps,tick);
	
		for (Element e : le) {
			Point pp = e.trj;
			result.push(pp);
		}
		
	
		return result;

	}
	
		

	public Deque<Point> neighbor_search(double x[]) {
		range_searches++;
		double[] ls = new double[dim];
		double[] us = new double[dim];

		for(int d = 0 ; d  < dim ; d++ )
		{
			ls[d] = x[d] - eps ;
			us[d] = x[d] + eps ;
		}
		
		MBR box = new MBR(dim);
		box.SetL(ls);
		box.SetU(us);

		

		List<Element> le = rtree.intersects(box);

		
		Deque<Point> result = new ArrayDeque<Point>();

		for (Element e : le) {
			Point pp = e.trj;
			double d = L2Distance(x,pp.p, dim);
			if (d <= eps * eps) {
				result.push(pp);
			}

		}
 
		return result;

	}
	


	public double L2Distance(double[] p1, double[] p2, int dim)
	{
		double dist = 0 ;
		for(int d= 0 ; d < dim ; d++)
		{
			dist += (p1[d] - p2[d])*(p1[d] - p2[d]);
		}
		return dist;
	}


	

	public void perform_DBSCAN() {

		for (Point in : data) {
			
			if (in.mark == UNCLASSIFIED) {

				int count = ExpandCluster(in, ClusterID);
				if (count != 0) {
					Set<Integer> ClusterIdSet = new HashSet<Integer>();
					uf.create(ClusterID);
					ClusterID++;
				}
			}
		}
	}

	public int ExpandCluster(Point p, int ClusterID) {

		int count = 0;
		
		Deque<Point> neighbors =neighbor_search(p.p);
		p.neighbors_count = neighbors.size();

		if (neighbors == null || neighbors.size() < minPts) {
			p.mark = NOISE;
			return count;
		} else {
			p.isCore = true;
			for (Point pp : neighbors) {
				if (pp.mark == UNCLASSIFIED || pp.mark == NOISE) {
					pp.mark = ClusterID;
					count++;
				}
			}
		}

		while (!neighbors.isEmpty()) {
			Point pp = neighbors.pop();
			Deque<Point> neighbors_of_pp = neighbor_search(pp.p);
			pp.neighbors_count = neighbors_of_pp.size();

			if (neighbors_of_pp.size() >= minPts) {
				pp.isCore = true;
				for (Point ppp : neighbors_of_pp) {
					if (ppp.mark == UNCLASSIFIED) {
						ppp.mark = ClusterID;
						count++;
						neighbors.push(ppp);
					} else if (ppp.mark == NOISE) {
						ppp.mark = ClusterID;
						ppp.neighbors_count = neighbor_search(ppp.p).size();
						count++;
					}
				}
			}
		}
		return count;

	}
	
	public void build_Rtree() {
		
		double[] ls = new double[dim];
		double[] us = new double[dim];
		for (Point in : data) {

			Element e = new Element(in);
			
			for( int d = 0 ; d < dim ; d++)
			{
				ls[d] = in.p[d];
				us[d] = in.p[d];
			}
		
			rtree.Insert(ls, us, e);
		}
		
		
	}
	
	public int Validation(List<Point> curWindow){
		System.out.println("RUN VALIDATION...");
		for(Point trj :  curWindow)
		{
			if( trj.mark != -1 ) { 
				trj.mark = uf.find(trj.mark);
			}
		}
		
		for(Point p :  curWindow)
		{
			  if( p.neighbors_count > minPts){ 
				  Deque<Point> neighbors = neighbor_search(p.p);
				  for ( Point q: neighbors )
				  {
					  if( q.neighbors_count > minPts && q.mark != p.mark){
						  System.out.println( q.mark+","+ uf.find( q.mark)+","+ p.mark);

						  System.out.println("BUG");
						  return -1;
					  }
				  }
			  }
		}
		System.out.println("VALIDATION...COMPLETE..");

		return 1;
		
		
	}
	
}