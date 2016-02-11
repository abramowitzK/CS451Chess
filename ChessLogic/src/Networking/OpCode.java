package Networking;

import java.io.Serializable;

/**
 * Created by Kyle on 2/1/2016.
 */
public enum OpCode implements Serializable{
    UpdateBoard,
    UpdatedBoard,
    JoinQueue,
    JoinedQueue,
    JoinGame,
    JoinedGame,
    QuitGame,
}
