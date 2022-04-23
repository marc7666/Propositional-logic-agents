package apryraz.eworld;

/**
 * @author Dand Marbà Sera
 * @author Marc Cervera Rosell
 **/

public class Position {
    /**
     *
     **/
    public int x, y;

    public Position(int row, int col) {
        x = row;
        y = col;
    }

    /**
     * This function converts the pair (x,y) of the position, to the linear equivalent.
     *
     * @param worldDim the dimension of world
     * @return lineal Position of the pair (x,y)
     **/
    public int toLinealPosition(int worldDim) {
        return ((y - 1) * worldDim) + (x - 1);
    }

    /**
     * This fuction convert lineal position of world
     *
     * @param lineal   identifier of the variable
     * @param worldDim world dimension
     * @return Position object with pair x,y of world linear position
     **/
    public static Position linealToCoord(int lineal, int worldDim) {
        lineal = lineal + 1;
        int x = ((lineal - 1) % worldDim) + 1;
        int y = (lineal - 1) / worldDim + 1;
        return new Position(x, y);
    }

    /**
     * This function converts the pair (x,y), to the linear equivalent.
     *
     * @param x        the x position
     * @param y        the y position
     * @param worldDim the dimension of world
     * @return lineal position of the pair (x,y)
     */
    public static int toLinealPosition(int x, int y, int worldDim) {
        return ((y - 1) * worldDim) + (x - 1);
    }
}
