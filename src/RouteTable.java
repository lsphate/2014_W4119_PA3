import java.util.ArrayList;

/**
 *
 * @author lsphate
 */
public class RouteTable
{
	public ArrayList<RouteInfo> routeTable;

	public RouteTable()
	{
		routeTable = new ArrayList<>();
	}

	public void AddRouteInfoWithoutCheck(RouteInfo RouteInfo)
	{
		routeTable.add(RouteInfo);
	}

	public void ReplaceRouteInfo(RouteInfo RouteInfo)
	{
		for (RouteInfo iter : routeTable)
		{
			if (iter.destination.equals(RouteInfo.destination))
			{
				iter.isValid = false;
			}
		}
		routeTable.add(RouteInfo);
	}

	public void ShowRouteTable()
	{
		for (RouteInfo iter : routeTable)
		{
			if (iter.isValid)
			{
				System.out.println(iter.toString());
			}
		}
	}
}
