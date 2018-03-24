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
	quit			----> JokeClient / JokeClientAdmin input to exit the program
	[enter]		----> Simply press enter to request a joke/proverb (in JokeClient program)
					  or to switch the server into Joke/Proverb mode (in JokeClientAdmin program)

----------------------------------------------------------*/

//Import the Java libraries for input/output, working with networks, and unique user identification
import java.io.*;
import java.net.*;
import java.util.UUID;

/* This class represents the Client. It contains static variables used uuid and for primary/secondary server accessibility.
 * It also contains a main method which takes user input used for requesting jokes/proverbs, switching between the primary
 * and secondary servers, and for exiting the program.
 */
public class JokeClient {

	static String uuid = UUID.randomUUID().toString(); //Unique user identifiction
	static boolean secondaryAvailable = false;	//True if the user entered 2 args representing hosts on the command line
	static String defaultServer;	//Contains the name of the default server
	static String secondaryServer;	//Contains the name of the secondary server
	static String currentServer;	//Contains the name of the server this client is currently connected to
	static int defaultPort = 4545;	//The port at which the default server will be connected through
	static int secondaryPort = 4546;	//The port at which the secondary server will be connected through
	static int currentPort;		//The current port that the client is connected through
	
	/* This main method first parses none/one/two args from the command line for server names to connect to.
	 * Then acquires the users name through an input stream. Then takes user input for requesting jokes/proverbs
	 * for switching between servers, or for exiting the program. If the user is requesting a joke/proverb, 
	 * the getServerResponse() method is called.
	 */
	public static void main (String args[]) {
		
		/* Parse command line args and update static vars containing primary/secondary server names.
		 * If no args are present, set default server to "localhost".
		 * Also update the port at which the client is currently connected to the server through.
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
		System.out.println("JokeClient starting up");
		System.out.println("Server one: " + defaultServer + ", port " + defaultPort);
		if (secondaryAvailable)
			System.out.println("Server two: " + secondaryServer + ", port " + secondaryPort);

		//Setup a buffered reader to read characters from input stream
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

		//This try block may throw an IOException which will be caught after
		try {

			//Create a variable to store the user's name, and another input variable to update in a loop
			String name;
			String input;
			
			//Request the user's name
			System.out.print("Enter your name: ");
			System.out.flush();
			name = in.readLine();

			/* Acquire input from the user, switching to the second server (if available) when user enters "s",
			 * otherwise requesting a joke/proverb from the server for any other input, except exiting the loop only
			 * when user enters 'quit'
			 */
			do {

				//Request input
				System.out.print("Press enter for joke/proverb, (s) to switch to secondary server, (quit) to exit:" );
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

				//For any other input that is not "quit", call getServerResponse() method
				if (!input.equals("quit")) {
					//Call helper method getServerResponse() which communicates with server
					getServerResponse(uuid, name, currentServer);
				}
			
			//Exit the loop if user input is "quit"
			} while (!input.equals("quit"));
			System.out.println("Cancelled by client.");
		} catch (IOException x) {
			x.printStackTrace();
		}
	}
	
	/* This method takes 3 arguments: the uuid, the users name, and the name of the
	 * server to which the client will make a request. After setting up means for server
	 * communication through a print stream and buffered reader, this method sends the
	 * server the client's uuid and name, and then reads the response from the buffer
	 * and prints it to the console.
	*/
	static void getServerResponse(String identifier, String name, String serverName) {

		Socket sock;	//Socket to use for server connection
		BufferedReader fromServer;	//Buffer used for receiving text from server
		PrintStream toServer;	//Print stream used to send info to the server
		String textFromServer;	//Var used to store the server's response
		
		try {

			//Initialize the socket and the print stream / buffer reader through the socket
			sock = new Socket(serverName, currentPort);
			fromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			toServer = new PrintStream(sock.getOutputStream());
			
			//Send the server the user's uuid and name. Flush the print stream
			toServer.println(identifier + "\n" + name);
			toServer.flush();
			
			//Read 2 lines of input from the server into the string variable and print to console
			for (int i = 1; i <= 3; i++) {
				textFromServer = fromServer.readLine();
				if (textFromServer != null)
					System.out.println(textFromServer);
			}

			//Close the socket before exiting this method
			sock.close();
		} catch (IOException x) {
			System.out.println("Socket error.");
			x.printStackTrace();
		}
	}
}