import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.SocketConnection;

public class GadaczThread extends Thread {

	public static final int MSG_MAX_LENGTH = 1024; 
	
	private String nick = null;
	private String serverAddres = null;
	private Gadacz gadacz = null;
	private SocketConnection connection = null;
	private InputStream input = null;
	private OutputStream output = null;
	private boolean receiving = true;
	
	public GadaczThread(String nick, String serverAddres, Gadacz midlet) {
		this.nick = nick;
		this.serverAddres = serverAddres;
		this.gadacz = midlet;
	}
	
	public void run() {
		super.run();
		
		try {
			
			String adres = "socket://" + serverAddres + ":" + gadacz.PORT;
			// adres == np.: "socket://localhost:1007"
			
			connection = (SocketConnection) Connector.open(adres);
			output = connection.openOutputStream();
			input = connection.openInputStream();
			
			/* NOTE!
			 * Sekcja logowania do serwera. Wysylamy komende "hello" z naszym nickiem.
			 * Wyglada to np tak: "hello jacek83" - serwer rozpozna komende i zaloguje 
			 * nas albo zwroci komunikat ze nick taki juz jest zajety.
			 */
			
			String[] msg_table = null;
			
			send(Gadacz.COMM_HELLO + " " + nick);
			
			msg_table = split(receive());
			
			if(msg_table[1].equals("loginbusy")) {
				receiving = false;
				gadacz.showMessage(
						"Nick zajety!",
						"Podany nick jest juz uzywany. Wybierz inny nick."
				);
				try {
					join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return;
			} else {
				receiving = true;
				gadacz.login();
			}
			
			/* NOTE!
			 * Glowna petla odczytujaca.
			 */
			
			String msg = null;
			while(receiving) {
				msg = receive();
				msg_table = split(msg);
				if(msg_table[0] != null && msg_table[1] != null) { 
					gadacz.addMessage(msg_table[0], msg_table[1]);
				}
			}
			
		} catch (ConnectionNotFoundException e) {
			gadacz.showMessage(
					"Wystapil blad!", 
					"Nie odnaleziono serwera. Sprawdz czy adres jest wpisany poprawnie " +
					"lub czy serwer jest dostepny."
			);
		} catch (IOException e) {
			gadacz.showMessage("Wystapil blad!", e.getMessage());
		}
	}
	
	public void send(String message) throws IOException {
		char c;
		for(int i = 0; i < message.length(); i++) {
			c = message.charAt(i);	
			output.write(c);		// znak po znaku do strumienia
		}
		output.write(gadacz.EOM);	// na koniec znak "End Of Message"
		output.flush();
	}
	
	public String receive() throws IOException {
		StringBuffer sb = new StringBuffer();
		int i =0;
		char c = (char) input.read();
		while(c != -1 && c != gadacz.EOM) {
			sb.append(c);
			c = (char) input.read();	// znak po znaku ze strumienia
			if(++i >= MSG_MAX_LENGTH) {			// max 1024 znaki w wiadomosci
				sb.append("  ... [cut, message to long]");
				break;
			}
		}
		
		if(sb.length() > 0) {
			String msg = sb.toString();
			msg = msg.trim();
			System.out.println("odebrano: " + msg);
			return msg;
		} else {
			return null;
		}
	}
	
	protected String[] split(String s) {
		int space_pos = s.indexOf(": ");
		// ~User (priv): Wiadomosc
		// ~User: Wiadomosc
		String[] msg_table = new String[2];
		if(space_pos == -1) {
			msg_table[1] = s;
		} else {
			msg_table[0] = s.substring(0, space_pos);
			msg_table[1] = s.substring(space_pos + 2, s.length());
		}
		return msg_table;
	}
}
