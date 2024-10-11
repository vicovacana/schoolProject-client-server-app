package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class KlijentHendler extends Thread {

	private Socket dolazniSocket;

	public KlijentHendler(Socket dolazniSocket) { // postavlja da gore napravljen dolazni socket bude onaj koji smo
													// inicijalizovali negde drugde
		this.dolazniSocket = dolazniSocket;
	}

	BufferedReader klijentInput = null;
	PrintStream klijentOutput = null;

	@Override
	public void run() {
		try {
			klijentInput = new BufferedReader(new InputStreamReader(dolazniSocket.getInputStream()));
			klijentOutput = new PrintStream(dolazniSocket.getOutputStream());

			int choice = -1;
			while (choice != 0) {

				// UPLATA
				if (choice == 1) {
					String ime;
					String adresa;
					String brojKartice;
					int cvv;
					int iznos;

					klijentOutput.println("Izabrali ste opciju 1");

					klijentOutput.println("Unesite ime i prezime:");
					ime = klijentInput.readLine();

					klijentOutput.println("Unesite adresu:");
					adresa = klijentInput.readLine();

					klijentOutput.println("Unesite broj katice:");
					brojKartice = klijentInput.readLine();

					klijentOutput.println("Unesite CVV:");
					cvv = Integer.parseInt(klijentInput.readLine());

					if (!validnaKarticaICVV(brojKartice, cvv)) {

						klijentOutput.println("kartica se ne nalazi u bazi");
						break;
					}

					klijentOutput.println("Unesite iznos koji zelite da uplatite:");
					iznos = Integer.parseInt(klijentInput.readLine());

					if (iznos >= 200) {
						Calendar vremeUplate = new GregorianCalendar();

						int godina = vremeUplate.get(Calendar.YEAR);
						int mesec = vremeUplate.get(Calendar.MONTH) + 1; // Meseèni indeks kreæe od 0
						int dan = vremeUplate.get(Calendar.DAY_OF_MONTH);
						int sati = vremeUplate.get(Calendar.HOUR_OF_DAY);
						int minuti = vremeUplate.get(Calendar.MINUTE);
						int sekunde = vremeUplate.get(Calendar.SECOND);

						klijentOutput.println("Uspesna uplata");
						klijentOutput.println(ime);
						klijentOutput.println(adresa);
						klijentOutput.println(iznos);
						klijentOutput.println(dan+". "+mesec+". "+ godina+".  "+sati+":"+minuti+":"+sekunde);
						
						ubaciUplatu(ime, iznos, dan, mesec, godina, sati, minuti, sekunde);

						try (BufferedReader reader = new BufferedReader(new FileReader("stanje.txt"))) {
							String linija = reader.readLine();

							if (linija == null) {

								try (BufferedWriter writer = new BufferedWriter(new FileWriter("stanje.txt"))) {
									writer.write(String.valueOf(iznos));
								}
							} else {

								int stanje = Integer.parseInt(linija);
								stanje += iznos;

								try (BufferedWriter writer = new BufferedWriter(new FileWriter("stanje.txt"))) {
									writer.write(String.valueOf(stanje));
								}
							}
						} catch (IOException e) {
							System.out.println("Došlo je do greške pri upisivanju iznosa u fajl");
						}

					} else
						System.out.println("Iznos mora biti najmanje 200 dinara.");

				} else if (choice == 0) {
					klijentOutput.println("opcija 0");
					break;
				}

				// PROVERA STANJA
				if (choice == 2) {

					try (BufferedReader reader = new BufferedReader(new FileReader("stanje.txt"))) {
						String broj = reader.readLine();
						if (broj == null) {
							klijentOutput.println("Trenutno nije prikupljeno sredstava.");
						} else {
							int stanje = Integer.parseInt(broj);

							klijentOutput.println("Trenutno je skupljeno " + stanje + " dinara.");
						}
					} catch (IOException e) {
						System.out.println("Greska pri citanju skupljenih sredstava");
					}
				}

				// REGISTRACIJA
				if (choice == 3) {

					String ime;
					String username;
					String pass;
					String brojKartice;
					String jmbg;
					String email;

					klijentOutput.println("Izabrali ste opciju 3.");
					klijentOutput.println("Da biste se registrovali unesite sledece podatke:");

					klijentOutput.println("Unesite Vase ime i prezime:");
					ime = klijentInput.readLine();

					klijentOutput.println("Unesite Vas username:");
					username = klijentInput.readLine();
					if (postojiUsername(username)) {
						klijentOutput.println("Taj username vec postoji.");
						break;
					}

					klijentOutput.println("Unesite Vasu lozinku:");
					pass = klijentInput.readLine();

					klijentOutput.println("Unesite broj Vase kartice:");
					brojKartice = klijentInput.readLine();
					if (!validnaKartica(brojKartice)) {
						klijentOutput.println("Pogresna kartica");
					}

					klijentOutput.println("Unesite Vas JMBG:");
					jmbg = klijentInput.readLine();
					if (jmbg.length() != 13) {
						klijentOutput.println("Los jmbg");
						break;
					}

					klijentOutput.println("Unesite Vas email:");
					email = klijentInput.readLine();
					if (!email.contains("@")) {
						klijentOutput.println("Los email");
						break;
					}

					ubaciRegistraciju(ime, username, pass, brojKartice, jmbg, email);
					klijentOutput.println("Uspesno ste se registrovali!");

				}

				// PRIJAVA
				if (choice == 4) {
					String username;
					String pass;

					klijentOutput.println("Izabrali ste opciju 4.");
					klijentOutput.println("Da biste se prijavili unesite sledece podatke:");

					klijentOutput.println("Unesite Vas username:");
					username = klijentInput.readLine();

					klijentOutput.println("Unesite Vasu lozinku:");
					pass = klijentInput.readLine();

							if (prijava(username, pass)) {
								klijentOutput.println("Uspesno ste se prijavili!");
								int choice2;
								
								while(true) {
										
										klijentOutput.println("0. Napusti server");
										klijentOutput.println("1. Idi nazad");
										klijentOutput.println("2. Izvrsi ulatu");
										klijentOutput.println("3. Poveri iznos pikupljenih sredstava");
										klijentOutput.println("4. Pogledaj 10 poslednjih uplata");
				
										choice2 = Integer.parseInt(klijentInput.readLine());
				
										if (choice2 == 2) {
											String ime = null;
											String adresa = null;
											String brojKartice = null;
											int cvv;
											int iznos;
				
											klijentOutput.println("Izabrali ste opciju 1");
				
											klijentOutput.println("Unesite adresu:");
											adresa = klijentInput.readLine();
				
											klijentOutput.println("Unesite CVV:");
											cvv = Integer.parseInt(klijentInput.readLine());
				
											try (BufferedReader reader = new BufferedReader(new FileReader("registrovani.txt"))) {
												String red;
												while ((red = reader.readLine()) != null) {
				
													String[] s = red.split(" ");
				
													if (s[0].equals(username) && s[3].equals(pass)) {
				
														ime = s[1] + " " + s[2];
														brojKartice = s[4];
				
													}
												}
											} catch (Exception e) {
												System.out.println("greska pri uzimanju podataka iz baze registrovanih");
											}
				
											if (!validnaKarticaICVV(brojKartice, cvv)) {
												klijentOutput.println("pogresan cvv");
												break;
											}
				
											klijentOutput.println("Unesite iznos koji zelite da uplatite:");
											iznos = Integer.parseInt(klijentInput.readLine());
				
											if (iznos >= 200) {
												Calendar vremeUplate = new GregorianCalendar();
				
												int godina = vremeUplate.get(Calendar.YEAR);
												int mesec = vremeUplate.get(Calendar.MONTH) + 1; // Meseèni indeks kreæe od 0
												int dan = vremeUplate.get(Calendar.DAY_OF_MONTH);
												int sati = vremeUplate.get(Calendar.HOUR_OF_DAY);
												int minuti = vremeUplate.get(Calendar.MINUTE);
												int sekunde = vremeUplate.get(Calendar.SECOND);
				
												klijentOutput.println("Uspesno ste uplatili " + iznos + " dinara. " + "Vasi podaci: "
														+ (String) ime + " " + (String) adresa);
												klijentOutput.println("Datum: " + dan + "." + mesec + "." + godina + ".");
												klijentOutput.println("Vreme: " + sati + ":" + minuti + ":" + sekunde);
												
												ubaciUplatu(ime, iznos, dan, mesec, godina, sati, minuti, sekunde);
				
												try (BufferedReader reader = new BufferedReader(new FileReader("stanje.txt"))) {
													String linija = reader.readLine();
				
													if (linija == null) {
				
														try (BufferedWriter writer = new BufferedWriter(new FileWriter("stanje.txt"))) {
															writer.write(String.valueOf(iznos));
														}
													} else {
				
														int stanje = Integer.parseInt(linija);
														stanje += iznos;
				
														try (BufferedWriter writer = new BufferedWriter(new FileWriter("stanje.txt"))) {
															writer.write(String.valueOf(stanje));
														}
													}
												} catch (IOException e) {
													System.out.println("Došlo je do greške pri upisivanju iznosa u fajl");
												}
				
											} else
												System.out.println("Iznos mora biti najmanje 200 dinara.");
										}
				
										if (choice2 == 3) {
				
											try (BufferedReader reader = new BufferedReader(new FileReader("stanje.txt"))) {
												String broj = reader.readLine();
												if (broj == null) {
													klijentOutput.println("Trenutno nije prikupljeno sredstava.");
												} else {
													int stanje = Integer.parseInt(broj);
				
													klijentOutput.println("Trenutno je skupljeno " + stanje + " dinara.");
												}
											} catch (IOException e) {
												System.out.println("Greska pri citanju skupljenih sredstava");
											}
										}
										
										if(choice2 == 4) poslednjih10();
											
										
										if(choice2 == 1) {
											break;
										}
										
								}//while
							}//if

				}//prijava

				klijentOutput.println("#Dobrodosao na server#");
				klijentOutput.println("0. Napusti server");
				klijentOutput.println("1. Izvrsi ulatu");
				klijentOutput.println("2. Poveri iznos pikupljenih sredstava");
				klijentOutput.println("3. Registruj se");
				klijentOutput.println("4. Prijavi se");
				klijentOutput.println("##Unesi redni broj opcije koju zelis.##");
				choice = Integer.parseInt(klijentInput.readLine());

			}

		} catch (IOException e) {
			System.out.println("Klijent se diskonektovao");
		}

		klijentOutput.println("Dovidjenja!");

	}

	public boolean validnaKarticaICVV(String brojKartice, int cvv) {
		try (BufferedReader reader = new BufferedReader(new FileReader("kartice.txt"))) {
			String red;
			while ((red = reader.readLine()) != null) {
				String[] s = red.split(" ");

				if (s[0].equals(brojKartice) && Integer.parseInt(s[1]) == cvv) {
					return true;
				}

			}

		} catch (IOException e) {
			System.out.println("greska pri validiranju kartice i cvv");
		}
		return false;
	}

	public boolean validnaKartica(String brojKartice) {
		try (BufferedReader reader = new BufferedReader(new FileReader("kartice.txt"))) {
			String red;
			while ((red = reader.readLine()) != null) {
				String[] s = red.split(" ");

				if (s[0].equals(brojKartice)) {
					return true;
				}

			}

		} catch (IOException e) {
			System.out.println("greska pri validiranju kartice");
		}
		return false;
	}

	public boolean postojiUsername(String username) {

		try (BufferedReader reader = new BufferedReader(new FileReader("registrovani.txt"))) {

			String red;
			while ((red = reader.readLine()) != null) {

				String[] s = red.split(" ");
				if (s[0].equals(username)) {
					return true;
				}

			}

		} catch (Exception e) {
			System.out.println("Greska pri proveravanju postojanja username");
		}
		return false;

	}

	public void ubaciRegistraciju(String ime, String username, String pass, String brojKartice, String jmbg,
			String email) {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter("registrovani.txt", true))) {

			writer.write(username + " " + ime + " " + pass + " " + brojKartice + " " + jmbg + " " + email);
			writer.newLine();

		} catch (Exception e) {
			System.out.println("greska pri upisu registracije");
		}

	}

	public boolean prijava(String username, String pass) {

		try (BufferedReader reader = new BufferedReader(new FileReader("registrovani.txt"))) {
			String red;
			while ((red = reader.readLine()) != null) {

				String[] s = red.split(" ");

				if (s[0].equals(username) && s[3].equals(pass)) {

					return true;

				}
			}

		} catch (IOException e) {
			System.out.println("greska pri nalazenju lozinke i imena");
		}

		return false;

	}

	public void poslednjih10() {
		
		 try (BufferedReader reader = new BufferedReader(new FileReader("uplate.txt"))) {
	            ArrayList<String> linije = new ArrayList<>();
	            String linija;
	            while ((linija = reader.readLine()) != null) {
	                linije.add(linija);
	                if (linije.size() > 10) {
	                    linije.remove(0); 
	                }
	            }

	            
	            for (int i = linije.size() - 1; i >= 0; i--) {
	                String trenutnaLinija = linije.get(i);
	                klijentOutput.println(trenutnaLinija);
	            }

	        } catch (IOException e) {
	            System.out.println("greska pri citanju uplata");
	        }
	}
	
	public void ubaciUplatu(String ime, int iznos, int dan, int mesec, int godina, int sati, int minuti, int sekunde) {
		
		try(BufferedWriter writer = new BufferedWriter(new FileWriter("uplate.txt"))) {
			
			writer.write(ime + " " + dan + "." + mesec + "." + godina + ". " + sati + ":" + minuti + ":" + sekunde + " " + iznos);
			writer.newLine();
			
		} catch (Exception e) {
			System.out.println("greska pri unosu uplate");
		}
	}
	
	
}
