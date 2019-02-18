import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class WebServer {
	
	private static byte[] parseHost(byte[] request) {
		
		int y=0;
		int size=0;
		byte[] host=new byte[65535];
				
		for (int i=0; i<request.length;i++) {
			
			if(request[i]=='H' && request[i+1]=='o' && request[i+2]=='s' && request[i+3]=='t' && request[i+4]==':') {
				
				i=i+6;
				
				while (request[i]!='\r' && request[i+1]!='\n') {
					
					if (request[i]!=':') {
						host[y]=request[i];
						i++;
						y++;
						size=y;
					}
					
					else 
						break;
				}
				
				break;
			}
		}
		
		host = Arrays.copyOf(host, size);	
		return host;
	}
	

	private static byte[] parsePath(byte[] request, int reqLength, int hostLength) {
		
		byte[] pathName = new byte[reqLength];
		boolean flag = false;
		int counter = hostLength+10;
		int k=0;
		int size=0;
		
		for(; (counter<reqLength); counter++) {
			
			 if(request[counter] == '/' ) {
				
				while(!flag) {
					
					pathName[k] = request[counter];
					
					if(request[counter] == ' ' || request[counter] == '\r')
						flag=true;
					
					k++;
					counter++;	
				}
				
				size=k;
			}
			 
			 else
				 continue;
		}
		
		pathName = Arrays.copyOf(pathName,size);
		return pathName;
	}
	
	private static int parsePort(byte[] request) {
		
		int y=0;
		int size=0; 
		boolean flag=false;
		byte[] temp = new byte[5];
		int port=80;
				
		for (int i=0; i<request.length;i++) {
			
			if(request[i]=='H' && request[i+1]=='o' && request[i+2]=='s' && request[i+3]=='t' && request[i+4]==':') {
				
				i=i+6;
				
				while (request[i]!='\r' && request[i+1]!='\n') {
					
					if (request[i]!=':') {
						
						i++;
						continue;
					}
					
					else {
						
						i=i+1;
						flag=true;
					
						while (request[i]!='\r') {
							
							temp[y]=request[i];
							y++;
							i++;
							size=y;
						}
						
						break;
					}
				}
			}
			
			if (flag==true) {
				
				temp = Arrays.copyOf(temp,size);
				port=Integer.parseInt(new String(temp));
				break;
			}
		}
		
		return port;
	}
	
	private static void doLookup (byte[] host, Socket clientSocket, byte[] pathName, int port) throws IOException {
		
		InetAddress[] addresses;
		InetAddress address=null;
		
		try {
			addresses = InetAddress.getAllByName(new String(host));
			address=addresses[0];
			doRequest(address,clientSocket,pathName,host,port);
		}
		
		catch (UnknownHostException E) {
			clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n".getBytes("UTF-8"));
			clientSocket.getOutputStream().write("Content-type: text/html\r\n\r\n".getBytes("UTF-8"));
			clientSocket.getOutputStream().write("<html><body><h1>Server can't be reached</h1>".getBytes("UTF-8"));
			clientSocket.getOutputStream().write("<p>Given website's server address could not be found.</p></body></html>".getBytes("UTF-8"));
			clientSocket.getOutputStream().close();
			clientSocket.close();
		}
	}
		
	private static void doRequest(InetAddress address, Socket clientSocket, byte[] pathName, byte[] host, int port) throws IOException {
		
		Socket newServerSocket = null;
		
		try {
			newServerSocket = new Socket(address, port);
		}
			
		catch(Exception E) {
			clientSocket.getOutputStream().write("HTTP/1.1 400 Bad Request\r\n".getBytes("UTF-8"));
			clientSocket.getOutputStream().write("Content-type: text/html\r\n\r\n".getBytes("UTF-8"));
			clientSocket.getOutputStream().write("<html><body><h1>Bad Request</h1>".getBytes("UTF-8"));
			clientSocket.getOutputStream().write("<p>Requested URL is inappropriate.</p></body></html>".getBytes("UTF-8"));
			clientSocket.getOutputStream().close();
			clientSocket.close();
		}
		
		if(port==80)
			newServerSocket.getOutputStream().write(("GET " +new String(pathName) + "HTTP/1.1\r\n" +  "Host: " +new String(host)  + "\r\n" +  "Connection: close\r\n\r\n").getBytes("UTF-8"));
		else
			newServerSocket.getOutputStream().write(("GET " +new String(pathName) + "HTTP/1.1\r\n" +  "Host: " +new String(host)+":"+String.valueOf(port)  + "\r\n" +  "Connection: close\r\n\r\n").getBytes("UTF-8"));
		
		int byteReader = 0;
		byte reader[] = new byte[1048];
		
		while(byteReader!=-1) {

			byteReader = newServerSocket.getInputStream().read(reader, 0, reader.length);
				
			if (byteReader==-1)
				continue;
				
			try {
				clientSocket.getOutputStream().write(reader, 0, byteReader);
			}
			catch (Exception E) {
				continue;
			}
		}
	}
	
	private static boolean doBlacklist (byte[] host, Socket clientSocket, String args) throws IOException {
		
		FileInputStream in = new FileInputStream(args);
		int c=0;
		boolean flag=false;
		
		while (c!=-1) {
			
			int i=0;
			int size=0;
			byte[] blockedSite = new byte[2083];
			c=in.read();
			
			while (c!=13 && c!=-1) {
				
				blockedSite[i]=(byte)c;
				c=in.read();
				i++;
				size=i;
			}
			
			blockedSite = Arrays.copyOf(blockedSite,size);
			
			if (Arrays.equals(host, blockedSite))
				flag=true;
			
			if (c==-1) {
				in.close();
				break;
			}
			
			c=in.read();
			continue;
		}
		
		return flag;
	}
			
	public static void main(String[] args) throws IOException {
		
		ServerSocket serverSocket = new ServerSocket();
		int socketPort = Integer.parseInt(args[1]);
		
		serverSocket.bind(new InetSocketAddress(socketPort));
		
		for(;;) {
			
			try(Socket clientSocket = serverSocket.accept()) {
				
				System.out.println("stage 1 program by Group number: M3 (Group Leader: djn26) listening on port: "+args[1]);
				byte request[]=new byte[65535];
				
				try {
			
					int reqLength =	clientSocket.getInputStream().read(request,0,65535);
					byte host[] = parseHost(request);
					boolean flag = doBlacklist(host,clientSocket,args[0]);
					
					if (flag==true) {
						
						clientSocket.getOutputStream().write("HTTP/1.1 403 Forbidden\r\n".getBytes("UTF-8"));
						clientSocket.getOutputStream().write("Content-type: text/html\r\n\r\n".getBytes("UTF-8"));
						clientSocket.getOutputStream().write("<html><body><h1>403 Forbidden</h1>".getBytes("UTF-8"));
						clientSocket.getOutputStream().write("<p>This site is forbidden by proxy server, that you are using.</p></body></html>".getBytes("UTF-8"));
					}
					
					else {
						
						if (reqLength>65535) {
					
							System.out.println("Request is more than 65535 bytes. Hence closing the connection!");
							clientSocket.close();
						}
					
						if (reqLength == -1)
							continue;
					
						else {
						
							int port = parsePort(request);

							if (port<0 || port>65535) {
								System.out.println("Port number is out of valid range");
								System.exit(1);
							}
						
							int hostLength = host.length;
							byte[] pathName = parsePath(request,reqLength,hostLength);
							System.out.println("REQUEST: http://"+new String(host)+new String(pathName)+"\n");
	
							doLookup(host,clientSocket,pathName,port);
						}
					}				
				}
				
				catch (Exception E) {
					clientSocket.close();
					E.printStackTrace();
				}	
			}
		}
	}
}