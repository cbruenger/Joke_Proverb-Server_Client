/*--------------------------------------------------------

1. Craig Bruenger / 1-19-2018:

2. Java version used: build 1.8.0_144-b01

3. Precise command-line compilation examples / instructions:

> javac *.java

4. Precise examples / instructions to run this program:

In separate shell windows:

> java JokeServer
> java JokeClient
> java JokeClientAdmin

All acceptable commands are displayed on the various consoles.

This runs across machines, in which case you have to pass the IP address of
the server to the clients. For example, if the server is running at
140.192.1.22 then you would type:

> java JokeClient 140.192.1.22
> java JokeClientAdmin 140.192.1.22

If two arguments are given for JokeClient / JokeClientAdmin, the first is used
as the default server, and the second is used as the secondary server:

> java JokeClient localHost 140.192.1.22
> java JokeClientAdmin localHost 140.192.1.22

When running the JokeServer, the argument "secondary" can be passed on the
command line in order to launch as the secondary server:

> java JokeServer secondary

5. List of files needed for running the program.

 a. checklist.html
 b. JokeServer.java
 c. JokeClient.java
 d. JokeClientAdmin.java

5. Notes:

When passing arguments on the command line and when providing input, make sure
to use EXACTLY the following text, without any spaces or differences in capitalization:

	localHost	----> Command line arg when using the local machine as the host
	secondary	----> Command line arg when launching the secondary server
	s			----> JokeClient / JokeClientAdmin input to switch to secondary server
	quit			----> JokeClient / JokeClientAdmin input to exit the program
	[enter]		----> Simply press enter to request a joke/proverb (in JokeClient program)
					  or to switch the server into Joke/Proverb mode (in JokeClientAdmin program)

----------------------------------------------------------*/


/* This file contains 4 classes, the primary joke server class and its worker class, and also the 
 * class which is spawned for admin interaction and its worker class. The primary joke server contains
 * the main method which spawns the 2nd thread for andmin interaction, and contains the loop which waits
 * for clients.
 */

//Import the Java libraries for input/output, for working with networks, and necessary data structures/collections
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;

/* This class represents the server and contains static variables and data structures, and also
 * the main method. The static variables are accessed by the other classes in this file, when needed
 * for joke/proverb mode state management by the admin, and also for client state management.
 */
public class JokeServer {
	
	static boolean secondary = false; //Used for construction of primary/secondary server based on command line arg
	static boolean controlSwitch = true; //I was going to use this var to allow the admin to shutdown the server but didn't get to it
	static boolean jokeMode = true; //If true, server is in joke mode. If false, server is in proverb mode
	static String serverTag = ""; //Empty string if primary server, contains "<S2>" if secondary server
	
	/* These HashMaps contain the client UUID's mapped to an int which represents the index of the last joke/proverb
	 * the client received from a randomly shuffed array containing the jokes/proverbs
	 */
	static HashMap<String, Integer> lastJokeSent = new HashMap<String, Integer>();
	static HashMap<String, Integer> lastProverbSent = new HashMap<String, Integer>();
	
	/* Arrays containing the jokes and proverbs. They are shuffled at the beginning of program creation,
	 * and then shuffled again each time a client has seen each joke/proverb.
	 */
	static String[] jokes = {"JA Joke 1", "JB Joke 2", "JC Joke 3", "JD Joke 4"};
	static String[] proverbs = {"PA Proverb 1", "PB Proverb 2", "PC Proverb 3", "PD Proverb 4"};
	
	/* This main method checks for command line arg, shuffles the jokes/proverbs, spawns the admin
	 * accessible thread, sets up a socket and port for communication with clients and then enters
	 * a loop waiting for client interactions.
	 */
	public static void main(String args[]) throws IOException {
		
		//If user entered "secondary" as arg on command line, this is a secondary server
		if (args.length > 0 ) {
			if (args[0].equals("secondary")) {
				secondary = true;
				serverTag = "<S2> ";
			}
		}

		System.out.println(serverTag + "JokeServer starting up");
		
		//Shuffle joke/proverb arrays so they are in a random order
		Collections.shuffle(Arrays.asList(JokeServer.jokes));
		Collections.shuffle(Arrays.asList(JokeServer.proverbs));
		
		//Spawn thread that runs a class used for admin interaction
		AdminAccessor AA = new AdminAccessor();
		Thread t = new Thread(AA);
		t.start();
		
		int q_len = 6;	//Maximum number of client requests to queue
		Socket sock;	//A socket that will be designated for each client request
		int port;	//The port at which the server will accept requests
		
		//Assigns port based on if this is the primary or secondary server
		if (secondary)
			port = 4546;
		else
			port = 4545;
		
		//Represents a server socket that is constructed on a given port and with a given queue length
		ServerSocket servsock = new ServerSocket(port, q_len);

		System.out.println(serverTag + "Listening for clients at port " + port + ".");

		/* This loop runs for the life of the program, waiting for client requests, and then calling the accept() method
		/  on the server socket which returns a new socket to be used. For each request, a new Worker class is started in
		/  its own thread to do the work
		*/
		while (controlSwitch) {
			sock = servsock.accept();	//Assigns the 'sock' var to a new socket to accept a client request
			new Worker(sock).start();	//An instance of Worker is constructed with the given socket and started in its own thread
		}

		//Close the socket when the program ends
		servsock.close();
	}
}

/* This class "does the work" of the primary server. After setting up means of input/output through the socket, 
 * it acquires the clients UUID and name, and then calls the handleClient() method, which uses some helper
 * functions to maintain client state and send the client a joke/proverb.
 */
class Worker extends Thread {

	//This socket is a class member, local to the Worker
	Socket sock;

	//Constructor, takes a socket as an argument and assigns the class member socket to it
	Worker (Socket s) {
		this.sock = s;
	}

	/* Since the class is setup to function in a multi-threaded environment, this method
	 * is automatically called upon invoking the .start() method on an instance of the class.
	 * This method creates and initializes a print stream to send communication through the
	 * given socket, and also creates a buffered reader to accept communication from client
	 * through the given socket. It acquires the client's UUID and name, calls the handleClient()
	 * method, and then closes the socket.
	 */
	public void run() {
		PrintStream out = null;	//This print stream variable will be used to send communication to the client
		BufferedReader in = null; //This var is a buffer which will receive characters from the client
		try {
			in = new BufferedReader(new InputStreamReader(sock.getInputStream())); //Initialize buffer reader variable with input stream reader through the given socket
			out = new PrintStream(sock.getOutputStream());	//Initialize the output stream to send communication to the client through the socket

			/* Attempt to retrieve client UUID and name from the buffered reader and call the handleClient() method.
			/  Otherwise if there is a problem retrieving the data from the buffer, an IOException is caught.
			*/
			try {

				//Assign client's uuid and name to vars from the buffered reader
				String uuid;
				String name;
				uuid = in.readLine();
				name = in.readLine();
				
				//Call method to maintain client state and send joke/proverb
				handleClient(uuid, name, out);
								
			} catch (IOException x) {
				System.out.println(JokeServer.serverTag + "Server read error");
				x.printStackTrace();
			}
			sock.close();	//Close the socket
		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
	
	/* Calls a method for sending a joke/proverb to a client depending on which state the server is currently in.
	 * If the client is new, an initial state is created by calling the initializeClientState() method
	 */
	private void handleClient(String uuid, String name, PrintStream out) {
		if (JokeServer.jokeMode) {
			if (!JokeServer.lastJokeSent.containsKey(uuid))
				initializeClientState(uuid);
			sendJoke(uuid, name, out);
		} else {
			if (!JokeServer.lastProverbSent.containsKey(uuid))
				initializeClientState(uuid);
			sendProverb(uuid, name, out);
		}
	}
	
	/* Called when a client is making their first request. Their UUID is stored in the JokeServer's
	 * hashtable mapped to a value of -1, meaning they have not yet received a joke/proverb.
	 */
	private void initializeClientState(String uuid) {
		if (JokeServer.jokeMode) {
			JokeServer.lastJokeSent.put(uuid, -1);
		} else {
			JokeServer.lastProverbSent.put(uuid, -1);
		}
	}

	/* Sends clients randomized jokes, and maintains the servers booking keeping of the client's state
	 * by updating the lastJokeSent value, representing an index in the array of jokes. When the index
	 * is the same as the length of the joke array, the array is shuffled, and the index is reset to -1.
	 */
	private void sendJoke(String uuid, String name, PrintStream out) {
		
		int numJokes = JokeServer.jokes.length;	//The number of jokes in the joke array
		int joke = JokeServer.lastJokeSent.get(uuid);	//The index of the joke that the client previously received
		joke++;	//Increment the index, since it is initialzed as -1
		
		//Print customized string (through print stream and to console) containing the client's name and joke tag
		out.println(JokeServer.serverTag + JokeServer.jokes[joke].substring(0, 3) + name + ": " + JokeServer.jokes[joke].substring(3));
		System.out.println(JokeServer.serverTag + "Sent " + name + " Joke " + JokeServer.jokes[joke].substring(0, 3));
		
		/* If the index is now the last joke in the array, print the cycle completion notification (through print stream and to console),
		 * then reset the lastJokeSent value to -1 and shuffle the joke array. Otherwise, simply update the index of the last joke sent
		 * in the HashTable.
		 */
		if (joke == numJokes - 1) {
			out.println(JokeServer.serverTag + "JOKE CYCLE COMPLETED");
			System.out.println(JokeServer.serverTag + "JOKE CYCLE COMPLETED FOR " + name);
			JokeServer.lastJokeSent.replace(uuid, -1);
			Collections.shuffle(Arrays.asList(JokeServer.jokes));
		} else {
			JokeServer.lastJokeSent.replace(uuid, joke);
		}
	}
	
	/* Sends clients randomized proverbs, and maintains the servers booking keeping of the client's state
	 * by updating the lastProverbSent value, representing an index in the array of proverbs. When the index
	 * is the same as the length of the proverb array, the array is shuffled, and the index is reset to -1.
	 */
	private void sendProverb(String uuid, String name, PrintStream out) {
		
		int numProverbs = JokeServer.proverbs.length;//The number of proverbs in the proverb array
		int proverb = JokeServer.lastProverbSent.get(uuid); //The index of the proverb that the client previously received
		proverb++;	//Increment the index, since it is initialzed as -1
		
		//Print customized string (through print stream and to console) containing the client's name and proverb tag
		out.println(JokeServer.serverTag + JokeServer.proverbs[proverb].substring(0, 3) + name + ": " + JokeServer.proverbs[proverb].substring(3));
		System.out.println(JokeServer.serverTag + "Sent " + name + " Proverb " + JokeServer.proverbs[proverb].substring(0, 3));
		
		/* If the index is now the last proverb in the array, print the cycle completion notification (through print stream and to console),
		 * then reset the lastProverbSent value to -1 and shuffle the proverb array. Otherwise, simply update the index of the last proverb sent
		 * in the HashTable.
		 */
		if (proverb == numProverbs - 1) {
			out.println(JokeServer.serverTag + "PROVERB CYCLE COMPLETED");
			System.out.println(JokeServer.serverTag + "PROVERB CYCLE COMPLETED FOR " + name);
			JokeServer.lastProverbSent.replace(uuid, -1);
			Collections.shuffle(Arrays.asList(JokeServer.proverbs));
		} else {
			JokeServer.lastProverbSent.replace(uuid, proverb);
		}
	}
}


/* An instance of this class is created by the server and run in its own thread in order admin interaction
 * through a different port.
 */
class AdminAccessor implements Runnable {
	
	/* Since this class runs in its own thread, this run() method is called upon creation. It creats a socket
	 * and a secondary port for the admin to have acces through. Enters a loop, waiting for admin requests.
	 */
	public void run() {

		int q_len = 6;	//Maximum number of client requests to queue
		Socket sock;	//A socket that will be designated for each client request
		int port;	//The port at which the server will accept requests
		
		//Assigns port based on if this is the primary or secondary server
		if (JokeServer.secondary)
			port = 5051;
		else
			port = 5050;
		
		System.out.println(JokeServer.serverTag + "Listening for admin at port " + port + ".");

		try {
			//Represents a server socket that is constructed on a given port and with a given queue length
			ServerSocket servsock = new ServerSocket(port, q_len);

			/* This loop runs for the life of this thread, waiting for admin requests, and then calling the accept() method
			 * on the server socket which returns a new socket to be used. For each request, a new AdminWorker class is started in
			 * its own thread to do the work
			 */
			while (JokeServer.controlSwitch) {
				sock = servsock.accept();
				new AdminWorker(sock).start();
			}

			//Close the socket when this thread is terminated
			servsock.close();

		} catch (IOException ioe) {
			System.out.println(ioe);
		}
	}
}


/* This class "does the work" of the Admin accessible server thread. After setting up means of output through the socket,
 * it changes the 'jokeMode' boolean and prints a prints a message (through the print stream and to the console).
 * (Note that there is no buffered reader since no explicit input is necessary besides having accepted an admin client
 * request through the socket when the admin presses [enter]). 
 */
class AdminWorker extends Thread {

	//This socket is a class member, local to the AdminWorker
	Socket sock;
	
	//Constructor, takes a socket as an argument and assigns the class member socket to it
	AdminWorker(Socket s) {
		this.sock = s;
	}
	
	/* Since the class is setup to function in a multi-threaded environment, this method
	 * is automatically called upon invoking the .start() method on an instance of the class.
	 * This method creates and initializes a print stream to send communication through the
	 * given socket. It switches the mode of the server and then prints a message through the
	 * print stream and to the console.
	 */
	public void run() {


		PrintStream out = null;	//This print stream variable will be used to send communication to the admin

		try {

			out = new PrintStream(sock.getOutputStream());	//Initialize the output stream to send communication to the admin through the socket
			
			//Change the boolean representing the server's current mode. True = jokeMode, False = proverbMode
			JokeServer.jokeMode = !JokeServer.jokeMode;

			//Create a string based on the server's current mode
			String mode;
			if (JokeServer.jokeMode)
				mode = "JOKE";
			else
				mode = "PROVERB";

			//Print customized message through print stream and to console
			System.out.println(JokeServer.serverTag + "Server has been switched into " + mode + " mode by admin.");
			out.println(JokeServer.serverTag + "Server has switched into " + mode + " mode.");
			
			//Close the socket
			sock.close();

		} catch (IOException ioe2) {
			ioe2.printStackTrace();
			System.out.println(ioe2);
		}
	}
}