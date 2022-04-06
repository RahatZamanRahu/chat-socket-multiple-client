import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Scanner;

public class MainServer
{

	private static class ClientHandler implements Runnable
	{
		private Socket socket;
		private PrintWriter out;
		private BufferedReader in;
		private String name;

		public ClientHandler(Socket socket)
		{
			this.socket = socket;
		}

		@Override
		public void run()
		{
			if (verbose)
				System.out.println("client connected " + socket.getInetAddress());

			try
			{
				in = new BufferedReader(
						new InputStreamReader(socket.getInputStream()));
				out = new PrintWriter(socket.getOutputStream(), true);

				while (true)
				{
					out.println("Please Enter your name:\n");
					name = in.readLine();

					if (name == null)
						return;

					// prevent a data race
					synchronized (connectedClients)
					{
						if (!name.isEmpty()
								&& !connectedClients.keySet().contains(name))
							break;
						else
							out.println("Name already exists or invalid name. Please enter an unique name.\n\n");

					}
				}

				out.println("Welcome " + name+"\n");
				if (verbose)
					System.out.println(name + " has joined\n");
				broadcastMessage(name + " has joined\n");
				connectedClients.put(name, out);

				String message;
				out.println("(type \\quit to exit)\n--------------------------Start Conversations--------------------------\n");
				while ((message = in.readLine()) != null)
				{
					if (!(message.isEmpty()))
					{
						if (message.toLowerCase().equals("/quit"))
						{
							break;
						}

						broadcastMessage(name + ": " + message);
					}
				}

			} catch (Exception e)
			{
				if (verbose)
					System.out.println(e);
			}

			finally
			{
				if (name != null)
				{
					if (verbose)
						System.out.println(name + " is leaving");

					connectedClients.remove(name);
					broadcastMessage(name + " has left");
				}
			}
		}

	}
//----------------------

	// Map of clients connected to the server
	private static HashMap<String, PrintWriter> connectedClients = new HashMap<>();

	// Set maximum amount of connected clients
	private static final int MAX_CONNECTED = 50;
	// Server port
	private static final int PORT = 59002;

	private static boolean verbose;

	private static ServerSocket listener;

	// Broadcast to all clients in map by writing to their output streams
	private static void broadcastMessage(String message)
	{
		for (PrintWriter p : connectedClients.values())
		{
			p.println(message);
		}
	}

	// starts listening for connections, creating threads to handle incoming
	// connections on the
	// specified port

	public static void start(boolean isVerbose)
	{
		verbose = isVerbose;

		try
		{
			listener = new ServerSocket(PORT);

			// verbose
			if (verbose)
			{
				System.out.println("Server started on port: " + PORT);
				System.out.println("Now listening for connections ...");
			}

			// client connection loop to accept new socket connections
			while (true)
			{
				// limit to a maximum amount of connected clients (a client
				// could disconnect, allowing a new connection)
				if (connectedClients.size() <= MAX_CONNECTED)
				{
					// dispatch a new ClientHandler thread to the socket
					// connection
					Thread newClient = new Thread(
							new ClientHandler(listener.accept()));
					newClient.start();
				}

			}
		} catch (BindException e)
		{
			// server already started on this port ... continue
		} catch (Exception e)
		{
			// error verbose
			if (verbose)
			{
				System.out.println("\nError occured: \n");
				e.printStackTrace();
				System.out.println("\nExiting...");
			}
		}
	}

	public static void stop() throws IOException
	{
		if (!listener.isClosed())
			listener.close();
	}

	// server class entry
	// stop or start the server

	public static void main(String[] args) throws IOException
	{
		boolean isVerbose;
		isVerbose = (args.length == 1
				&& args[0].toLowerCase().equals("verbose")) ? true : false;
		start(isVerbose);
	}
}
