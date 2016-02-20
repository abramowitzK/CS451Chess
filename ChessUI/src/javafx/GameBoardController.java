package javafx;

import Game.Board;
import Game.Move;
import Game.Position;
import Pieces.Color;
import Pieces.Piece;
import Pieces.PieceType;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import Networking.OpCode;
import Networking.Packet;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.SynchronousQueue;

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
	private Color m_color;
	final private Object lock = new Object();
	private Piece m_selectedPiece = null;
	private Position m_oldPosition = null;
	private Position m_newPosition = null;
	private ArrayList<Position> m_validMoves = null;
	private boolean m_hasMoved;

    public void handleClick(MouseEvent e) {
		if(m_hasMoved)
			return;
		ImageView view = (ImageView)e.getSource();
		int i = GridPane.getRowIndex(view);
		int j = GridPane.getColumnIndex(view);
		Piece p = boardState.GetPiece(i,j);
		if(p != null && p != m_selectedPiece && p.PieceColor == m_color){
			//We selected a new non null piece that we own.
			RemoveColoring();
			m_selectedPiece = p;
			m_oldPosition = new Position(i,j);
		}
		else if(m_selectedPiece != null && (p == null || p.PieceColor != m_color)){
			// We have a piece selected and we want to move it
			if(ListContainsPosition(i,j, m_validMoves)){
				m_newPosition = new Position(i, j);
				boardState.SetPiece(i, j, m_selectedPiece);
				boardState.SetPiece(m_oldPosition.GetX(), m_oldPosition.GetY(), null);
				UpdateImagesFromBoardState();
				RemoveColoring();
				m_hasMoved = true;
			}
		}
		if(null != p && p.PieceColor == m_color) {
			m_validMoves = boardState.GetValidMoves(i, j);
			if (m_validMoves != null)
			{
				for (int k = 0; k < m_validMoves.size(); k++)
				{
					ColorRegion(m_validMoves.get(k).GetX(), m_validMoves.get(k).GetY());
				}
			}
		}
	}
    
	public void handleForfeitClick(){	
		try{
			weQuit = true;
			System.out.println("You clicked Forfeit");
			Stage getstage = (Stage) forfeitButton.getScene().getWindow();
			Parent root = FXMLLoader.load(getClass().getResource("MainMenu.fxml"));
			//tell Server we're quitting.

			Scene scene = new Scene(root,600,400);
			scene.getStylesheets().add(getClass().getResource("MainMenu.css").toExternalForm());
			
			getstage.setScene(scene);
			getstage.show();
		}
		catch (Exception e){
			e.printStackTrace();
		}

	}
	public void handleReset(){
		boardState.SetPiece(m_oldPosition.GetX(), m_oldPosition.GetY(), m_selectedPiece);
		boardState.SetPiece(m_newPosition.GetX(),m_newPosition.GetY(), null);
		m_hasMoved = false;
		m_selectedPiece = null;
		m_newPosition = null;
		m_oldPosition = null;
		UpdateImagesFromBoardState();
	}
	public void handleSubmitMoveClick(){
		try{
			System.out.println("You clicked SubmitMove");
			//TODO send move object here.
			if(m_hasMoved)
			{
				Move move = new Move(m_oldPosition, m_newPosition);
				synchronized (lock)
				{
					//Send the move object to the Server here
					out.writeObject(new Packet(OpCode.UpdateBoard, id, move));
					//Expected to get an OpCode.UpdatedBoard packet here.
					in.readObject();
				}
				m_hasMoved = false;
			}
		}
		catch (SocketException ex){
			System.out.println("Socket closed");
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
	public void setColor(Color color){ this.m_color = color;}
	public void processPacket(Packet p){
		try {
			switch (p.GetOpCode()) {
				case UpdateBoard:
					//The other player made a move and we need to update our board.
					synchronized (lock) {
						boardState.ApplyMove(p.GetMove());
					}
					Platform.runLater(() -> UpdateImagesFromBoardState());
					break;
				case UpdatedBoard:
					//Response packet from Server confirming that we updated the board
					break;
				case QuitGame:
					//Other player quit Game
					System.out.println("Other player quit the Game!");
					out.writeObject(new Packet(OpCode.QuitGame, id, null));
					otherPlayerQuit = true;
					Platform.runLater(()-> HandleOtherPlayerQuit());

					break;
			}
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
	}
	private void HandleOtherPlayerQuit(){
		Alert A = new Alert(Alert.AlertType.ERROR, "Other player quit!", ButtonType.FINISH);
		A.showAndWait();
		Stage getstage = (Stage) forfeitButton.getScene().getWindow();
		try
		{
			Parent root = FXMLLoader.load(getClass().getResource("MainMenu.fxml"));
			Scene scene = new Scene(root,600,400);
			scene.getStylesheets().add(getClass().getResource("MainMenu.css").toExternalForm());

			getstage.setScene(scene);
			getstage.show();
			in.close();
			out.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

	}
	private Node GetByRowColumn(int i, int j){
		for(Node n : gameBoard.getChildren()){
			if(GridPane.getRowIndex(n) == i && GridPane.getColumnIndex(n) == j){
				return n;
			}
		}
		return null;
	}
	private ImageView GetImageView(int i, int j){
		return (ImageView)gameBoard.lookup("#"+i+j);
	}
	private Region GetRegion(int i, int j){
		for(Node node : gameBoard.getChildren()){
			if(gameBoard.getRowIndex(node) == i && gameBoard.getColumnIndex(node)== j)
				return (Region)node;
		}
		return null;
	}
	private void ColorRegion(int i, int j){
		Region r = GetRegion(i,j);
		r.setStyle("-fx-background-color:yellow");
	}
	private void UpdateImagesFromBoardState(){
		for(int i = 0; i < 8;  i++){
			for(int j = 0; j < 8; j++){
				if(boardState.GetPiece(i,j) != null)
					GetImageView(i,j).setImage(boardState.GetPiece(i,j).PieceImage);
				else
					GetImageView(i,j).setImage(null);
			}
		}
	}
	private boolean ListContainsPosition(int i, int j, ArrayList<Position> list){
		for(Position p : list){
			if(p.GetX() == i && p.GetY() == j)
				return true;
		}
		return false;
	}
	void RemoveColoring(){
		for(int i = 0; i < 8; i++){
			for(int j = 0; j < 8; j++){
				Region r = GetRegion(i,j);
				String color;
				if(r.getId().equals("light"))
					color = "wheat";
				else
					color = "peru";
				r.setStyle("-fx-background-color:" +  color);
			}
		}
	}
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		System.out.println("Initializing...");
		boardState = new Board();
		//Create background task to communicate with Server
		UpdateImagesFromBoardState();
		backgroundTask = new Service<Void>() {
			@Override
			protected Task<Void> createTask() {

				return new Task<Void>(){

					@Override
					protected Void call() throws Exception {
							while (true) {
								System.out.println("asdfa");
								//TODO go back to main menu if other player quits. Also show win screen
								if (isCancelled() || otherPlayerQuit || weQuit) {
									System.out.println("Quitting");
									return null;
								}
								try {
									Packet p = (Packet) in.readObject();
									processPacket(p);
								}
								catch (SocketTimeoutException ex){
									//This is okay
								}
								catch (EOFException ex) {
									return null;
								}
								catch (IOException ex) {
									ex.printStackTrace();
								}
							}
					}
				};
			}
		};
		backgroundTask.setOnCancelled(event -> {
            System.out.println("Handeled Cancel");
            //TODO put disconnect code here. Have to implement that on Server first.
        });
		//Close the dialog box and transition to the Game board
		backgroundTask.setOnSucceeded(event -> {
				try {
					System.out.println("Informing server that we quit");
					out.writeObject(new Packet(OpCode.QuitGame, id, null));
				} catch (IOException ex) {
					System.out.println("failed to inform server we quit. Panic!");
				}
		});
		backgroundTask.start();
	}
}

