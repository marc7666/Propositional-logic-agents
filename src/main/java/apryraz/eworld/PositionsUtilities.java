package apryraz.eworld;

import java.util.ArrayList;
import java.util.List;

/**
 * @author dandmarbasera on 22/4/22.
 * @project Propositional-logic-agents
 */
public class PositionsUtilities {
    /**
     * World Dimension
     */
    int worldDim;

    /**
     * Constructor of the class
     *
     * @param worldDim informs the utility which is the dimension of the world
     */
    public PositionsUtilities(int worldDim) {
        this.worldDim = worldDim;
    }

    /**
     * This function checks if the pair (x, y) is within the limits
     *
     * @param x cord x of position
     * @param y cord y of position
     * @return true if the pair (x,y) is within the limits, false in other case
     */
    public boolean withinLimits( int x, int y ) {
        return ( x >= 1 && x <= worldDim && y >= 1 && y <= worldDim);
    }

    /**
     * This function get possible positions of sensor value 1 from a position
     *
     * @param linealPosition position origin of sensor lecture
     * @return list of possible positions
     */
    public List<Integer> positionsOfValue1(int linealPosition) {
        Position position =  Position.linealToCoord(linealPosition, worldDim);
        ArrayList<Integer> posiblePositions = new ArrayList<>();

        if(withinLimits(position.x + 1, position.y))
            posiblePositions.add(Position.toLinealPosition(position.x + 1, position.y, worldDim));

        if(withinLimits(position.x - 1, position.y))
            posiblePositions.add(Position.toLinealPosition(position.x - 1, position.y, worldDim));

        if(withinLimits(position.x, position.y - 1))
            posiblePositions.add(Position.toLinealPosition(position.x, position.y - 1, worldDim));

        if(withinLimits(position.x, position.y + 1))
            posiblePositions.add(Position.toLinealPosition(position.x, position.y + 1, worldDim));

        return posiblePositions;
    }

    /**
     * This function get possible positions of sensor value 2 from a position
     *
     * @param linealPosition position origin of sensor lecture
     * @return list of possible positions
     */
    public List<Integer> positionsOfValue2(int linealPosition) {
        Position position =  Position.linealToCoord(linealPosition, worldDim);
        ArrayList<Integer> posiblePositions = new ArrayList<>();

        if(withinLimits(position.x - 1, position.y - 1))
            posiblePositions.add(Position.toLinealPosition(position.x - 1, position.y - 1, worldDim));

        if(withinLimits(position.x + 1, position.y - 1))
            posiblePositions.add(Position.toLinealPosition(position.x + 1, position.y - 1, worldDim));

        if(withinLimits(position.x - 1, position.y + 1))
            posiblePositions.add(Position.toLinealPosition(position.x - 1, position.y + 1, worldDim));

        if(withinLimits(position.x + 1, position.y + 1))
            posiblePositions.add(Position.toLinealPosition(position.x + 1, position.y + 1, worldDim));

        return posiblePositions;
    }


    /**
     * This function get possible positions of sensor value 5 from a position
     *
     * @param linealPosition position origin of sensor lecture
     * @return list of possible positions
     */
    public List<Integer> positionsOfValue3(int linealPosition) {
        Position position =  Position.linealToCoord(linealPosition, worldDim);
        ArrayList<Integer> posiblePositions = new ArrayList<>();
        if(withinLimits(position.x, position.y))
            posiblePositions.add(linealPosition);
        return posiblePositions;
    }

}
