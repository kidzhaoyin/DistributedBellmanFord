import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Kid Zhao Yin
 * 
 * Thread to send communications to other clients
 *
 */
public class SendingThread implements Runnable {
	
	private bfclient client;
	private DatagramSocket socket;
	private String address;
	
	public SendingThread(bfclient c, String addr) throws SocketException {
		this.client = c;
		this.socket = new DatagramSocket();
		this.address = addr;
		
	}
	
	public void run() {
		try {
			sendDistanceVec();
			System.out.println("first time dv sent");
		} catch (IOException e) {
			System.err.println("error sending distance vector.");
			e.printStackTrace();
		}
	}
	
	//send distance vector to all neighbors connected
	public void sendDistanceVec() throws IOException {
		ConcurrentHashMap<String, Neighbor> neighbors = client.getNeighbors();
		ConcurrentHashMap<String, Float> dv = client.getDistanceVector();
		//System.out.println("creating packet..");
		Packet pack = new Packet(2, address, dv);
		//System.out.println("created" + pack.getType());
		byte[] buf = pack.toByteArray();
//		String msg = new String(buf);
//		System.out.println("message to sent: " + msg);
		for(String neighbor: neighbors.keySet()) {
			//skip disconnected neighbor
			if (!neighbors.get(neighbor).connect) {
				continue;
			}
			
			String ip = neighbor.split(":")[0];
			int port = Integer.valueOf(neighbor.split(":")[1]);
			DatagramPacket sendPack = 
					new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), port);
			socket.send(sendPack);
		}
	}
	
	public void link(String id, int type) throws IOException {
		if (!client.getNeighbors().containsKey(id)) {
			System.out.println("Unknown neighbor.");
			return;
		}
		Packet pack = new Packet(type, address, null);
		byte[] buf = pack.toByteArray();
		String ip = id.split(":")[0];
		int port = Integer.valueOf(id.split(":")[1]);
		DatagramPacket sendPack = 
				new DatagramPacket(buf, buf.length, InetAddress.getByName(ip), port);
		socket.send(sendPack);
	}
	
}
