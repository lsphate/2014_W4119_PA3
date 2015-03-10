import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author lsphate
 */
public class LinkInfo implements Serializable
{
	public ConcurrentHashMap<String, Double> linkMap;
	public String owner;
	
	public HashMap<String, Boolean> fakeNeighbor;
	public HashMap<String, Boolean> brokenLink;
	
	public LinkInfo(ArrayList<Linkage> Links, String Owner)
	{
		linkMap = new ConcurrentHashMap<>();
		fakeNeighbor = new HashMap<>();
		brokenLink = new HashMap<>();
		for (Linkage iter : Links)
		{
			linkMap.put(iter.toString(), iter.weight);
		}
		owner = Owner;
	}
}
