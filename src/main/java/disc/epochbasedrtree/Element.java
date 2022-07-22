package disc.epochbasedrtree;

import disc.Point;
import java.io.Serializable;

public class Element implements Serializable
{

	private static final long serialVersionUID = 1L;
	public Point trj;
	public Element(Point trj)
	{
		this.trj = trj;
	}
	
	public Element()
	{
		
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null) return false;
		else if(!Element.class.isAssignableFrom(obj.getClass())) return false;
		final Element e = (Element) obj;
		return trj.equals(e.trj);
	}
}
