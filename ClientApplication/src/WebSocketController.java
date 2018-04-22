import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.util.Base64;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;

public class WebSocketController{
	@FXML TextField userTextField;
	@FXML TextArea chatTextArea;
	@FXML TextField messageTextField;
	@FXML Button btnSet;
	@FXML Button btnSend;
	@FXML Button btnAttach;
	@FXML Button btnSave;
	
	private String user;
	private String lastFileName;
	private String lastFileUser;
	private String lastFileData;
	private WebSocketClient webSocketClient;
	
	@FXML private void initialize() {
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		btnSend.setDisable(true);
		btnAttach.setDisable(true);
		btnSave.setDisable(true);
	}
	
	@FXML private void btnSet_Click() {
		if (userTextField.getText().isEmpty()) { return; }
		user = userTextField.getText();
		btnSend.setDisable(false);
		btnAttach.setDisable(false);
		btnSave.setDisable(false);
	}
	
	@FXML private void messageTextField_OnKeyPressed(KeyEvent ke) {
		if(ke.getCode() == KeyCode.ENTER) {
			btnSend_Click();
		}
	}
	
	JsonObject getJson(ChatMessage msg) {
		return 		Json.createObjectBuilder()
				   .add("who", msg.getWho())
				   .add("type", msg.getType())
				   .add("text", msg.getText())
				   .add("data", msg.getData())
				   .build();
	}
	
	@FXML private void btnSend_Click() {
		ChatMessage msg = new ChatMessage(user, "message");
		msg.setText(messageTextField.getText());
		msg.setData("");
		JsonObject toSend =  getJson(msg);
		webSocketClient.sendMessage(toSend.toString());
		messageTextField.setText(null);
	}
	
	@FXML private void btnAttach_getPromptText() {
		messageTextField.setPromptText("click Attach to send a file");
	}
	
	@FXML private void btnAttach_deletePromptText() {
		messageTextField.setPromptText(null);
	}
	
	@FXML private void btnAttach_Click() {		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		File selectedFile = fileChooser.showOpenDialog(null);
		
		if(selectedFile != null)
		{	
			try {
				ChatMessage msg = new ChatMessage(user, "file");
				String n = selectedFile.getName();
				msg.setText(n);
				String bytes = Base64.getEncoder().encodeToString(Files.readAllBytes(selectedFile.toPath()));
				msg.setData(bytes);
				JsonObject toSend =  getJson(msg);
				webSocketClient.sendMessage(toSend.toString());
			} catch (IOException e) {
				System.out.println("INVALID PATH TO FILE");
			}	
		}
	}
	
	@FXML private void btnSave_Click() {
		if(lastFileName == null)
			return;
		FileChooser fileChooser = new FileChooser();
		String title = "Save file \"" + lastFileName +"\" from " + lastFileUser;
		fileChooser.setTitle(title);
		fileChooser.setInitialFileName(lastFileName);
		File file = fileChooser.showSaveDialog(null);
		if(file != null) {
			FileOutputStream fileOutputStream = null;
			try {
				fileOutputStream = new FileOutputStream(file);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			byte[] data = lastFileData.getBytes();
			byte[] decodedData = Base64.getDecoder().decode(data);
			try {
				fileOutputStream.write(decodedData);
				fileOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public void closeSession(CloseReason closeReason) {
		try {
			webSocketClient.session.close(closeReason);
		}
		catch (IOException e) { 
			e.printStackTrace(); } 
	}
	
	@ClientEndpoint
	public class WebSocketClient {
		private Session session;
		
		public WebSocketClient() {
			connectToWebSocket(); 
		}
		
		@OnOpen public void onOpen(Session session) {
			System.out.println("Connection is opened.");
			this.session = session;
		}
		
		@OnClose public void onClose(CloseReason closeReason) {
			System.out.println("Connection is closed: " + closeReason.getReasonPhrase());
		}
		
		@OnError public void onError(Throwable throwable) {
			System.out.println("Error occured");
			throwable.printStackTrace();
		}
		@OnMessage public void onMessage(String message, Session session) {
			System.out.println("Message was received");
			System.out.println(message);
			JsonReader reader = Json.createReader(new StringReader(message));
			JsonObject inc = reader.readObject();
			String who = inc.getString("who");
			String type = inc.getString("type");
			String text = inc.getString("text");
			String data = inc.getString("data");
			System.out.println(who + " " + type);
			if(type.equals("message")) {
				chatTextArea.setText(chatTextArea.getText() + who + ":" + text + "\n");
			}
			else {
				chatTextArea.setText(chatTextArea.getText() + "Użytkownik " + who + " wysłał plik " + text + "\n");
				lastFileName = text;
				lastFileUser = who;
				lastFileData = data;
			}
		}
		
		
		private void connectToWebSocket() {
			WebSocketContainer webSocketContainer =
			ContainerProvider.getWebSocketContainer();
			try {  
				URI uri = URI.create("ws://localhost:8080/WebSocket/websocketendpoint");
				webSocketContainer.connectToServer(this, uri);
			} 
			catch (DeploymentException | IOException e) { 
				e.printStackTrace(); }
		}
		
		public void sendMessage(String message) {
			try {
				System.out.println("Message was sent: " + message);
				session.getBasicRemote().sendText(message);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	} // public class WebSocketClient
	
} // public class WebSocketChatStageControler

