public class Linkage
{
	public String routerIP;
	public int port;
	public double weight;
	public boolean isSelf = false;
	public boolean isAlive = true;
	public boolean stopExchange = false;

	public Linkage(String RouterIP, int Port, double Weight)
	{
		routerIP = RouterIP;
		port = Port;
		weight = Weight;
	}
	
	public Linkage(String RouterIP, int Port, double Weight, boolean IsSelf)
	{
		routerIP = RouterIP;
		port = Port;
		weight = Weight;
		isSelf = IsSelf;
	}

	@Override
	public String toString()
	{
		return routerIP + ":" + Integer.toString(port);
	}
}
