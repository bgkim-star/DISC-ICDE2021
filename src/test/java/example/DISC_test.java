package example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import disc.DISC;
import disc.DBSCAN_options;
import disc.Point;


public class DISC_test {

	String path;
	BufferedReader br = null;
	int dim;
	int window;
	int stride;
	int slide;
	int option;
	int pminpts;
	double peps;
	int iter = 0;

	public void indented_println(String str){
		System.out.println("\t"+str);
	}


	public  void run(String[] args) {

	path = args[0];	// path to dataset forlder (sample_dataset)
	option = Integer.parseInt(args[1]); // select datase:  1. DTG, 2.GeoLife, 3.IRIS, 4.Household
	int pminpts = Integer.parseInt(args[2]); // density threshold: minPts
	double peps = Double.parseDouble(args[3]); // distance threshold: epsilon
	window =  Integer.parseInt(args[4]); // window size
	stride =  Integer.parseInt(args[5]);  // stride size
	slide = Integer.parseInt(args[6]); // #slide

	indented_println("[Example.DISC] Run DISC...");
	indented_println("[Example.DISC] Read the dataset...");
	indented_println("[Example.DISC] Load the dataset...");

	List<Point> dataset =  load_data(option);


	DBSCAN_options ops = new DBSCAN_options(peps,pminpts);

		
	indented_println("[Example.DISC] Fill the window...");
	for(Point p : dataset){ p.init(); }

	DISC disc = new DISC(dataset.subList(0, window),ops, dim);
	disc.build_Rtree();
	disc.perform_DBSCAN();

		
	indented_println("[Example.DISC] Update the strides...");
		
	for( int i = 0 ; i < stride*slide ; i+=stride)
	{
		List<Point> out = dataset.subList(i,i+stride);
		List<Point> in = dataset.subList(window+i,window+i+stride);
		disc.collect(in, out);
		disc.cluster();
	}

	indented_println("[Example.DISC] Complete!");

	int resDenForest[]  = disc.labelAndReturn(dataset.subList(stride*slide,window+stride*slide)); // DISC's clustering result;


	}
	
	private String getData() {
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

	private  List<Point> load_data(int option){

		try {
			List<Point> dataset = new ArrayList<Point>();


			if( option == 1){ /* DTG */
				dim = 2;
				br = new BufferedReader(new FileReader(path+"/DTG_sample.csv"));

				indented_println("[Example.DISC]->[DTG dataset]");

				for (int i = 0; i < window+stride*slide;) {
					String result;
					if ((result = getData()) != null) {

						double x[] = new double[dim] ;

						String[] splited = result.split(",");

						x[0] = Double.parseDouble(splited[0]);
						x[1] = Double.parseDouble(splited[1]);

						Point trj = new Point((i), x);  // id, location, time;
						dataset.add(trj);
						i++;
					}
				}
			}
			else if( option == 2){ /* GeoLife */
				dim = 3;
				FileReader file = new FileReader(path+"/GeoLife_sample.csv");
				br = new BufferedReader(file);

				indented_println("[Example.DISC]->[Geolife dataset]");



				for (int i = 0; i < window+stride*slide;) {
					String result;
					if ((result = getData()) != null) {

						double x[] = new double[dim] ;

						String[] splited = result.split(",");
						x[0] = Double.parseDouble(splited[0]);
						x[1] = Double.parseDouble(splited[1]);
						x[2] = Double.parseDouble(splited[2])/300000;

						Point trj = new Point((i), x);
						dataset.add(trj);
						i++;
					}
				}
			}
			else if( option == 3){ /* IRIS */
				dim = 4;
				br = new BufferedReader(new FileReader(path+"/IRIS_sample.csv"));

				indented_println("[Example.DISC]->[IRIS dataset]");


				for (int i = 0; i < window+stride*slide;) {
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
								Point trj = new Point((i), x);
								dataset.add(trj);
								i++;
							}
						}
					}
				}
			}
			else if( option == 4){ /* Household */
				dim = 7;
				br = new BufferedReader(new FileReader(path+"/Household_sample.csv"));

				indented_println("[Example.DISC]->[Household dataset]");
				for (int i = 0; i < window+stride*slide;) {
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

						Point trj = new Point((i), x);
						dataset.add(trj);
						i++;
					}
				}
			}

			return dataset;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}

