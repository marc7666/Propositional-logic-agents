package apryraz.eworld;


import java.io.IOException;

import org.sat4j.specs.*;
import org.sat4j.minisat.*;
import org.sat4j.reader.*;

/**
 * @author Dand Marbà Sera
 * @author Marc Cervera Rosell
 **/

/**
 * The class for the main program of the Barcenas World
 **/
public class EnvelopeWorld {


    /**
     * This function should execute the sequence of steps stored in the file fileSteps,
     * but only up to numSteps steps. Each step must be executed with function
     * runNextStep() of the BarcenasFinder agent.
     *
     * @param wDim          the dimension of world
     * @param numSteps      num of steps to perform
     * @param fileSteps     file name with sequence of steps to perform
     * @param fileEnvelopes file name with sequence of steps to perform
     **/
    public static void runStepsSequence(int wDim,
                                        int numSteps, String fileSteps, String fileEnvelopes) throws
            IOException, ContradictionException, TimeoutException {
        // Make instances of TreasureFinder agent and environment object classes
        EnvelopeFinder EAgent = new EnvelopeFinder(wDim);
        EnvelopeWorldEnv EnvAgent = new EnvelopeWorldEnv(wDim, fileEnvelopes);
        // save environment object into EAgent
        EAgent.setEnvironment(EnvAgent);
        // load list of steps into the Finder Agent
        EAgent.loadListOfSteps(numSteps, fileSteps);
        // Execute sequence of steps with the Agent
        while (EAgent.hasMovements()) {
            EAgent.runNextStep();
        }

    }

    /**
     * This function should load five arguments from the command line:
     * arg[0] = dimension of the word
     * arg[3] = num of steps to perform
     * arg[4] = file name with sequence of steps to perform
     * arg[5] = file name with list of envelopes positions
     **/
    public static void main(String[] args) throws ParseFormatException,
            IOException, ContradictionException, TimeoutException {
        int wDim = Integer.parseInt(args[0]);
        int numSteps = Integer.parseInt(args[1]);
        String fileSteps = args[2];
        String envelopesFile = args[3];
        runStepsSequence(wDim, numSteps, fileSteps, envelopesFile);
    }

}
