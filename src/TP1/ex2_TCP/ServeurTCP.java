package TP1.ex2_TCP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ServeurTCP {
	static int nombreClients = 0;
	
	class Messagerie extends Thread{
		Socket socketClient;
		PrintWriter pw;
		
		public Messagerie(Socket socketClient,PrintWriter pw){
			this.socketClient = socketClient;
			this.pw = pw;
		}
		
		public void run(){
				try {
					while(true){
						pw.write("Nombre de clients: " + nombreClients + "\n");
						pw.flush();
						Thread.sleep(10000);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					nombreClients--;
					e.printStackTrace();
				}
			}
	}
	
	public void demarrer(){
		try(ServerSocket server = new ServerSocket(1027)){
			while(true){
				Socket socket = server.accept();
				nombreClients++;
				InetAddress UserAddress = socket.getInetAddress();
				int UserPort = socket.getPort();
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				String message = br.readLine();
				System.out.println("(UserAddress/Port) " + UserAddress + "/" + UserPort + " : " + message );
				pw.write("Bienvenue !\n");
				pw.flush();
				Messagerie m = new Messagerie(socket,pw);
				m.start();
				
				
			}
			
		}catch(Exception e){
			System.out.println(e);
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ServeurTCP server = new ServeurTCP();
		server.demarrer();
	}

}
