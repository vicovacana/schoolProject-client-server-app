package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.attribute.AclEntry;
import java.util.ArrayList;

public class Server {

	static ArrayList<KlijentHendler> klijenti = new ArrayList<>();

	public static void main(String[] args) {
		int port = 9555;
		Socket klijentSocket = null;
		try {
			ServerSocket serverSocket = new ServerSocket(port);

			while (true) {
				System.out.println("cekanje konekcije");

				klijentSocket = serverSocket.accept();

				System.out.println("konekcija uspostavljena");

				KlijentHendler klijent = new KlijentHendler(klijentSocket);
				klijenti.add(klijent);
				klijent.start();

			}
		} catch (IOException e) {
			System.out.println("greska pri konekciji");
		}
	}

}
