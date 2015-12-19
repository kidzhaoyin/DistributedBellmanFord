import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 
 * @author Kid Zhao Yin
 *
 */

public class KeyboardListener implements Runnable {

	private Scanner in;
	private bfclient client;
	
	public KeyboardListener(bfclient c) {
		this.in = new Scanner(System.in);
		this.client = c;
	}
	
	public void run() {
		ConcurrentHashMap<String, Float> dv = client.getDistanceVector();
		ConcurrentHashMap<String, String> next = client.getNextNode();
        SendingThread sendingThread = client.getSendingThread();
        
		while(true) {
			String input = in.nextLine();
			String[] arr = input.split("\\s+");
			// linkdown command
			if (arr[0].equalsIgnoreCase("LINKDOWN")) {
				if (arr.length < 3) {
					System.err.print("Not enough information about the client.");
					continue;
				}
				try {
					String id = arr[1] + ":" + arr[2];
                    client.selfInitLinkDown(id);
                    sendingThread.link(id, 0);
                    sendingThread.sendDistanceVec();

				} catch (IOException e) {
					System.err.println("error sending message.");
					e.printStackTrace();
				}
			}
			
			//linkup command
			else if (arr[0].equalsIgnoreCase("LINKUP")) {
				if (arr.length < 3) {
					System.err.print("Not enough information about the client.");
					continue;
				}
				try {
                    String id = arr[1] + ":" + arr[2];
                    client.selfInitLinkUp(id);
                    sendingThread.link(id, 1);
                    sendingThread.sendDistanceVec();
					//client.getSendingThread().link(arr[1] + ":" + arr[2], 1);
				} catch (IOException e) {
					System.err.println("error sending message.");
					e.printStackTrace();
				}
			}
			
			else if (arr[0].equalsIgnoreCase("SHOWRT")) {
				printRoutingTable(dv, next);
			}
			
			else if (arr[0].equalsIgnoreCase("CLOSE")) {
				client.close();
				break;
			}
			
			else {
				System.out.println("Unrecognized command.");
				continue;
			}
		}
	} //end of run method
	
	public void printRoutingTable(ConcurrentHashMap<String, Float> dv, ConcurrentHashMap<String, String> next) {
		//ConcurrentHashMap<String, Float> dv = client.getDistanceVector();
		//ConcurrentHashMap<String, String> next = client.getNextNode();
		Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a ");
        String formattedDate = sdf.format(date);
        System.out.println(formattedDate + " " + "Distance vector list is:");
        
        for (String id : dv.keySet()) {
        	System.out.println("Destination = " + id +
        			", Cost = " + dv.get(id) + " Link = (" + next.get(id) + ")");
        }
        
	}
	
}
