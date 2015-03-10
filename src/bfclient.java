import java.util.*;
import java.net.*;
import java.io.*;

/**
 *
 * @author lsphate
 */
public class bfclient
{
	public final static String M_LINKDOWN = "LINKDOWN";
	public final static String M_LINKUP = "LINKUP";
	public final static String M_SHOWRT = "SHOWRT";
	public final static String M_CLOSE = "CLOSE";

	public static int localPort = 0;
	public static int timeout = 0;

	/* Local Clinets Link History */
	public static ArrayList<Linkage> originalLinks = new ArrayList<>();
	public static LinkInfo linkInfo;
	public static DistanceVector localDV;

	static ListenThread ltrd;
	static BroadcastThraed btrd;

	public static void main(String[] args)
	{
		if (args.length < 5)
		{
			System.out.println("Invalid arguments.");
		}

		if (args.length >= 5)
		{
			localPort = Integer.parseInt(args[0]);
			timeout = Integer.parseInt(args[1]);
			try
			{
				originalLinks.add(new Linkage(InetAddress.getLocalHost().getHostAddress(), localPort, 0.0, true));
			}
			catch (UnknownHostException ex)
			{
			}
			if (args.length >= 5 && (args.length - 2) % 3 == 0)
			{
				int sets = (args.length - 2) / 3;
				for (int i = 0; i < sets; i++)
				{
					try
					{
						originalLinks.add(new Linkage(InetAddress.getByName(args[(i * 3) + 2]).getHostAddress(),
								Integer.parseInt(args[(i * 3) + 3]),
								Double.parseDouble(args[(i * 3) + 4])));
					}
					catch (UnknownHostException ex)
					{
						System.out.println("Linkage IP invalid.");
						System.exit(0);
					}
				}
			}
		}

		/* Set local link information.*/
		String localhost = "";
		try
		{
			localhost = InetAddress.getLocalHost().getHostAddress() + ":" + args[0];
		}
		catch (UnknownHostException ex)
		{
		}
		linkInfo = new LinkInfo(originalLinks, localhost);

		/* Create the first distance vector & route table*/
		localDV = new DistanceVector(localhost, originalLinks);
		localDV.CreateDistanceVector(linkInfo);

//		for (Map.Entry<String, Double> iter : linkInfo.linkMap.entrySet())
//		{
//			System.out.println(iter.getKey() + " " + iter.getValue());
//		}

		/* Start Listen Thread */
		ltrd = new ListenThread(localPort, linkInfo, originalLinks, localDV);
		Thread listenThread = new Thread(ltrd);
		listenThread.start();

		/* Start Broadcast Thread */
		btrd = new BroadcastThraed(timeout, linkInfo, originalLinks);
		//btrd.UpdataBroadcastLinkInfo(linkInfo);
		Thread broadcastThread = new Thread(btrd);
		broadcastThread.start();

		/* Start UI */
		bfclient bfc = new bfclient();
		bfc.StartConsole();
	}

	public bfclient()
	{
	}

	/* Wait for user to key-in commands. */
	public void StartConsole()
	{
		Scanner sc = new Scanner(System.in);
		System.out.println("Welcome to bfclient!");

		while (true)
		{
			String userInput = sc.nextLine().trim();
			String[] tokens = userInput.split(" ");
			if (tokens.length == 0 || tokens[0].length() == 0)
			{
				continue;
			}
			String cmd = tokens[0].toUpperCase();

			switch (cmd)
			{
				case M_LINKDOWN:
					DoLinkDown(tokens);
					break;
				case M_LINKUP:
					DoLinkUp(tokens);
					break;
				case M_SHOWRT:
					DoShowRT(tokens);
					break;
				case M_CLOSE:
					DoClose();
					break;
				default:
					System.out.println("Unknown command: " + userInput);
					break;
			}
		}
	}

	void DoLinkDown(String[] tokens)
	{
		System.out.println("Invoke LINKDOWN");
		if (tokens.length != 3)
		{
			System.out.println("Command format error.");
			System.out.println("Usage: LINKDOWN <ip> <port>");
			return;
		}

		linkInfo.linkMap.replace(tokens[1] + ":" + tokens[2], Double.POSITIVE_INFINITY);
		linkInfo.brokenLink.put(tokens[1] + ":" + tokens[2], Boolean.TRUE);
		localDV.cmdHost = true;
		for (Linkage iter : originalLinks)
		{
			if (iter.toString().equals(tokens[1] + ":" + tokens[2]))
			{
				/* Prepare to stop exchange */
				iter.isAlive = false;
				break;
			}
		}
	}

	void DoLinkUp(String[] tokens)
	{
		System.out.println("Invoke LINKUP");
		if (tokens.length != 3)
		{
			System.out.println("Command format error.");
			System.out.println("Usage: LINKUP <ip> <port>");
			return;
		}
		for (Linkage iter : originalLinks)
		{
			if (iter.toString().equals(tokens[1] + ":" + tokens[2]))
			{
				iter.isAlive = true;
				iter.stopExchange = false;
				linkInfo.linkMap.replace(tokens[1] + ":" + tokens[2], iter.weight);
				linkInfo.brokenLink.remove(tokens[1] + ":" + tokens[2]);
				localDV.cmdHost = false;
				break;
			}
		}
	}

	void DoShowRT(String[] tokens)
	{
		localDV.routeTable.ShowRouteTable();
	}

	void DoClose()
	{
		System.exit(0);
	}
}

/* Thread for listening to ROUTE UPDATE */
class ListenThread implements Runnable
{
	DatagramSocket listenSkt;
	DatagramPacket incomingPkt;
	byte[] incomingData_byte = new byte[60 * 1024];
	ObjectInputStream ois;
	int listenPort;
	ArrayList<Linkage> links;
	LinkInfo linkInfo;
	DistanceVector localDV;
	int trustCount = 0;

	public ListenThread(int LocalPort, LinkInfo LinkInfo, ArrayList<Linkage> Links, DistanceVector LocalDV)
	{
		//System.out.println("Loading listening thread...");
		listenPort = LocalPort;
		linkInfo = LinkInfo;
		links = Links;
		localDV = LocalDV;
	}

	@Override
	public void run()
	{
		try
		{
			listenSkt = new DatagramSocket(listenPort);

			while (true)
			{
//				if (trustCount > 5)
//				{
//					localDV.isTrustingIncoming = true;
//				}

				incomingPkt = new DatagramPacket(incomingData_byte, incomingData_byte.length);
				listenSkt.receive(incomingPkt);

				if (RUDPPacket.VerifyChecksum(incomingPkt) == false)
				{
					System.out.println("Checksum error - packet discarded.");
				}
				else
				{
					RUDPPacket rudpIn = new RUDPPacket(incomingData_byte);
					ois = new ObjectInputStream(new ByteArrayInputStream(rudpIn.GetReceiveData()));
					LinkInfo incomingLinks = (LinkInfo) ois.readObject();
					//System.out.println("ListenThread: Incoming update from: " + incomingLinks.owner + " accepted.");
					
//					boolean blockCheck = false;
//					for (Linkage iter : links)
//					{
//						if (iter.toString().equals(incomingLinks.owner) && iter.isAlive == false)
//						{
//							System.out.println("block msg from " + incomingLinks.owner);
//							blockCheck = true;
//						}
//					}
//					
//					if (!blockCheck)
//					{
						DoRouteUpdate(incomingLinks);
//					}
					
//					if (localDV.isTrustingIncoming == false)
//					{
//						trustCount++;
//					}
				}
			}
		}
		catch (IOException | ClassNotFoundException e)
		{
		}
	}

	void DoRouteUpdate(LinkInfo IncomingLinks)
	{
		//System.out.println("ListenThread: Update processing...");
		localDV.UpdateDistanceVector(linkInfo, IncomingLinks);
	}
}

/* Thread for broadcasting ROUTE UPDATE */
class BroadcastThraed implements Runnable
{
	int timeout;
	ArrayList<Linkage> neighbors;
	LinkInfo linkInfo;
	DatagramSocket broadcastSkt;
	DatagramPacket broadcastPkt;
	ByteArrayOutputStream outputStream;
	ObjectOutputStream oos;
	byte[] broadcastData_byte = new byte[60 * 1024];

	public BroadcastThraed(int Timeout, LinkInfo LinkInfo, ArrayList<Linkage> Links)
	{
		//System.out.println("Loading broadcasting thread...");
		linkInfo = LinkInfo;
		neighbors = new ArrayList<>();
		timeout = Timeout;
		for (Linkage iter : Links)
		{
			if (iter.isSelf == false)
			{
				neighbors.add(iter);
			}
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				Thread.sleep(timeout * 1000);
			}
			catch (InterruptedException ex)
			{
			}
			try
			{
				broadcastSkt = new DatagramSocket(65535);
			}
			catch (SocketException ex)
			{
				System.out.println("Error: Socket creatation failed.");
			}

			UpdataBroadcastLinkInfo();
			for (Linkage neighbor : neighbors)
			{
				if (neighbor.isAlive)
				{
					try
					{
						broadcastPkt = new DatagramPacket(this.WrapPacket(), this.WrapPacket().length, InetAddress.getByName(neighbor.routerIP), neighbor.port);
						//System.out.println("BroadcastThread: Sending data toward " + neighbor.routerIP + ":" + neighbor.port);
						broadcastSkt.send(broadcastPkt);
					}
					catch (IOException ex)
					{
					}
				}
				else
				{
					if (neighbor.stopExchange == false)
					{
						try
						{
							broadcastPkt = new DatagramPacket(this.WrapPacket(), this.WrapPacket().length, InetAddress.getByName(neighbor.routerIP), neighbor.port);
							//System.out.println("BroadcastThread: Sending data toward " + neighbor.routerIP + ":" + neighbor.port);
							broadcastSkt.send(broadcastPkt);
							neighbor.stopExchange = true;
						}
						catch (IOException ex)
						{
						}
					}
				}

			}
			broadcastSkt.close();
		}
	}

	public void UpdataBroadcastLinkInfo()
	{
		try
		{
			outputStream = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(outputStream);
			oos.writeObject(linkInfo);
			broadcastData_byte = outputStream.toByteArray();
			//System.out.println("BroadcastThread: Broadcast data updated.");
		}
		catch (Exception e)
		{
		}
	}

	public byte[] WrapPacket()
	{
		RUDPPacket rudp = new RUDPPacket();
		rudp.SetSendData(broadcastData_byte);
		return rudp.CreateRUDPPacket();
	}
}
