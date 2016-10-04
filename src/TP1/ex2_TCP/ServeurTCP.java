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
	int port = 1027;

	class Messagerie extends Thread {
		Socket socketClient;
		PrintWriter pw;

		public Messagerie(Socket socketClient, PrintWriter pw) {
			this.socketClient = socketClient;
			this.pw = pw;
		}

		public void run() {
			try {
				while (true) {
					pw.write("Nombre de clients: " + nombreClients + "\n");
					pw.flush();
					Thread.sleep(10000);
				}
			} catch (InterruptedException e) {
				nombreClients--;
				e.printStackTrace();
			}
		}
	}

	public void demarrer() {
		try (ServerSocket server = new ServerSocket(port)) {
			
			while (true) {
				Socket socket = server.accept();
				nombreClients++;
				InetAddress userAddress = socket.getInetAddress();
				int userPort = socket.getPort();
				BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
				String message = br.readLine();
				System.out.println("(UserAddress/Port) " + userAddress + "/" + userPort + " : " + message);
				pw.write("Bienvenue !\n");
				pw.flush();
				Messagerie m = new Messagerie(socket, pw);
				m.start();
			}
		} catch (IllegalArgumentException e) {
			System.err.println("Valeur de port invalide, doit être entre 0 et 65535");
		} catch (IOException e) {
			System.err.println("Problème de traitement de la socket : port " + port);
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ServeurTCP server = new ServeurTCP();
		server.demarrer();
	}

}
