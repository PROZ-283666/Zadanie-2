package client;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {
	
	@Override
	public void start(Stage primaryStage) {
		try {	
			FXMLLoader fxmlLoader = 
				  new FXMLLoader(Main.class.getResource("OkienkoKlienta.fxml"));
			AnchorPane root = fxmlLoader.load();
			Scene scene = new Scene(root);
			primaryStage.setScene(scene);
			primaryStage.setTitle("JavaFX Web Socket Client");
			primaryStage.setOnHiding( e -> primaryStage_Hiding(e, fxmlLoader));
			primaryStage.show();
		} 
		catch(Exception e) { e.printStackTrace(); }
	}
	
    public static void main(String[] args) {
        launch(args);
    }
	
	private void primaryStage_Hiding(WindowEvent e, FXMLLoader fxmlLoader) {
		
		((WebSocketController) fxmlLoader.getController())
		.closeSession(new CloseReason(CloseCodes.NORMAL_CLOSURE, "Stage is hiding"));
	}
}
