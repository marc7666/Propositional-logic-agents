package apryraz.eworld;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.System.exit;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;




/**
 *  This agent performs a sequence of movements, and after each
 *  movement it "senses" from the evironment the resulting position
 *  and then the outcome from the smell sensor, to try to locate
 *  the position of Envelope
 *
 * @author Alejandro Clavera Poza
 * @author Moises Bernaus Lechosa
 */
public class EnvelopeFinder  {
    /**
     * The list of steps to perform
     */
    ArrayList<Position> listOfSteps;

    /**
     * index to the next movement to perform, and total number of movements
     */
    int idNextStep, numMovements;

    /**
     *  Array of clauses that represent conclusiones obtained in the last
     * call to the inference function, but rewritten using the "past" variables
     */
    ArrayList<VecInt> futureToPast = null;

    /**
     *  List of clauses that represent Evidences obtained of sensor readings
     */
    List<IConstr> evidencesClauses = null;

    /**
     *  List of discard positions
     */
    List<Integer> discardPositions = null;

    /**
     * the current state of knowledge of the agent (what he knows about
     * every position of the world)
     */
    EFState efstate;

    /**
     *   The object that represents the interface to the Envelope World
     */
    EnvelopeWorldEnv EnvAgent;

    /**
     *   SAT solver object that stores the logical boolean formula with the rules
     *   and current knowledge about not possible locations for Envelope
     */
    ISolver solver;

    /**
     *   Agent position in the world
     */
    int agentX, agentY;

    /**
     *  Dimension of the world and total size of the world (Dim^2)
     */
    int WorldDim, WorldLinealDim;

    /**
     * This set of variables CAN be use to mark the beginning of different sets
     * of variables in your propositional formula (but you may have more sets of
     * variables in your solution).
     **/
    int EnvelopePastOffset;
    int EnvelopeFutureOffset;
    int DetectorOffset;

    /**
     * Utilities of positions
     */
    PositionsUtilities positionsUtilities;

    /**
     * The class constructor must create the initial Boolean formula with the
     * rules of the Envelope World, initialize the variables for indicating
     * that we do not have yet any movements to perform, make the initial state.
     *
     * @param WDim the dimension of the Envelope World
     */
    public EnvelopeFinder(int WDim) {

        WorldDim = WDim;
        WorldLinealDim = WorldDim * WorldDim;

        positionsUtilities = new PositionsUtilities(WDim);

        // Create solver and formula
        try {
            solver = buildGamma();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException | ContradictionException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
        }
        numMovements = 0;
        idNextStep = 0;
        discardPositions = new ArrayList<>();
        System.out.println("STARTING Envelope FINDER AGENT...");

        efstate = new EFState(WorldDim);  // Initialize state (matrix) of knowledge with '?'
        efstate.printState();
    }

    /**
     * Store a reference to the Environment Object that will be used by the
     * agent to interact with the Envelope World, by sending messages and getting
     * answers to them. This function must be called before trying to perform any
     * steps with the agent.

     * @param environment the Environment object
     */
    public void setEnvironment( EnvelopeWorldEnv environment ) {
        EnvAgent =  environment;
    }

    /**
     * Load a sequence of steps to be performed by the agent. This sequence will
     * be stored in the listOfSteps ArrayList of the agent.  Steps are represented
     * as objects of the class Position.

     * @param numSteps number of steps to read from the file
     * @param stepsFile the name of the text file with the line that contains
    the sequence of steps: x1,y1 x2,y2 ...  xn,yn

     */
    public void loadListOfSteps( int numSteps, String stepsFile ) {
        String[] stepsList;
        String steps = ""; // Prepare a list of movements to try with the FINDER Agent
        try {
            BufferedReader br = new BufferedReader(new FileReader(stepsFile));
            System.out.println("STEPS FILE OPENED ...");
            steps = br.readLine();
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => Steps file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeFinder.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        stepsList = steps.split(" ");
        listOfSteps = new ArrayList<Position>(numSteps);
        for (int i = 0 ; i < numSteps ; i++ ) {
            String[] coords = stepsList[i].split(",");
            listOfSteps.add(new Position(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
        }
        numMovements = listOfSteps.size(); // Initialization of numMovements
        idNextStep = 0;
    }

    /**
     *    Returns the current state of the agent.
     *
     *    @return the current state of the agent, as an object of class EFState
     */
    public EFState getState()
    {
        return efstate;
    }

    /**
     *    Execute the next step in the sequence of steps of the agent, and then
     *    use the agent sensor to get information from the environment. In the
     *    original Envelope World, this would be to use the Sensor to get
     *    a binary answer, and then to update the current state according to the
     *    result of the logical inferences performed by the agent with its formula.
     *
     **/
    public void runNextStep() throws
            IOException,  ContradictionException, TimeoutException
    {

        // Add the conclusions obtained in the previous step
        // but as clauses that use the "past" variables
        addLastFutureClausesToPastClauses();

        // Ask to move, and check whether it was successful
        processMoveAnswer(moveToNext());

        // Next, use Detector sensor to discover new information
        processDetectorSensorAnswer(DetectsAt());


        // Perform logical consequence questions for all the positions
        // of the Envelope World
        performInferenceQuestions();
        efstate.printState();      // Print the resulting knowledge matrix
    }

    /**
     *   Ask the agent to move to the next position, by sending an appropriate
     *   message to the environment object. The answer returned by the environment
     *   will be returned to the caller of the function.
     *
     *   @return the answer message from the environment, that will tell whether the
     *           movement was successful or not.
     */
    public AMessage moveToNext() {
        Position nextPosition;

        if (idNextStep < numMovements) {
            nextPosition = listOfSteps.get(idNextStep);
            idNextStep = idNextStep + 1;
            return moveTo(nextPosition.x, nextPosition.y);
        } else {
            System.out.println("NO MORE steps to perform at agent!");
            return (new AMessage("NOMESSAGE","","", ""));
        }
    }

    /**
     *
     * Use agent "actuators" to move to (x,y)
     * We simulate this by telling to the World Agent (environment)
     * that we want to move, but we need the answer from it
     * to be sure that the movement was made with success
     *
     *  @param x  horizontal coordinate (row) of the movement to perform
     *  @param y  vertical coordinate (column) of the movement to perform
     *
     *  @return returns the answer obtained from the environment object to the
     *           moveto message sent
     */
    public AMessage moveTo( int x, int y ) {
        // Tell the EnvironmentAgentID that we want  to move
        AMessage msg, ans;

        msg = new AMessage("moveto", (new Integer(x)).toString(), (new Integer(y)).toString(), "" );
        ans = EnvAgent.acceptMessage( msg );
        System.out.println("FINDER => moving to : (" + x + "," + y + ")");

        return ans;
    }

    /**
     * Process the answer obtained from the environment when we asked
     * to perform a movement
     *
     * @param moveans the answer given by the environment to the last move message
     */
    public void processMoveAnswer (AMessage moveans) {
        if ( moveans.getComp(0).equals("movedto") ) {
            agentX = Integer.parseInt( moveans.getComp(1) );
            agentY = Integer.parseInt( moveans.getComp(2) );

            System.out.println("FINDER => moved to : (" + agentX + "," + agentY + ")");
        }
    }

    /**
     *   Send to the environment object the question:
     *   "Does the detector sense something around(agentX,agentY) ?"
     *
     *   @return return the answer given by the environment
     */
    public AMessage DetectsAt( ) {
        AMessage msg, ans;

        msg = new AMessage( "detectsat", (new Integer(agentX)).toString(),
                (new Integer(agentY)).toString(), "" );
        ans = EnvAgent.acceptMessage( msg );
        System.out.println("FINDER => detecting at : (" + agentX + "," + agentY + ")");
        return ans;
    }

    /**
     * Process the answer obtained for the query "Detects at (x,y)?"
     * by adding the correspond evidence clause to the formula with addEvidencesOfPosition
     * function.
     *
     *   @param ans message obtained to the query "Detects at (x,y)?".
     *          It will a message with three fields: DetectorValue
     */
    public void processDetectorSensorAnswer(AMessage ans) throws
            IOException, ContradictionException,  TimeoutException
    {
        // Process the answer obtained for the query "Detects at (x,y)?"
        int x = Integer.parseInt(ans.getComp(1));
        int y = Integer.parseInt(ans.getComp(2));
        String[] detects = ans.getComp(3).split(" ");
        List<Integer> readingsList = new ArrayList<>(detects.length);
        for (int i = 0; i < detects.length; i++) {
            if(detects[i].length() >= 1)
                readingsList.add(Integer.parseInt(detects[i]));
        }
        addEvidencesOfPosition(Position.toLinealPosition(x, y, WorldDim), readingsList);
    }

    /**
     * This function should check, using the future variables related
     * to possible positions of Envelope, whether it is a logical consequence
     * that an envelope is NOT at certain positions. This should be checked for all the
     * positions of the Envelope World.
     * The logical consequences obtained is stored in the futureToPast list
     * but using the variables corresponding to the "past" variables of the same positions
     *
     * Finally removes the current evidences from the formula with removeEvidences function,
     * because it is not necessary, since the agent has consistency clauses for all the inferred information.
     */
    public void performInferenceQuestions() throws  IOException,
            ContradictionException, TimeoutException {
        // For each position check if not contains envelope
        for(int indexPast = 1; indexPast <= WorldLinealDim; indexPast++) {
            int indexFuture = EnvelopeFutureOffset + indexPast - 1;
            Position position = Position.linealToCoord(indexPast - 1, WorldDim);

            // if the position was discarded in the past this position is not evaluated
            if (discardPositions.contains(position)) {
                continue;
            }

            VecInt variablePositive = new VecInt();
            variablePositive.insertFirst(indexFuture);
            // Check logical consequence formula + not indexFutureX,Y
            if (!(solver.isSatisfiable(variablePositive))) {     // Add conclusion to list, but rewritten with respect to "past" variables
                VecInt concPast = new VecInt();
                concPast.insertFirst(-(indexPast));
                futureToPast.add(concPast);
                discardPositions.add(position.toLinealPosition(WorldDim));
                efstate.set(position.x, position.y, "X" );
            }
        }
        // remove the current evidences
        removeEvidences();
    }

    /**
     *  This function should add all the clauses stored in the list
     *  futureToPast to the formula stored in solver. It is not necessary to check if it causes
     *  contradictions since the agent works in a monotonous system
     */
    public void addLastFutureClausesToPastClauses() throws  IOException,
            ContradictionException, TimeoutException {
        if (futureToPast != null) {
            for(VecInt clause : futureToPast) {
                solver.addClause(clause);
            }
        }
        futureToPast = new ArrayList<>();
    }

    /**
     * Add the evidences to the formula, for each possible sensor reading in the current position,
     * it is included as a clause with the equivalent variable as a positive literal if it appears
     * in the reading and negative if it does not appear, except for readings that are outside
     * the limits of the world.
     *
     * @param evidences list of evidences (sould be sensors values)
     */
    public void addEvidencesOfPosition(int linealPosition, List<Integer> evidences) throws ContradictionException {
        Position position = Position.linealToCoord(linealPosition, WorldDim);
        evidencesClauses = new ArrayList<>();
        for (int sensorValue = 1; sensorValue <= 5; sensorValue++) {
            int literal;
            //check if the sensor value is in the reading and in other case check if the
            // neighboring positions for that value are within the limits of the world
            if (evidences.contains(sensorValue)) {
                literal = DetectorOffset + 5 * (linealPosition) + sensorValue - 1;
            } else if (sensorValue == 1 && position.x > WorldDim) {
                literal = DetectorOffset + 5 * (linealPosition) + sensorValue - 1;
            } else if (sensorValue == 2 && position.y > WorldDim) {
                literal = DetectorOffset + 5 * (linealPosition) + sensorValue - 1;
            } else  {
                literal = -1 * (DetectorOffset + 5 * (linealPosition) + sensorValue - 1);
            }
            // Create evidence clause
            VecInt evidenceClause = new VecInt(new int[]{literal});
            IConstr evidence = solver.addClause(evidenceClause);
            evidencesClauses.add(evidence);
        }
    }

    /**
     * This function remove the current evidences from the formula
     */
    public void removeEvidences() {
        for (IConstr evidence : evidencesClauses) {
            solver.removeConstr(evidence);
        }
    }
    /**
     * This function check if the agent has movements.
     *
     *  @return returns true if has moviments, false in other case
     */
    public boolean hasMovements() {
        return idNextStep < numMovements;
    }

    /**
     * This function builds the initial logical formula of the agent and stores it
     * into the solver object.
     *
     * For this, the function generates the consistency clauses, the sensor discard
     * clauses and the clauses that indicate that there may be envelopes in any of
     * the positions, making use of the functions addconsistencyClauses (),
     * addSensorsClauses(), addLocatedClauses() respectively.
     *
     *  @return returns the solver object where the formula has been stored
     */
    public ISolver buildGamma() throws UnsupportedEncodingException,
            FileNotFoundException, IOException, ContradictionException {
        // Set this variable to the total number of boolean variables
        int totalNumVariables = 7 * WorldLinealDim;

        // Set Envelope offsets
        EnvelopePastOffset = 1;
        EnvelopeFutureOffset = WorldLinealDim + 1;
        DetectorOffset = 2 * WorldLinealDim + 1;

        // create solver
        solver = SolverFactory.newDefault();
        solver.setTimeout(3600);
        solver.newVar(totalNumVariables);
        // add clauses to solver
        addLocatedClauses();
        addSensorsClauses();
        addconsistencyClauses();
        return solver;
    }

    /**
     * This function add location clauses of future and past into the solver object.
     * For this, the function generates two clauses, the first contains all variables that
     * represents the location of envelopes in the past and the other contains all variables of
     * location of envelopes in the future.
     */
    private void addLocatedClauses() throws ContradictionException {
        VecInt pastClause = new VecInt();
        VecInt futureClause = new VecInt();
        // Add located clause of past and future
        for(int currentLit = 0; currentLit < WorldLinealDim; currentLit++) {
            pastClause.push(currentLit + EnvelopePastOffset);
            futureClause.push(currentLit + EnvelopeFutureOffset);
        }
        // Add location clause of past
        solver.addClause(pastClause);
        // Add location clause of future
        solver.addClause(futureClause);
    }

    /**
     * This function adds consistency clauses in the resolution object. For each position
     * a clause is generated that contains the negation of the location of the variable envelope of that position
     * in the past and its equivalent in the future also denied.
     */
    private void addconsistencyClauses() throws ContradictionException {
        VecInt consistencyClause;
        // Add located clause of past and future
        for(int currentLit = 0; currentLit < WorldLinealDim; currentLit++) {
            // generate the clause not pastPositionxy -> not futurePositionxy
            consistencyClause = new VecInt();
            consistencyClause.push(-1 * (currentLit + EnvelopePastOffset));
            consistencyClause.push(-1 * (currentLit + EnvelopeFutureOffset));
            solver.addClause(consistencyClause);
        }
    }

    /**
     * This function add sensors clauses into the solver object. For each
     * possible reading of position gets all position that correspond to this
     * reading(with getPosiblePositions function) and generates the clause for each this positions.
     *
     * This clauses allow the agent to discard positions in case
     * of not receiving any or all of the sensor responses.
     */
    private void addSensorsClauses() throws ContradictionException {
        int fistLiteralSensor = 2 * WorldLinealDim + 1;
        // for each position of word add all sensor clauses
        for(int position = 0; position < WorldLinealDim; position++) {
            // generate all sensor variable of the position
            for(int sensorvalue = 1; sensorvalue <= 5; sensorvalue++) {
                // Codification of negation variable sensors
                int sensorVariable = fistLiteralSensor + 5 * (position) + sensorvalue - 1  ;
                // get the possible positions of a sensor lecture in a position
                List<Integer> possiblePositions = getPosiblePositions(position, sensorvalue);
                // for each possible position create a discard clause
                for(int possiblePosition : possiblePositions) {
                    // generate the clause
                    VecInt clause = new VecInt(new int[]{sensorVariable, -1*(EnvelopeFutureOffset + possiblePosition)});
                    solver.addClause(clause);
                }
            }
        }
    }

    /**
     * This function obtains the list of possible positions for a sensor reading in
     * the position indicated by the linearPosition variable.
     *
     * For obtain the list of possible positions, use different functions of the class
     * positionUtilities, like positionsOfValue1, positionsOfValue2, positionsOfValue3
     * positionsOfValue4, positionsOfValue5.
     *
     * @param lineaPosition lineal position
     * @param sensorValue sensor value
     *
     * @return returns a list of integers containing the valid positions for the sensor
     * value from the position that represent linearPosition variable.
     */
    private List<Integer> getPosiblePositions(int lineaPosition, int sensorValue) {
        if(sensorValue == 1) {
            return positionsUtilities.positionsOfValue1(lineaPosition);
        }else if(sensorValue == 2) {
            return positionsUtilities.positionsOfValue2(lineaPosition);
        }else if(sensorValue == 3) {
            return positionsUtilities.positionsOfValue3(lineaPosition);
        }
        return new ArrayList<>();
    }
}

