/**
 *
 * @author lsphate
 */
public class RouteInfo
{
	public String destination;
	public double cost;
	public String nextHop;
	public boolean isValid;

	public RouteInfo(String Destination, double Cost, String NextHop, boolean IsValid)
	{
		this.destination = Destination;
		this.cost = Cost;
		this.nextHop = NextHop;
		this.isValid = IsValid;
	}
	
	@Override
	public String toString()
	{
		return "Destination = " + destination + ", Cost = " + cost + ", (Link = " + nextHop + ")";
	}
}
