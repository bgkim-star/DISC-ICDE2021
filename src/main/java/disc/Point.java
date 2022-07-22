package disc;
import java.io.Serializable;

public class Point implements Serializable {
	  /**
	 * 
	 */
	private static final long serialVersionUID = -6159109518742050573L;
	public int id;
	public int mark = 0;
	public double[] p;
	public boolean isCore = false; 
	public int neighbors_count = 0;
	  
	public Point(int id, double p1, double p2, double p3, String t)
	{
		this.id = id;
	}

	public Point(int id, double[] p_array)
	{
		this.id = id;
		p = p_array;
	}

	public void init(){
		this.mark = 0;
		this.isCore = false;
		this.neighbors_count=0;
	}
  
	public String toString() {
		return id+","+mark+","+ new Double(p[0]).toString()+","+ new Double(p[1]).toString(); //+","+ new Double(alt).toString();
	}

}