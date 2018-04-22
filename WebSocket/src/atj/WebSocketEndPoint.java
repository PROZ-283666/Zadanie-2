package atj;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;


//adnotacja deklaruje klasę gniazda serwerowego
//w kontekście aplikacji
//i adres URI używany przez klientów do komunikacji
@ApplicationScoped
@ServerEndpoint("/websocketendpoint")
public class WebSocketEndPoint {
	
	//adnotacja metody, która będzie wołana
	//przy każdym nawiązaniu połączenia przez klienta
	@OnOpen
	public void onOpen(Session session) { }
	
	//adnotacja metody, która będzie wołana
	//przy każdym zamknięciu połączenia przez klienta
	@OnClose
	public void onClose(Session session) { }
	
	//adnotacja metody, która będzie wołana po wystąpieniu błędu
	@OnError
	public void onError(Throwable error) { }
	
	//adnotacja metody, która będzie wołana po każdym odbiorze wiadomości
	@OnMessage
	public void onMessage(String message, Session session) {
	//rozgłoszenie otrzymanej wiadomości
		//do wszystkich podłączonych klientów
		try {
			for (Session oneSession : session.getOpenSessions()) {
				if (oneSession.isOpen()) {
					oneSession.getBasicRemote().sendText(message);
				}
			}
		}
		catch (IOException e) { e.printStackTrace(); }
	}
	
} // public class WebSocketEndpoint