import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import DISC.DISC;
import DISC.DBSCAN_options;
import DISC.DISC;
import DISC.EpochBasedRtree.Point;


public class RunDISC {

	static String path;	
	static BufferedReader br = null;
	static String[] nnames = null;
	static int iter = 0;

	static int dim = 0;
	
	static long elapsed_time_dbscan = 0;
	static long elapsed_time_extraN = 0;
	static long elapsed_time_disc = 0;
	static long elapsed_time_indbscan = 0;
	static long elapsed_time_indbscan_in = 0;
	static long elapsed_time_indbscan_out = 0;
	
	static long elapsed_time_disc_in = 0;
	static long elapsed_time_disc_out = 0;
	
	
	static long mem_dbscan = 0;
	static long mem_extraN = 0;
	static long mem_disc = 0;
	static long mem_indbscan = 0;

	static long range_dbscan = 0;
	static long range_extraN = 0;
	static long range_disc = 0;
	static long range_indbscan = 0;
	
			
			
	static long range_searches_indbscan = 0;
	static long range_searches_disc = 0;
	
;
	
	public static void main(String[] args) {
		
	String Path = args[0];
	int option = Integer.parseInt(args[1]);
	int pminpts = Integer.parseInt(args[2]);
	double peps = Double.parseDouble(args[3]);
	int window =  Integer.parseInt(args[4]);
	int stride =  Integer.parseInt(args[5]); 
	int Time = Integer.parseInt(args[6]);	
	String filename = args[7];	

	
	System.out.println("Run DISC...");
	System.out.print("Read the dataset...");
	
	try {
		List<Point> dataset = new ArrayList<Point>();
		
		if( option == 2){ /* GeoLife */ 
		dim = 3; 
		FileReader file = new FileReader(Path+"/GeoLife_sample.csv");
		br = new BufferedReader(file);
		
		System.out.println("[GeoLife]");

		for (int i = 0; i < window+stride*Time;) {
			String result;
			if ((result = getData()) != null) {					
					
					double x[] = new double[dim] ;
					
					String[] splited = result.split(",");
					x[0] = Double.parseDouble(splited[0]);
					x[1] = Double.parseDouble(splited[1]);
					x[2] = Double.parseDouble(splited[2])/300000;

					
					Point trj = new Point(Integer.toString(i), x);
					dataset.add(trj);
					i++;
				}
			}
		}
		else if( option == 3){ /* IRIS */ 
			dim = 4; 
			br = new BufferedReader(new FileReader(Path+"/IRIS_sample.csv"));
			
			System.out.println("[IRIS]");
			for (int i = 0; i < window+stride*Time;) {
				String result;
				if ((result = getData()) != null) {					
					
						double x[] = new double[dim] ;
						String[] splited = result.split(",");
						
						if(splited[2].trim().length() != 0  && splited[3] != "" )
						{
							x[0] = Double.parseDouble(splited[0]);
							x[1] = Double.parseDouble(splited[1]);
							x[2] = Double.parseDouble(splited[2])/10;
							x[3] = Double.parseDouble(splited[3])*10;
							if( x[3] >= 0 ){
								Point trj = new Point(Integer.toString(i), x);
								dataset.add(trj);
								i++;
							}
						}
					}
				}
		}
		else if( option == 4){ /* Household */ 
			dim = 7; 
			br = new BufferedReader(new FileReader(Path+"/Household_sample.csv"));
			
			System.out.println("[Household]");

			for (int i = 0; i < window+stride*Time;) {
				String result;
				if ((result = getData()) != null) {					
					
						double x[] = new double[dim] ;
						String[] splited = result.split(",");
						
						x[0] = Double.parseDouble(splited[0]);
						x[1] = Double.parseDouble(splited[1]);
						x[2] = Double.parseDouble(splited[2]);
						x[3] = Double.parseDouble(splited[3]);
						x[4] = Double.parseDouble(splited[4]);
						x[5] = Double.parseDouble(splited[5]);
						x[6] = Double.parseDouble(splited[6]);
						
						Point trj = new Point(Integer.toString(i), x);
						dataset.add(trj);
						i++;
					}
				}
		}
		else if( option == 1){ /* DTG */ 
			dim = 2; 
			br = new BufferedReader(new FileReader(Path+"/DTG_sample.csv"));
			
			System.out.println("[DTG]");

			for (int i = 0; i < window+stride*Time;) {
				String result;
				if ((result = getData()) != null) {					
					
						double x[] = new double[dim] ;
						
						String[] splited = result.split(",");

						x[0] = Double.parseDouble(splited[0]);
						x[1] = Double.parseDouble(splited[1]);
						
						Point trj = new Point(Integer.toString(i), x);
						dataset.add(trj);
						i++;
					}
				}
		}
		
		DBSCAN_options ops = new DBSCAN_options(peps,pminpts);

		
		System.out.println("Fill the window...");
		for(Point p : dataset){ p.mark = 0;  p.isCore = false; p.neighbors_count=0; }				
		DISC disc = new DISC(dataset.subList(0,window),ops, dim);
		disc.build_Rtree();
		disc.perform_DBSCAN();

		
		System.out.println("Update the strides...");
		
		
		FileWriter fw = new FileWriter(filename);

		for( int i = 0 ; i < stride*Time+0 ; i+=stride)
		{
			List<Point> out = dataset.subList(i,i+stride);
			long dec_start = System.nanoTime();
			disc.COLLECT(null, out);
			disc.CLUSTER();
			long dec_end = System.nanoTime();
					
			List<Point> in = dataset.subList(window+i,window+i+stride);
			long inc_start = System.nanoTime();
			disc.COLLECT(in, null);
			disc.CLUSTER();
			long inc_end = System.nanoTime();
			
			fw.write((inc_end-inc_start)/1000000 +", "+ (dec_end-dec_start)/1000000 + ", " +  ((dec_end-dec_start)+(inc_end-inc_start))/1000000  +"\n");
		}
		
		fw.flush();
		fw.close();
				
		
		System.out.println("Complete!");
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getData() {
		String line = null;
		try {
			line = br.readLine();
			if (line != null && !line.equals("")) {
			}
			else{
				return null;
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		iter++;
		return line;
	}
}

