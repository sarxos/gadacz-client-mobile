import java.io.IOException;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

public class Gadacz extends MIDlet implements CommandListener {

	public int PORT = 1007;		// domyslny port komunikacji
	public char EOM = '\n';		// domyslny znak konca wiadomosci
	
	public static final String COMM_HELLO 	= "hello";
	public static final String COMM_MSG 	= "msg";
	public static final String COMM_PRIV 	= "priv";
	public static final String COMM_KICK 	= "kick"; 
	
	/* NOTE!
	 * Oprocz powyzszych moga tez byc inne komendy gadacza, 
	 * np. "op", "deop", "kick" lub "ban".
	 */

	private final Command COMMAND_POLACZENIE = new Command("Polacz", Command.OK, 1);
	private final Command COMMAND_WYJSCIE = new Command("Wyjscie", Command.EXIT, 1);
	private final Command COMMAND_WYSLIJ_MSG = new Command("Wyslij", Command.OK, 1);
	
	private Display display = null;				// ekran komorki
	private Form formPolaczenie = null;			// forma z informacjami do polaczenia
	private Form formRozmowa = null;			// forma z chatem
	private TextField fieldServer = null;		// adres serwera
	private TextField fieldNickname = null;		// nick
	private TextField fieldWiadomosc = null;	// nowa wiadomosc
	private StringItem informacja = null;		// info
	private StringItem[] wiadomosci = null;		// ostatnie odebrane wiadomosci 
	private Alert alert = null;
	
	private GadaczThread watekPolaczenia = null;
	
	private int msgCount = 5;		// ile ma max przechowywac wiadomosci
	
	public Gadacz() {
		
		fieldServer = new TextField("Adres serwera", "localhost", 100, TextField.ANY);
		fieldServer.setInitialInputMode("MIDP_LOWERCASE_LATIN");
		fieldServer.setLayout(Item.LAYOUT_CENTER);
		
		fieldNickname = new TextField("Twoj nick", "johndoe", 100, TextField.ANY);
		fieldNickname.setInitialInputMode("MIDP_LOWERCASE_LATIN");
		fieldNickname.setLayout(Item.LAYOUT_CENTER);

		fieldWiadomosc = new TextField("", "", 100, TextField.ANY);
		fieldWiadomosc.setLayout(Item.LAYOUT_BOTTOM);

		informacja = new StringItem("Info:", "Aby sie zalogowac wcisnij Polacz.");
		informacja.setLayout(Item.LAYOUT_LEFT);

		formPolaczenie = new Form("Dane polaczenia");
		formPolaczenie.append(fieldServer);
		formPolaczenie.append(fieldNickname);
		formPolaczenie.append(informacja);

		formPolaczenie.addCommand(COMMAND_POLACZENIE);
		formPolaczenie.addCommand(COMMAND_WYJSCIE);
		
		formPolaczenie.setCommandListener(this);
		
		formRozmowa = new Form("Okno rozmowy");
		
		wiadomosci = new StringItem[msgCount];
		for(int i = 0; i < wiadomosci.length; i++) {	// tworzymy puste wiadomosci i dodajemy
			StringItem w = new StringItem("", ""); 
			w.setLayout(Item.LAYOUT_BOTTOM);
			wiadomosci[i] = w;
			formRozmowa.append(wiadomosci[i]);
		}

		formRozmowa.append(fieldWiadomosc);
		formRozmowa.addCommand(COMMAND_WYJSCIE);
		formRozmowa.addCommand(COMMAND_WYSLIJ_MSG);

		formRozmowa.setCommandListener(this);
	} 
	
	protected void startApp() throws MIDletStateChangeException {
		
		display = Display.getDisplay(this);
		display.setCurrent(formPolaczenie);
	}

	protected void pauseApp() {
	}

	protected void destroyApp(boolean uncondinental) throws MIDletStateChangeException {
		notifyDestroyed();
	}

	protected boolean checkLoginData() {
		String nick = fieldNickname.getString();
		String server = fieldServer.getString();
		if(nick.length() > 0 && server.length() > 0) {
			return true;
		}
		return false;
	}
	
	/**
	 * Metoda CommandListenera wywolywana gdy ktorys z Commands zostanie wykonany.
	 * @param c obiekt Command który zosta³ wykonany  
	 * @param component element wyœwietlany na którym zosta³ wykonany Command (formy)
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command c, Displayable component) {
		
		// KOMENDA POLACZ Z SERWEREM
		if(c.equals(COMMAND_POLACZENIE)) {
			
			boolean data_complete = checkLoginData();
			
			if(data_complete) {
				
				String nick = fieldNickname.getString();
				String adres = fieldServer.getString();

				// Jesli login bedzie ok to wtedy watek wywola na midlecie
				// metode logowania i rozpocznie sie rozmowa
				
				watekPolaczenia = new GadaczThread(nick, adres, this); 
				watekPolaczenia.start();
			}
		
		// KOMENDA WYSLIJ WIADOMOSC
		} else if(c.equals(COMMAND_WYSLIJ_MSG)) {
			
			try {
				
				watekPolaczenia.send("msg " + fieldWiadomosc.getString());
				fieldWiadomosc.setString("");
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		// KOMENDA WYJSCIE
		} else if(c.equals(COMMAND_WYJSCIE)) {
			
			try {
				destroyApp(true);
			} catch (MIDletStateChangeException e) {
				e.printStackTrace();
			}
		}
	}

	public void login() {
		display.setCurrent(formRozmowa);
	}
	
	public void logout() {
		
	}
	
	public void addMessage(String fromWhom, String msg) {
		for(int i = 0; i < wiadomosci.length - 1; i++) {
			StringItem tmp = wiadomosci[i + 1];
			wiadomosci[i].setLabel(tmp.getLabel());
			wiadomosci[i].setText(tmp.getText());
		}
		StringItem last = wiadomosci[wiadomosci.length - 1]; 
		last.setLabel(fromWhom + ": ");
		last.setText(msg);
	}
	
	public void showMessage(String title, String message) {
		alert = new Alert("");
		alert.setTimeout(5000);	// ms
		alert.setTitle(title);
		alert.setString(message);
		display.setCurrent(alert, display.getCurrent());
	}
}
