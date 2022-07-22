package disc;


public class DBSCAN_options {
	
		public double epsilon = 1f;
		public int minPts = 2;
		public DBSCAN_options(double param_eps, int param_minPts)
		{
			epsilon = param_eps;
			minPts = param_minPts;
		}
		
		public  double get_eps()
		{
			return epsilon;
		}
		
		public  int get_minPts()
		{
			return minPts;
		}

		
}
