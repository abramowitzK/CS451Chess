package Game;

import java.net.Socket;

/**
 * Created by Kyle on 2/1/2016.
 */
public class Player {
    private int m_id;
    private Socket m_socket;
    private boolean m_isBlack;
    public Player(Socket socket, int id){

    }

    public int GetID(){
        return m_id;
    }
    public Socket GetSocket(){
        return m_socket;
    }


}
