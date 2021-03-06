package Pieces;

import Game.Position;
import javafx.scene.image.Image;

public final class Piece {
    public static Position[] KnightDirs() {
        return new Position[]{
                new Position(-1, 2),
                new Position(1, 2),
                new Position(2, 1),
                new Position(2, -1),
                new Position(1, -2),
                new Position(-1, -2),
                new Position(-2, 1),
                new Position(-2, -1)
        };
    }
    public static Position[] BishopDirs() {
            return new Position[]{
                    new Position(1,1),
                    new Position(1,-1),
                    new Position(-1,1),
                    new Position(-1,-1),
            };
        }
    public  static Position[] RookDirs() {
            return new Position[]{
                    new Position(0,1),
                    new Position(0,-1),
                    new Position(1, 0),
                    new Position(-1,0),
            };
        }
    public static Position[] QueenKingDirs() {
        return new Position[]{
                new Position(1,1),
                new Position(1,-1),
                new Position(-1,1),
                new Position(-1,-1),
                new Position(0,1),
                new Position(0,-1),
                new Position(1, 0),
                new Position(-1,0),
        };
    }
    public final Image PieceImage;
    public final Color PieceColor;
    public final PieceType Type;
    private boolean m_hasMoved;
    public Piece(PieceType type, Color color){
        Type = type;
        if(type != PieceType.Empty) {
            PieceImage = new ImageLoader(color).LoadPiece(type);
            PieceColor = color;
        } else {
            PieceImage = null;
            PieceColor = Color.Empty;
        }
    }
    public boolean HasMoved(){
        return m_hasMoved;
    }
    public void SetHasMoved(){
        m_hasMoved = true;
    }
    public void UnsetHasMoved(){ m_hasMoved = false;}
    @Override
    public String toString(){
        return Type.name() + " " + PieceColor.name() + " ";
    }

}
