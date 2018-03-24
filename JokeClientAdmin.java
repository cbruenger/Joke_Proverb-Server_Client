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

> java JokeClient localhost 140.192.1.22
> java JokeClientAdmin localhost 140.192.1.22

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

	localhost	----> Command line arg when using the local machine as the host
	secondary	----> Command line arg when launching the secondary server
	s			----> JokeClient / JokeClientAdmin input to switch to secondary server
	quit		----> JokeClient / JokeClientAdmin input to exit the program
	[enter]		----> Simply press enter to request a joke/proverb (in JokeClient program)
					  or to switch the server into Joke/Proverb mode (in JokeClientAdmin program)

----------------------------------------------------------*/

//Import the Java libraries for input/output and working with networks 
import java.io.*;
import java.net.*;


/* This class represents the Admin Client. It contains static variables used for aknowledging a possible secondary server,
 * and for switching between the primary/secondary servers. 
 * It also contains a main method which takes user input for switching between servers, for switching the mode of the 
 * servers to between joke/proverb modes, and for exiting the program.
 */
public class JokeClientAdmin {

	static boolean secondaryAvailable = false;	//True if 2 servers, false if only 1
	static String defaultServer;	//Contains the name of the default server
	static String secondaryServer;	//Contains the name of the secondary server
	static String currentServer;	//Contains the server name to which the admin client is currently connected
	static int defaultPort = 5050;	//The port number at which the admin connects to the default server
	static int secondaryPort = 5051;	//The port number at which the admin connects to the secondary server
	static int currentPort;	//The port number through which the admin is currently connected to the server
	
	/* This main method first parses none/one/two args from the command line for server names to connect to.
	 * Then takes user input for switching between servers, switching server modes, or exiting the program.
	 * The switchServerMode() method is called if the user enters any input except for "s" or "quit".
	 */
	public static void main (String args[]) {

		/* Parse command line args and update static vars containing primary/secondary server names.
		 * If no args are present, set default server to "localhost".
		 * Also update the port at which the admin is currently connected to the server through.
		 */
		if (args.length < 1) {
			defaultServer = "localhost";
			currentServer = "localhost";
			currentPort = defaultPort;
		} else if (args.length == 1) {
			defaultServer = args[0];
			currentServer = defaultServer;
			currentPort = defaultPort;
		} else {
			secondaryAvailable = true;
			defaultServer = args[0];
			secondaryServer = args[1];
			currentServer = defaultServer;
			currentPort = defaultPort;
		}

		//Print info to console
		System.out.println("JokeClientAdmin starting up");
		System.out.println("Server one: " + defaultServer + ", port " + defaultPort);
		if (secondaryAvailable)
			System.out.println("Server two: " + secondaryServer + ", port " + secondaryPort);

		//Setup a buffered reader to read characters from input stream
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		//This try block may throw an IOException which will be caught after
		try {

			//Create a variable to store user input in a loop
			String input;

			/* Acquire input from the user, switching to the second server (if available) when user enters "s",
			 * otherwise switching the server between joke/proverb mode, except exiting the loop only
			 * when user enters 'quit'
			 */
			do {
				//Request input from user and flush the buffer
				System.out.print("Press enter to switch modes, (s) to switch to secondary server, (quit) to exit: ");
				System.out.flush();

				//Read the input
				input = in.readLine();
				
				/* If input is "s" and there is a secondary server, switch the current port to the other available port
				 * and print a notification informing the user of the switch.
				 * If there is no secondary server, print a notification informing the user
				 */
				if (input.equals("s")) {
					if (secondaryAvailable) {
						if (currentServer.equals(defaultServer) && currentPort == defaultPort) {
							currentServer = secondaryServer;
							currentPort = secondaryPort;
						} else {
							currentServer = defaultServer.toString();
							currentPort = defaultPort;
						}
						System.out.println("Now communicating with: " + currentServer + ", port " + currentPort);
					} else {
						System.out.println("No secondary server being used.");
					}
				}

				//For any other input that is not "quit", call switchServerMode() method
				if (!input.equals("quit")) {
					//Call helper method switchServerMode() which communicates with server
					switchServerMode(currentServer);
				}
			//Exit the loop if user input is "quit"	
			} while (!input.equals("quit"));
			System.out.println("Cancelled by admin.");
		} catch (IOException x) {
			x.printStackTrace();
		}
	}
	
	/* This method takes input representing the name of the server to which the admin 
	 * client will make a request input. Since no uuid or identification is necessary and
	 * the client entered any arbitraty text besides "s" or "quit", no print steam is
	 * necessary. Simply connecting to the server at the port at which it is listening for
	 * an admin request will trigger the server to switch between joke/proverb modes.
	 * After setting up a socket, this method reads 2 lines of server response from its buffered
	 * reader and prints it to the console.
	*/
	static void switchServerMode(String serverName) {

		Socket sock;	//Socket used for server communication
		BufferedReader fromServer;	//Buffer used form receiving text from the server
		String textFromServer;	//String used to store server response in
		
		try {

			//Initialize the socket and the buffer reader through the socket
			sock = new Socket(serverName, currentPort);
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			
			//Iterate through the buffered reader of response from the server, printing up to 2 lines
			for (int i = 1; i <= 3; i++) {
				textFromServer = fromServer.readLine();
				if (textFromServer != null)
					System.out.println(textFromServer);
			}

			//Close the socket
			sock.close();
		} catch (IOException x) {
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
}