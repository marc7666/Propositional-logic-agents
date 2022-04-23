package apryraz.eworld;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;

/**
 * @author Dand Marbà Sera
 * @author Marc Cervera Rosell
 **/

public class EnvelopeWorldEnv {
    /**
     * world dimension
     **/
    int WorldDim;
    int worldDims;

    PositionsUtilities positionsUtilities;
    List<Position> envelopePositions;

    /**
     * Class constructor
     *
     * @param dim          dimension of the world
     * @param envelopeFile File with list of envelopes locations
     **/
    public EnvelopeWorldEnv(int dim, String envelopeFile) {

        WorldDim = dim;
        worldDims = dim * dim;
        loadEnvelopeLocations(envelopeFile);
        positionsUtilities = new PositionsUtilities(worldDims);
    }

    /**
     * Load the list of pirates locations
     *
     * @param: name of the file that should contain a
     * set of envelope locations in a single line.
     **/
    public void loadEnvelopeLocations(String envelopeFile) {
        String[] envelopesList;
        String envelopes = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(envelopeFile));
            System.out.println("ENVELOPE FILE OPENED ...");
            envelopes = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        envelopesList = envelopes.split(" ");
        envelopePositions = new ArrayList<Position>(envelopesList.length);
        for (int i = 0; i < envelopesList.length; i++) {
            String[] coords = envelopesList[i].split(",");
            envelopePositions.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }

    }


    /**
     * Process a message received by the EFinder agent,
     * by returning an appropriate answer
     * It should answer to moveto and detectsat messages
     *
     * @param msg message sent by the Agent
     * @return a msg with the answer to return to the agent
     **/
    public AMessage acceptMessage(AMessage msg) {
        AMessage ans = new AMessage("voidmsg", "", "", "");
        msg.showMessage();
        if (msg.getComp(0).equals("moveto")) {
            int nx = Integer.parseInt(msg.getComp(1));
            int ny = Integer.parseInt(msg.getComp(2));
            if (positionsUtilities.withinLimits(nx, ny)) {
                ans = new AMessage("movedto", msg.getComp(1), msg.getComp(2), "");
            } else {
                ans = new AMessage("notmovedto", msg.getComp(1), msg.getComp(2), "");
            }
        } else if (msg.getComp(0).equals("detectsat")) {
            int nx = Integer.parseInt(msg.getComp(1));
            int ny = Integer.parseInt(msg.getComp(2));
            // Gets sensor readings
            String sensorsReadings = getSensorReadings(nx, ny);
            ans = new AMessage("detectsat", msg.getComp(1), msg.getComp(2), sensorsReadings);
        }
        return ans;

    }

    /**
     * Get the readings from the sensor at position (x, y).
     * For each position that contains an envelope, check if this
     * position is in the set of possible positions of each sensor
     * value in the position indicated for the parameters x, y.
     *
     * @param x position x
     * @param y position y
     * @return sensor values
     */
    public String getSensorReadings(int x, int y) {
        int linealPosition = Position.toLinealPosition(x, y, worldDims);
        String sensorsReadings = "";
        // For each treasure check if it is detected by the sensor in position x, y
        for (Position position : envelopePositions) {
            int envelopePosition = position.toLinealPosition(worldDims);
            // check if the sensor with value 1 detects any treasure
            if (positionsUtilities.positionsOfValue1(linealPosition).contains(envelopePosition))
                sensorsReadings += "1 ";
            // check if the sensor with value 2 detects any treasure
            if (positionsUtilities.positionsOfValue2(linealPosition).contains(envelopePosition))
                sensorsReadings += "2 ";
            // check if the sensor with value 3 detects any treasure
            if (positionsUtilities.positionsOfValue3(linealPosition).contains(envelopePosition))
                sensorsReadings += "3 ";
        }
        // strip end space
        return sensorsReadings;
    }


    /**
     * Check if position x,y is within the limits of the
     * WorldDim x WorldDim   world
     *
     * @param x x coordinate of agent position
     * @param y y coordinate of agent position
     * @return true if (x,y) is within the limits of the world
     **/
    public boolean withinLimits(int x, int y) {

        return (x >= 1 && x <= WorldDim && y >= 1 && y <= WorldDim);
    }

}
