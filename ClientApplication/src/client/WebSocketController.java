package client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;

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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
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
	@FXML ChoiceBox<String> fileChoiceBox;
	
	private ArrayList<FileInformation> filesSent = new ArrayList<>();
	private final int MAX_FILE_LENGHT = 2000000;
	
	private String user;
	private WebSocketClient webSocketClient;
	
	@FXML private void initialize() {
		webSocketClient = new WebSocketClient();
		user = userTextField.getText();
		btnSend.setDisable(true);
		btnAttach.setDisable(true);
		btnSave.setDisable(true);
		chatTextArea.setEditable(false);
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
	
	
	private void getTooLargeFileAlert() {
		Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("File is too large");
		alert.setHeaderText("A file you tried to attach was too large - the limit is 2MB.");
		alert.setContentText("Larger files should be sent as a link to a cloud.\nTry to compress it or send it this way.");
		alert.showAndWait();
	}
	
	private void getInvalidPathAlert(IOException e) {
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("INVALID PATH TO FILE");
		alert.setHeaderText("Attaching file generated the error");
		alert.setContentText("Error: " + e);
		alert.showAndWait();
	}
	
	@FXML private void btnAttach_Click() {		
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open Resource File");
		File selectedFile = fileChooser.showOpenDialog(null);
		
		if(selectedFile != null)
		{
			if(selectedFile.length() > MAX_FILE_LENGHT)
			{
				getTooLargeFileAlert();
				return;
			}
			
			//file lenght is ok
			try {
				ChatMessage msg = new ChatMessage(user, "file");
				String n = selectedFile.getName();
				msg.setText(n);
				String bytes = Base64.getEncoder().encodeToString(Files.readAllBytes(selectedFile.toPath()));
				msg.setData(bytes);
				JsonObject toSend =  getJson(msg);
				webSocketClient.sendMessage(toSend.toString());
			} catch (IOException e) {
				getInvalidPathAlert(e);
			}	
		}
	}
	
	FileInformation getFileInformationByDateAndFileName(String date, String name) {
		 for (FileInformation f : filesSent) {
		        if (f.getSentDate().equals(date) && f.getFileName().equals(name)) {
		            return f;
		        }
		    }
		    return null; 
	}
	
	@FXML private void btnSave_Click() {
		if(fileChoiceBox.getSelectionModel().isEmpty())
			return;
		
		// getting the proper sentDate and filename from fileChoiceBox
		String fileNameWithDate = fileChoiceBox.getSelectionModel().getSelectedItem().toString();
		int lastIndexOf =fileNameWithDate.lastIndexOf('(');
		String lastFileName = fileNameWithDate.substring(0, lastIndexOf - 1);
		String lastFileDate = fileNameWithDate.substring(lastIndexOf + 1, fileNameWithDate.length() - 1);
		
		FileInformation fileToSave = getFileInformationByDateAndFileName(lastFileDate, lastFileName);
		
		// saving file
		if(fileToSave != null) {
			FileChooser fileChooser = new FileChooser();
			String title = "Save file \"" + lastFileName +"\" from " + fileToSave.getUser();
			fileChooser.setTitle(title);
			fileChooser.setInitialFileName(lastFileName);
			File file = fileChooser.showSaveDialog(null);
				if(file != null){
					try {
					FileOutputStream fileOutputStream = null;
					fileOutputStream = new FileOutputStream(file);
					byte[] data = fileToSave.getData();
					byte[] decodedData = Base64.getDecoder().decode(data);
					fileOutputStream.write(decodedData);
					fileOutputStream.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}		
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
			//System.out.println("Message was received");
			JsonReader reader = Json.createReader(new StringReader(message));
			JsonObject inc = reader.readObject();
			String who = inc.getString("who");
			String type = inc.getString("type");
			String text = inc.getString("text");
			String data = inc.getString("data");
			if(type.equals("message")) {
				chatTextArea.setText(chatTextArea.getText() + who + ":" + text + "\n");
			}
			else {
				chatTextArea.setText(chatTextArea.getText() + "Użytkownik " + who + " wysłał plik " + text + "\n");
				FileInformation sentFile = new FileInformation();
				sentFile.setData(data.getBytes());
				sentFile.setUser(who);
				sentFile.setFileName(text);
				
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				sentFile.setSentDate(dateFormat.format(date));
				filesSent.add(sentFile);
				
				fileChoiceBox.getItems().add(text + " (" + dateFormat.format(date) + ")");
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
				//System.out.println("Message was sent: " + message);
				session.getBasicRemote().sendText(message);
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	} // public class WebSocketClient
	
} // public class WebSocketChatStageControler

