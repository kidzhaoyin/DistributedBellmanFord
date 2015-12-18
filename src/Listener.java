import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 
 * @author Kid Zhao Yin
 * 
 * Thread to listen to incoming communication from other clients.
 * Communication types: 0 for Link-Down, 1 for Link-UP, 2 for updating distance vector
 *
 */

public class Listener implements Runnable{

	private bfclient client;
	private DatagramPacket dgpacket;

	
	public Listener (bfclient client, DatagramPacket pack) throws SocketException {
		this.client = client;
		this.dgpacket = pack;
	}
	
	
	public void run() {
		
		byte[] message = dgpacket.getData();
		String st = new String(message);
		//System.out.println("message receid: " + st);
		Packet packet = new Packet(message);
		
		ConcurrentHashMap<String, Neighbor> neighbors = client.getNeighbors();
		String id = packet.getSenderID();
		
		//System.out.println("message type: " + packet.getType());
		//System.out.println("message from: " + packet.getSenderID());
		
		//link-down
		if (packet.getType() == 0) {
			if (neighbors.containsKey(id)) {
				neighbors.get(id).disconnect();
				linkDown(id, neighbors);
				try {
					//reset timer after each sending
					client.getSendingThread().sendDistanceVec();
					client.getTimer().cancel();
					client.scheduleTimer();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println(id + " trying to link down unknown client.");
			}
		}
		
		//link-up
		else if (packet.getType() == 1) {
			if (neighbors.containsKey(id) && !neighbors.get(id).isConnected()) {
				neighbors.get(id).connect();
				linkUp(id, neighbors);
				try {
					//reset timer after each sending
					client.getSendingThread().sendDistanceVec();
					client.getTimer().cancel();
					client.scheduleTimer();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println(id + " trying to link up unknown client.");
			}
		}
		
		//update distance vector
		else if (packet.getType() == 2) {
			if(updateDistanceVec(packet, id.trim(), neighbors) == true) {
				try {
					client.getSendingThread().sendDistanceVec();
					client.getTimer().cancel();
					client.scheduleTimer();
				} catch (IOException e) { 
					e.printStackTrace();
				}
			};
		}
		
	}
	
	//args: pack which contains updated info, sender the ip:port key of the neighbor
	//neighbors the client's neighbors
	public boolean updateDistanceVec(Packet pack, String sender, 
			ConcurrentHashMap<String, Neighbor> neighbors) {
	
		//my distance vector
		ConcurrentHashMap<String, Float> vector = client.getDistanceVector();
		//my next nodes to each node
		ConcurrentHashMap<String, String> next = client.getNextNode();
		//sender neighbor's distance vector
		ConcurrentHashMap<String, Float> othVector = pack.getDistanceVector();
		//if update == true, notify neighbors
		boolean update = false;
		
		//System.out.println("this client: " + client.getAddress());
		
//		for (String key : othVector.keySet()) {
//		System.out.println("neighbors vec: " + key);
//		}
		
		//System.out.println("dis: " + othVector.get(client.getAddress()));
		
		//first time connect. client add oth to its neighbors list
		if (!neighbors.containsKey(sender)) {
			client.addNeighbor(sender, new Neighbor(othVector.get(client.getAddress())) );
			//neighbors.put(sender, new Neighbor(othVector.get(client.getAddress())));
			if (!vector.containsKey(sender) || vector.get(sender) > othVector.get(client.getAddress())) {
				vector.put(sender, othVector.get(client.getAddress()));
				client.addToNext(sender, sender);
				update = true;
			}
		}
		
		//check all known nodes
		for (String node : vector.keySet()) {
			if (next.containsKey(node)) {
				String nextNodeAddr = next.get(node);
				//I reach 'node' through the sender neighbor, thus need to change value
				if(nextNodeAddr.equals(sender) && !sender.equals(node)) {
					//link broke. remove all path involving it
//					if(!othVector.containsKey(node) && vector.get(node)!=Float.POSITIVE_INFINITY) {
//						vector.put(node, Float.POSITIVE_INFINITY);
//						for(String s : next.keySet()) {
//							if(next.get(s).equals(node)) {
//								next.remove(s);
//							}
//						}
//					}
					//oth's distance to node is inf, meaning link broke, remove all paths from me through node
					if (othVector.get(node) == Float.POSITIVE_INFINITY && vector.get(node) != Float.POSITIVE_INFINITY) {
						vector.put(node, Float.POSITIVE_INFINITY);
						for (String s : next.keySet()) {
							if (next.get(s).equals(node)) {
								next.remove(s);
							}
						}
						update = true;
					}
					
					if(neighbors.get(sender) != null && othVector.containsKey(node)){
						float newDis = neighbors.get(sender).getDistance() + othVector.get(node);
						if (newDis != vector.get(node)) {
							vector.put(node, newDis);
							update = true;
						}	
					}		
				}
			}
		}
		
		//check all nodes that the sender knows
		for (String receivedNode : othVector.keySet()) {
			
			if (receivedNode.equals(client.getAddress())) {
				if (vector.containsKey(sender)) {
					float othDis = neighbors.get(sender).getDistance();
					if (othDis < vector.get(sender)) {
						vector.put(sender, othDis);
						next.put(sender, sender);
						update = true;
					}
				}
				//I didn't know the sender
				else {
					//receivedNode == my address
					vector.put(sender, othVector.get(receivedNode));
					next.put(sender, sender);
					update = true;
				}
				
				continue;
			}
			//unknown to the sender
			if (othVector.get(receivedNode) == Float.POSITIVE_INFINITY) {
				continue;
			}
			
			//for other nodes except me in sender's dv
			float newDis = neighbors.get(sender).getDistance() + othVector.get(receivedNode);
			//previously unknown node or with a shorter path
			if(!vector.containsKey(receivedNode) || newDis < vector.get(receivedNode)) {
					vector.put(receivedNode, newDis);
					next.put(receivedNode, sender);
					update = true;
			}	
		}
		return update;	
	} //end of method update
	
	//if this method is called, already checked that the mentioned is currently a connected neighbor
	//therefore guaranteed to update the distance vector
	public void linkDown(String id, ConcurrentHashMap<String, Neighbor> neighbors) {
		ConcurrentHashMap<String, Float> vector = client.getDistanceVector();
		ConcurrentHashMap<String, String> next = client.getNextNode();
//		
		//set the dis to mentioned neighbor to infinity
		vector.put(id, Float.POSITIVE_INFINITY);
		next.remove(id);
		//set the dis to the nodes with mentioned neighbor as the first hop to infinity
		for (String key : next.keySet()) {
			if (next.get(key).equals(id)) {
				vector.put(key, Float.POSITIVE_INFINITY);
				next.remove(key);
			}
		}
		
		
//		for(String n : neighbors.keySet()) {
//			if (neighbors.get(n).isConnected() && !vector.containsKey(n)) {
//				vector.put(n, neighbors.get(n).getDistance());
//				next.put(n, n);
//			}
//		}
		
	}
	
	//if this method is called, already checked that the mentioned is a disconnected neighbor
	public void linkUp(String id, ConcurrentHashMap<String, Neighbor> neighbors) {
		ConcurrentHashMap<String, Float> vector = client.getDistanceVector();
		ConcurrentHashMap<String, String> next = client.getNextNode();
		
		//resume the previous distance
		if (neighbors.get(id).getDistance() < vector.get(id)) {
			vector.put(id, neighbors.get(id).getDistance());
			next.put(id, id);
		}
		//not maintaining oth's dv thus cannot resume all oth paths. leave to others to figure out
//		for (String key : vector.keySet()) {
//			if (vector.get(key))
//		}

		
	}
	

}//end of class
