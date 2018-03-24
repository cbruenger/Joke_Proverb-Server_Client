This program contains classes representing a server, a client and an admin client. THIS IS NOT A SECURE PROGRAM! USE CAUTION!

Program execution:

	- To compile, enter the command: $ javac *.java
	- To run the primary server: $ java JokeServer
	- To run the secondary server: $ java JokeServer secondary
	- To run the client locally: $ java JokeClient
	- To run the client locally with an available secondary server: $ java JokeServer localhost localhost
	- To run the client with a remote server where arg1 is the IP of the remote server: $ java JokeClient arg1
	- To run the client with a secondary server where either server can be local or remote and arg1 and arg2 are either 'localhost' or the IP of the remote server: $ java JokeClient arg1 arg2
	- The admin client follows the same conventions as shown above for the client, but the program name is 'JokeClientAdmin' instead of 'JokeClient'.

Server:

	- Upon running the server, with an optional argument to run the server as a secondary, it listens at port 4545 for client requests and at port 5050 for the admin client.

	- In the case of the secondary server, it listens for clients at port 4546 and for the admin client at port 5051.

	- When a client request is received (no data, just a simple request), the server sends one of four jokes or proverbs, depending if the server is currently in joke mode or server mode.

	- The server maintains the state of the conversations with each client, sending one of the 4 jokes/proverbs randomly to clients, and ensuring that each joke/proverb has been sent to the client before starting the joke/proverb cycle over again (the cycles for joke states and proverb states are handled separately).

	- When any request is made by the admin (no data, just a simple request), the server is switched into joke/proverb mode (whichever mode it currently is not in).

Client:

	- Upon running the client the user will be prompted to enter their name. 

	- The user will then be prompted to press enter to receive a joke/proverb, 's' to switch to the secondary server, or 'quit' to exit the program.

	- When a request is made, the user receives one of the for jokes/proverbs.

	- The users state of conversation reguarding jokes/proverbs is maintained separately, and also separately among the primary and secondary servers.

Admin Client:

	- Upon running the admin client a connection is made with the primary server at port 5050.

	- The user will be prompted to press enter to switch the server mode, 's' to switch between primaray/secondary servers, or 'quit' to exit.
