import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 
 * @author K Zhao Yin
 *
 */
public class bfclient {
	
	static final int MAX_PACK_SIZE = 2048;
	
	private int port;
	private String ip;
	// sending period 
	private long timeout;
	// for CLOSE command
	private boolean connected;
	private long lastSent;
	
	private SendingThread sendingThread;
	
	private Timer timer;
	private DatagramSocket sock;
	
	private ConcurrentHashMap<String, Float> dv;
	private ConcurrentHashMap<String, Neighbor> neighbors;
	private ConcurrentHashMap<String, String> next;
	
	
	public bfclient(int port, int to) throws SocketException, UnknownHostException {
		this.port = port;
		this.timeout = (long)to * 1000;
		this.connected = true;
		this.dv = new ConcurrentHashMap<String, Float>();
		this.neighbors = new ConcurrentHashMap<String, Neighbor>();
		this.next = new ConcurrentHashMap<String, String>();
		this.sendingThread = null;
		this.timer = new Timer();
		this.sock = new DatagramSocket(this.port);
		this.ip = InetAddress.getLocalHost().getHostAddress();
	}
	
	
	public void addNeighbor(String id, Neighbor n) {
		this.neighbors.put(id, n);
	}
	
	public void addToDV(String id, float dis) {
		this.dv.put(id, dis);
	}
	
	public void addToNext(String id, String next) {
		this.next.put(id, next);
	}
	
	public int getPort() {
		return this.port;
	}
	
	
	public ConcurrentHashMap<String, Neighbor> getNeighbors() {
		return this.neighbors;
	}
	
	
	public ConcurrentHashMap<String, Float> getDistanceVector() {
		return this.dv;
	}
	
	
	public ConcurrentHashMap<String, String> getNextNode() {
		return this.next;
	}

	
	public void setAddress(String addr) {
		this.ip = addr;
	}
	
	
	public void setSendingThread(SendingThread st) {
		this.sendingThread = st;
	}
	
	public void close() {
		this.connected = false;
	}
	
	public String getAddress() {
		return this.ip + ":" + this.port;
	}
	
	
	public Timer getTimer() {
		return this.timer;
	}
	
	
	public SendingThread getSendingThread() {
		return this.sendingThread;
	}
	
	
	public void run() throws IOException {
//		timer = new Timer();
//		TimerTask regularSend = new RegularSend();
//		timer.schedule(regularSend, timeout, timeout);
		
		while (connected) {
			
			//start listening for incoming message
			byte[] buf = new byte[MAX_PACK_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			sock.receive(packet);
			//a new thread to process the message
			Listener processPack = new Listener(this, packet);
			Thread pt = new Thread(processPack);
			pt.start();
		}
		
		sock.close();
	}
	
	//schedule a TimerTask sends every <timeout>*1000 seconds
	public void scheduleTimer() {
		RegularSend task = new RegularSend(this);
		timer = new Timer();
		timer.schedule(task, timeout, timeout);
	}
    
    //link down initialized by user keyboard input
    public void selfInitLinkDown(String id) {
        if (!neighbors.containsKey(id)) {
            System.out.println("Cannot link down unknow neighbor.");
            return;
        }
        
        neighbors.get(id).disconnect();
        
        this.dv.put(id, Float.POSITIVE_INFINITY);
        this.neighbors.get(id).disconnect();
        this.next.remove(id);
        
        for (String key : this.next.keySet()) {
            if (next.get(key).equals(id)) {
                dv.put(key, Float.POSITIVE_INFINITY);
                next.remove(key);
            }
        }
    }
    
    //link up initialized by user keyboard input
    public void selfInitLinkUp(String id) {
        
        if (!neighbors.containsKey(id)) {
            System.out.println("Unknown neighbor.");
            return;
        }
        
        neighbors.get(id).connect();
        
        if(neighbors.get(id).getDistance() < dv.get(id)) {
            dv.put(id, neighbors.get(id).getDistance());
            next.put(id, id);
        }
    }
	
	
	public static void main(String[] args) throws IOException {
		
		if (args.length < 2) {
			System.out.println("usage: java bfclient <localport> <timeout>"
					+ " [<ipaddress1> <port1> <weight1> ...]");
			System.exit(-1);
		}
		
		//initialize a new client
		int port = Integer.parseInt(args[0]);
		int to = Integer.parseInt(args[1]);
		
		bfclient me = new bfclient(port, to);
		
		//populate client's neighbors, distance vector and next node info
		for (int i = 2; i < args.length; i += 3) {
			if (args.length <= i+2) {
				System.out.println("not enough info for neighbor.");
				break;
			}
			//add neighbors
			String ip = args[i];
			float d1 = Integer.parseInt(args[i + 2]);
			String id = ip + ":" + args[i + 1];
			me.addNeighbor(id, new Neighbor(d1));
			me.addToDV(id, d1);
			me.addToNext(id, id);	
		}
	
		//start sending thread
		//call send vector once on entering the network
		SendingThread sendingThread = new SendingThread(me, me.getAddress());
		me.setSendingThread(sendingThread);
		Thread t = new Thread(me.getSendingThread());
		t.start();
		
		//start keyboard listener thread
		KeyboardListener keyboardThread = new KeyboardListener(me);
		Thread t2 = new Thread(keyboardThread);
		t2.start();
		
		//schedule to call send vector periodically
		me.scheduleTimer();
		
		//start listen socket and keep listening for incoming message until CLOSE
		me.run();
		

	} //end of main class
	
	static class RegularSend extends TimerTask {
		
		private bfclient client;
		
		public RegularSend(bfclient c) {
			this.client = c;
		}
		
		public void run() {
			try {
				client.getSendingThread().sendDistanceVec();
			} catch (IOException e) {
				System.err.println("error sending distance vector.");
				e.printStackTrace();
			}
		}
	}
}

