package klijent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Klijent implements Runnable{

	private static Socket klijentSocket = null;
	static BufferedReader ulazniTokOdServera = null;
    static PrintStream izlazniTokKaServeru = null;
    static BufferedReader ulazSaKonzole = null;
	
	public static void main(String[] args) {
		
		int port = 9555;
		
		try {
			klijentSocket = new Socket("localhost", port);
			ulazSaKonzole = new BufferedReader(new InputStreamReader(System.in));
            ulazniTokOdServera = new BufferedReader(new InputStreamReader(klijentSocket.getInputStream()));
            izlazniTokKaServeru = new PrintStream(klijentSocket.getOutputStream());

            
			new Thread(new Klijent()).start();
			String input;
			
			while(true) {
				input = ulazniTokOdServera.readLine();
				
				if(input.equals("Uspesna uplata")) {
					String name = ulazniTokOdServera.readLine();
					String address = ulazniTokOdServera.readLine();
					String amount = ulazniTokOdServera.readLine();
					String dtf = ulazniTokOdServera.readLine();
					String fileName = "src/" + name.split(" ")[0].toLowerCase() + "_" + name.split(" ")[1].toLowerCase() + ".txt";
					
					PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(fileName)));
					writer.println("Ime i prezime: " + name);
					writer.println("Adresa: " + address);
					writer.println("Datum i vreme: " + dtf);
					writer.println("Iznos: " + amount + " RSD");
					writer.close();
				}
				
				System.out.println(input);
				
				if(input.equals("Dovidjenja.")) break;
			}
			
			klijentSocket.close();
			
			
		} catch (IOException e) {
			System.out.println("Greska pri komunikaciji sa serverom.");
			System.exit(0);
		}
		
		
		
		
	}
	
	
	@Override
	public void run() {
		String poruka;
		
		while(true) {
			try {
				poruka = ulazSaKonzole.readLine();
				izlazniTokKaServeru.println(poruka);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	
}


