Distributed Bellman-Ford Algorithm for Routing
Yin Zhao (yz2426)

-- 5 Source Files:

bfclient.java
Listener.java
KeyboardListener.java
SendingThread.java
Packet.java
Neighbor.java

-- To Run:

make

java bfclient <localport> <timeout> [ipaddress1 port1 distance1 ...]

-- Features:

Compute minimum distances between pairs of nodes using a distributed Bellman-Ford algorithm. 
On entering the network, a client will send its distance vectors to its neighbors;
on receiving a packet (bfclient class), a Listener thread will analyse the packet and linkdown/linkup/update. Send distance vector to its neighbors if updated.
KeyboardListener thread waits for keyboard input command. Supports LinkDown, Linkup, Showrt and CLOSE.
On CLOSE, the client will send link down to all its neighbors to remove itself from their neighbor lists and set distances to infinity, remove any path with the client as first hop. Then exit the program.
Send distance vectors periodically or on updating.

Packet: messages among clients. Sent through UDP.
Format: 
|length 4 byte int| type 4 byte int | senderID ID_SIZE byte | distanceVector (ID_SIZE + 4) * n byte |
