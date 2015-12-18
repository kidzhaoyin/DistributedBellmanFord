/**
 * 
 * @author K Zhao Yin
 *
 */
public class Neighbor {

	boolean connect;
	float distance;
	
	
	public Neighbor(float dis) {
		this.distance = dis;
		this.connect = true;
	}
	
	
	public int disconnect() {
		this.connect = false;
		return this.connect == false ? 1 : 0;
	}
	
	
	public int connect() {
		this.connect = true;
		return this.connect == true ? 1 : 0;
	}
	
	
	public boolean isConnected() {
		return connect;
	}
	
	
	public float getDistance() {
		return distance;
	}
	
}