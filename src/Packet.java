import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 
 * @author Yin Zhao
 *
 *Protocal structure
 *
 *Packet:
 *| length 4 byte | type 4 byte | sender 20 byte | distance vector 24*n byte|
 *
 */
public class Packet {
	//ip:port
	private static final int ID_SIZE = 20;
	
	//length {not update: 28, update: 24*n + 28}
	private int length; //integer 4 byte
	//type {0: link down, 1: link up, 2: update}
	private int type; // integer. 4 byte
	private String sender; // String 20 byte
	private ConcurrentHashMap<String, Float> dv; // 24*n byte
	
	//construct Packet for sending
	public Packet(int t, String s, ConcurrentHashMap<String, Float> vector) {
		this.length = t == 2 ? 24 * vector.size() + 8 + ID_SIZE : 8 + ID_SIZE;
		this.type = t;
		this.sender = s;
		this.dv = vector;
		//System.out.println("packet constructed with vector length "+vector.size());
	}
	
	//construct Packet from incoming message
	public Packet(byte[] message) {
		ByteBuffer bb = ByteBuffer.wrap(message);
		this.length = bb.getInt(0);
		this.type = bb.getInt(4);
		this.sender = new String(Arrays.copyOfRange(message, 8, 8 + ID_SIZE));
		//System.out.println("received type "+type);
		if (type != 2) {
			this.dv = null;
		}
		
		else {
			this.dv = new ConcurrentHashMap<String, Float>();
			int position = 8 + ID_SIZE;
			while (position < length) {
//				bb.position(position);
//				bb.limit(position + ID_SIZE);
				String addr = new String(Arrays.copyOfRange(message, position, position + ID_SIZE));
				bb.position(position + ID_SIZE);
				float distance = bb.getFloat();
				dv.put(addr.trim(), distance);
				position += (ID_SIZE + 4);
				//System.out.println("dv address: " + addr);
				//System.out.println("dv distance: " + distance);
			}
			
		}
		
	}
	
	//write info to byte array to be sent
	public byte[] toByteArray() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(intToFourByte(length));
		out.write(intToFourByte(type));
		byte[] senderAddr = ByteBuffer.allocate(ID_SIZE).put(sender.getBytes()).array();
		out.write(senderAddr);
		if (dv != null)
			out.write(distanceVecToByteArray(dv));
		return out.toByteArray();
	}
	
	public byte[] distanceVecToByteArray(ConcurrentHashMap<String, Float> vector) {

		byte[] result = new byte[vector.size() * 24];
		int position = 0;

		for(String key : vector.keySet()) {
			
			byte[] id = ByteBuffer.allocate(ID_SIZE).put(key.getBytes()).array();
			System.arraycopy(id, 0, result, position, ID_SIZE);
			byte[] dis = floatToByteArray(vector.get(key));
			System.arraycopy(dis, 0, result, position + ID_SIZE, 4);
			position += (ID_SIZE + 4);
		}
		return result;
	}
	
	public int getType() {
		return this.type;
	}
	
	public String getSenderID() {
		return this.sender.trim();
	}
	
	public ConcurrentHashMap<String, Float> getDistanceVector() {
		return this.dv;
	}
	
	//convert an integer to 4 byte array
	public static byte[] intToFourByte(int value) {
		return new byte[] {
				(byte)(value >>> 24),
				(byte)(value >>> 16),
				(byte)(value >>> 8),
				(byte)value
		};
	}
	
	public static byte[] floatToByteArray(float value) {
		return ByteBuffer.allocate(4).putFloat(value).array();
	}

}
