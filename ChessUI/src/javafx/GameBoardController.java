package javafx;

import com.sun.xml.internal.bind.v2.runtime.reflect.Lister;
import game.Board;
import game.Move;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import networking.OpCode;
import networking.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class GameBoardController implements Initializable {

	@FXML Button forfeitButton;
	@FXML Button submitMoveButton;
    @FXML GridPane gameBoard;
	private boolean otherPlayerQuit = false;
	private boolean weQuit = false;
	private Board boardState;
	private Service<Void> backgroundTask;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private int id;
	private Move move = null;
	final private Object lock = new Object();


    public void handleClick(MouseEvent e) {
    	Node source = (Node) e.getSource();
		//TODO Generate moveset here.
		//TODO Also pick which move and set the move member variable
    	System.out.println( source.getId() );
		if(otherPlayerQuit){
			try {
				Alert a = new Alert(Alert.AlertType.INFORMATION);
				a.setTitle("Warning!");
				a.setContentText("Other Player quit game! Returning to main menu");
				a.showAndWait();
				Stage getstage = (Stage) forfeitButton.getScene().getWindow();
				Parent root = FXMLLoader.load(getClass().getResource("MainMenu.fxml"));
				Scene scene = new Scene(root, 600, 400);
				scene.getStylesheets().add(getClass().getResource("MainMenu.css").toExternalForm());

				getstage.setScene(scene);
				getstage.show();
			}
			catch (IOException ex){
				ex.printStackTrace();
			}
		}
    }
    
	public void handleForfeitClick(){	
		try{
			weQuit = true;
			System.out.println("You clicked Forfeit");
			Stage getstage = (Stage) forfeitButton.getScene().getWindow();
			Parent root = FXMLLoader.load(getClass().getResource("MainMenu.fxml"));
			//tell server we're quitting.
			synchronized (lock){
				out.writeObject(new Packet(OpCode.QuitGame, id, null));
			}
			Scene scene = new Scene(root,600,400);
			scene.getStylesheets().add(getClass().getResource("MainMenu.css").toExternalForm());
			
			getstage.setScene(scene);
			getstage.show();
			in.close();
			out.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}
	
	public void handleSubmitMoveClick(){
		try{
			System.out.println("You clicked SubmitMove");
			//TODO send move object here.
			Stage getstage = (Stage) forfeitButton.getScene().getWindow();
			Parent root = FXMLLoader.load(getClass().getResource("MainMenu.fxml"));
			synchronized (lock) {
				//Send the move object to the server here
				out.writeObject(new Packet(OpCode.UpdateBoard, id, move));
				//Expected to get an OpCode.UpdatedBoard packet here.
				in.readObject();
			}
			Scene scene = new Scene(root,600,400);
			scene.getStylesheets().add(getClass().getResource("MainMenu.css").toExternalForm());
			
			getstage.setScene(scene);
			getstage.show();
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}
	public void setIn(ObjectInputStream in){
		this.in = in;
	}
	public void setOut(ObjectOutputStream out){ this.out = out; }
	public void setId(int id){ this.id = id; }
	public void processPacket(Packet p){
		try {
			switch (p.GetOpCode()) {
				case UpdateBoard:
					//The other player made a move and we need to update our board.
					synchronized (lock) {
						boardState.ApplyMove(p.GetMove());
					}
					break;
				case UpdatedBoard:
					//Response packet from server confirming that we updated the board
					break;
				case QuitGame:
					//Other player quit game
					System.out.println("Other player quit the game!");
					otherPlayerQuit = true;
					break;
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
	}
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println("Initialized");
		boardState = new Board();
		//Create background task to communicate with server
		backgroundTask = new Service<Void>() {
			@Override
			protected Task<Void> createTask() {

				return new Task<Void>(){

					@Override
					protected Void call() throws Exception {
							while (true) {
								System.out.println("asdfa");
								//TODO go back to main menu if other player quits. Also show win screen
								if (isCancelled() || otherPlayerQuit || weQuit)
									return null;
								try {
									Packet p = (Packet) in.readObject();
									processPacket(p);
								}
								catch (IOException ex) {
									ex.printStackTrace();
								}
								System.out.println("asdfa");
							}
					}
				};
			}
		};
		backgroundTask.setOnCancelled(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event)  {
				System.out.println("Handeled Cancel");
				//TODO put disconnect code here. Have to implement that on server first.
			}
		});
		//Close the dialog box and transition to the game board
		backgroundTask.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {}

		});
		backgroundTask.start();
	}
}

