import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author lsphate
 */
public class DistanceVector
{
	public boolean cmdHost = false;
	public String owner;
	public ArrayList<Linkage> links;

	public ArrayList<String> routersList;
	public ConcurrentHashMap<String, LinkInfo> linkInfoTable;
	public ConcurrentHashMap<String, Double> distanceVector;

	public RouteTable routeTable;
//	public HashMap<String, Boolean> brokenLink;

	public DistanceVector(String Owner, ArrayList<Linkage> Links)
	{
		owner = Owner;
		links = Links;

		routersList = new ArrayList<>();
		linkInfoTable = new ConcurrentHashMap<>();
		distanceVector = new ConcurrentHashMap<>();

		routeTable = new RouteTable();
//		brokenLink = new HashMap<>();
	}

	public void CreateDistanceVector(LinkInfo LocalLinks)
	{
		for (ConcurrentHashMap.Entry<String, Double> iter : LocalLinks.linkMap.entrySet())
		{
			routersList.add(iter.getKey());
			linkInfoTable.put(owner, LocalLinks);
			distanceVector.put(iter.getKey(), iter.getValue());

			if (iter.getKey().equals(owner) == false)
			{
				routeTable.AddRouteInfoWithoutCheck(new RouteInfo(iter.getKey(), iter.getValue(), iter.getKey(), true));
			}
			else
			{
				routeTable.AddRouteInfoWithoutCheck(new RouteInfo(iter.getKey(), iter.getValue(), iter.getKey(), false));
			}
		}
	}

	public void UpdateDistanceVector(LinkInfo LocalLinks, LinkInfo IncomingLinks)
	{
		//System.out.println("ListenThread: Process update from: " + IncomingLinks.owner);
		linkInfoTable.put(IncomingLinks.owner, IncomingLinks);

		/* Check broken links*/
		if (IncomingLinks.linkMap.get(LocalLinks.owner) == Double.POSITIVE_INFINITY
				&& LocalLinks.linkMap.get(IncomingLinks.owner) != Double.POSITIVE_INFINITY)
		{
			for (Linkage iter : links)
			{
				if (iter.toString().equals(IncomingLinks.owner))
				{
					iter.isAlive = false;
					iter.stopExchange = true;
				}
			}
			LocalLinks.brokenLink.put(IncomingLinks.owner, Boolean.TRUE);
			linkInfoTable.get(owner).linkMap.put(IncomingLinks.owner, Double.POSITIVE_INFINITY);
		}
		
		/* Linkage restore */
		if (IncomingLinks.brokenLink.containsKey(owner) == false
				&& LocalLinks.brokenLink.containsKey(IncomingLinks.owner)
				&& cmdHost == false)
		{
			LocalLinks.brokenLink.remove(IncomingLinks.owner);
			linkInfoTable.get(owner).linkMap.put(IncomingLinks.owner, IncomingLinks.linkMap.get(owner));
			System.out.println("restore");
		}

		/* Learning new routers */
		for (ConcurrentHashMap.Entry<String, Double> iter : IncomingLinks.linkMap.entrySet())
		{
			if (LocalLinks.linkMap.containsKey(iter.getKey()) == false
					&& LocalLinks.fakeNeighbor.containsKey(iter.getKey()) == false)
			{
				LocalLinks.fakeNeighbor.put(iter.getKey(), Boolean.TRUE);
				routersList.add(iter.getKey());
				distanceVector.put(iter.getKey(), Double.POSITIVE_INFINITY);
				//System.out.println("ListenThread: Knowing " + iter.getKey() + ":" + Double.POSITIVE_INFINITY);
			}
		}

		/* Calculate distance vector */
		for (String dst : routersList)
		{
			//System.out.println("Trace to " + dst);

			/* Skip itself. */
			if (dst.equals(owner))
			{
				continue;
			}

			double currentCost = Double.POSITIVE_INFINITY;
			String next = "";

			/* If direct link exists, use direct link first. */
			if (linkInfoTable.get(owner).linkMap.containsKey(dst) && LocalLinks.fakeNeighbor.containsKey(dst) == false)
			{
				double directCost = linkInfoTable.get(owner).linkMap.get(dst);
				if (directCost < currentCost)
				{
					currentCost = directCost;
					next = dst;
				}
			}

			/* Try to find shorter path. */
			for (ConcurrentHashMap.Entry<String, LinkInfo> via : linkInfoTable.entrySet())
			{
				if (via.getValue().linkMap.containsKey(dst) == false)
				{
					continue;
				}
				if (LocalLinks.brokenLink.containsKey(dst) && linkInfoTable.get(dst).fakeNeighbor.containsKey(via.getKey()))
				{
					continue;
				}
				double viaCost = linkInfoTable.get(owner).linkMap.get(via.getKey()) + via.getValue().linkMap.get(dst);
				//System.out.println("via = " + via.getKey() + ", " + viaCost);
				if (viaCost < currentCost)
				{
					if (via.getKey().equals(owner) == false)
					{
						currentCost = viaCost;
						next = via.getKey();
					}

				}
			}

			if (LocalLinks.fakeNeighbor.containsKey(dst))
			{
				LocalLinks.linkMap.put(dst, currentCost);
			}

			if (LocalLinks.brokenLink.containsKey(dst))
			{
				LocalLinks.linkMap.put(dst, currentCost);
			}

			if (distanceVector.get(dst) != currentCost)
			{
				distanceVector.put(dst, currentCost);
				routeTable.ReplaceRouteInfo(new RouteInfo(dst, currentCost, next, true));
				System.out.println("ListenThread: Update " + dst + ":" + currentCost + ", " + next);
			}

		}

//		/* Step 1. Check if there's broken link */
//		if (IncomingLinks.linkMap.get(LocalLinks.owner) == Double.POSITIVE_INFINITY
//				&& LocalLinks.linkMap.get(IncomingLinks.owner) != Double.POSITIVE_INFINITY/*&& isTrustingIncoming*/)
//		{
//			for (Linkage iter : links)
//			{
//				if (iter.toString().equals(IncomingLinks.owner))
//				{
//					iter.isAlive = false;
//					iter.stopExchange = true;
//				}
//			}
//			LocalLinks.linkMap.replace(IncomingLinks.owner, Double.POSITIVE_INFINITY);
//			System.out.println("ListenThread: Broken linkage detected.");
//		}
//
////		if (IncomingLinks.linkMap.get(LocalLinks.owner) != Double.POSITIVE_INFINITY
////				&& LocalLinks.linkMap.get(IncomingLinks.owner) == Double.POSITIVE_INFINITY/*&& isTrustingIncoming*/)
////		{
////			for (Linkage iter : links)
////			{
////				if (iter.toString().equals(IncomingLinks.owner))
////				{
////					iter.isAlive = true;
////					iter.stopExchange = false;
////				}
////			}
////			LocalLinks.linkMap.replace(IncomingLinks.owner, IncomingLinks.linkMap.get(LocalLinks.owner));
////			System.out.println("ListenThread: Restore a linkage.");
////		}
//
//		/* Step 2. Check if there's any new router */
//		for (Map.Entry<String, Double> iter : IncomingLinks.linkMap.entrySet())
//		{
//			if (LocalLinks.linkMap.containsKey(iter.getKey()) == false)
//			{
//				routersList.add(iter.getKey());
//				LocalLinks.linkMap.put(iter.getKey(), Double.POSITIVE_INFINITY);
//				distanceVector.put(iter.getKey(), Double.POSITIVE_INFINITY);
//				routeTable.AddRouteInfoWithoutCheck(new RouteInfo(iter.getKey(), Double.POSITIVE_INFINITY, IncomingLinks.owner, true));
//				notNeighbor.put(iter.getKey(), Boolean.TRUE);
//				System.out.println("ListenThread: Add Linkage " + iter.getKey() + ":" + Double.POSITIVE_INFINITY);
//			}
//		}
//
//		/* Step 3. Calculate Distance Vector */
//		for (String routerName : routersList/*Map.Entry<String, Double> iter : IncomingLinks.linkMap.entrySet()*/)
//		{
//			//String routerName = iter.getKey();
////			if (/*LocalLinks.linkMap.containsKey(routerName) == false || */IncomingLinks.linkMap.containsKey(routerName) == false)
////			{
////				continue;
////			}
//
//			if (distanceVector.get(routerName) > (LocalLinks.linkMap.get(IncomingLinks.owner) + IncomingLinks.linkMap.get(routerName)))
//			{
//				distanceVector.replace(routerName, (LocalLinks.linkMap.get(IncomingLinks.owner) + IncomingLinks.linkMap.get(routerName)));
//				routeTable.ReplaceRouteInfo(new RouteInfo(routerName, (LocalLinks.linkMap.get(IncomingLinks.owner) + IncomingLinks.linkMap.get(routerName)), IncomingLinks.owner, true));
//				//System.out.println(IncomingLinks.owner);
//
//				if (notNeighbor.containsKey(routerName))
//				{
//					LocalLinks.linkMap.put(routerName, (LocalLinks.linkMap.get(IncomingLinks.owner) + IncomingLinks.linkMap.get(routerName)));
//				}
//				System.out.println("ListenThread: Local route table updated. Add " + routerName + " " + (LocalLinks.linkMap.get(IncomingLinks.owner) + IncomingLinks.linkMap.get(routerName)));
//			}
//
//			if (LocalLinks.linkMap.get(routerName) == Double.POSITIVE_INFINITY/* && notNeighbor.containsKey(routerName) == false*/)
//			{
//				if (notNeighbor.containsKey(routerName) == false)
//				{
//					if (distanceVector.get(routerName) > (IncomingLinks.linkMap.get(owner) + IncomingLinks.linkMap.get(routerName)) /*&& IncomingLinks.linkMap.get(routerName).equals(Double.POSITIVE_INFINITY) == false*/)
//					{
//						distanceVector.replace(routerName, (IncomingLinks.linkMap.get(owner) + IncomingLinks.linkMap.get(routerName)));
//						routeTable.ReplaceRouteInfo(new RouteInfo(routerName, (IncomingLinks.linkMap.get(owner) + IncomingLinks.linkMap.get(routerName)), IncomingLinks.owner, true));
//						routeTable.ReplaceRouteInfo(new RouteInfo(routerName, (IncomingLinks.linkMap.get(owner) + IncomingLinks.linkMap.get(routerName)), routerName, true));
//						//System.out.println(IncomingLinks.owner);
//						System.out.println("ListenThread: Local route table re-routed Add " + routerName + " " + (IncomingLinks.linkMap.get(owner) + IncomingLinks.linkMap.get(routerName)));
//
//					}
//				}
//				else
//				{
//
//				}
//
//			}
//		}
//
//		if (IncomingLinks.linkMap.containsValue(Double.POSITIVE_INFINITY))
//		{
//
//		}
//		//System.out.println("ListenThread: Update completed.");
	}
}
