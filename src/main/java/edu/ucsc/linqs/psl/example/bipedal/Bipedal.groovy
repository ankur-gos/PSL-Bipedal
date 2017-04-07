/*
 * Bipedal.groovy
 * Main file for inference
 * Ankur Goswami, agoswam3@ucsc.edu
 */

package edu.ucsc.linqs.psl.example.bipedal;

import edu.umd.cs.psl.application.inference.MPEInference;
import edu.umd.cs.psl.config.ConfigBundle;
import edu.umd.cs.psl.config.ConfigManager;
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DatabasePopulator;
import edu.umd.cs.psl.database.DataStore;
import edu.umd.cs.psl.database.loading.Inserter;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver;
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type;
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore;
import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.evaluation.result.FullInferenceResult;
import edu.umd.cs.psl.evaluation.resultui.printer.AtomPrintStream;
import edu.umd.cs.psl.evaluation.resultui.printer.DefaultAtomPrintStream;
import edu.umd.cs.psl.evaluation.statistics.ContinuousPredictionComparator;
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionComparator;
import edu.umd.cs.psl.evaluation.statistics.DiscretePredictionStatistics;
import edu.umd.cs.psl.groovy.PSLModel;
import edu.umd.cs.psl.model.argument.ArgumentType;
import edu.umd.cs.psl.model.atom.Atom;
import edu.umd.cs.psl.model.predicate.StandardPredicate;
import edu.umd.cs.psl.ui.loading.InserterUtils;
import edu.umd.cs.psl.util.database.Queries;
import edu.umd.cs.psl.model.function.ExternalFunction;
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.Term;
import java.util.HashSet;
import edu.umd.cs.psl.model.argument.Variable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.time.TimeCategory;
import java.nio.file.Paths

public class Bipedal{
    private static final PARTITION_OBSERVATIONS = 0;
    private static final PARTITION_TARGETS = 1;
    private static final PARTITION_TRUTH = 2;

    private Logger log;
    private DataStore ds;
    private PSLConfig config;
    private PSLModel model;

    // Config
    // Copy pasted from example
    private class PSLConfig {
        public ConfigBundle cb;
        public String experimentName;
        public String dbPath;
        public String dataPath;
        public String outputPath;

        public boolean sqPotentials = true;
        public Map weightMap = [
                "Knows":10,
                "Prior":2
        ];
        public String[] timesOfDay = [
            "Morning",
            "Afternoon",
            "Evening",
            "Night"
        ]
        public boolean useFunctionalConstraint = false;
        public boolean useFunctionalRule = false;

        public PSLConfig(ConfigBundle cb){
            this.cb = cb;

            this.experimentName = cb.getString('experiment.name', 'default');
            this.dbPath = cb.getString('experiment.dbpath', '/tmp');
            this.dataPath = cb.getString('experiment.data.path', 'data');
            this.outputPath = cb.getString('experiment.output.outputdir', Paths.get('output', this.experimentName).toString());
            this.weightMap["Knows"] = cb.getInteger('model.weights.knows', weightMap["Knows"]);
            this.weightMap["Prior"] = cb.getInteger('model.weights.prior', weightMap["Prior"]);
            this.useFunctionalConstraint = cb.getBoolean('model.constraints.functional', false);
            this.useFunctionalRule = cb.getBoolean('model.rules.functional', false);
        }
    }

    // Constructor
    public Bipedal(ConfigBundle cb) {
        log = LoggerFactory.getLogger(this.class);
        config = new PSLConfig(cb);
        ds = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, Paths.get(config.dbPath, 'easycc').toString(), true), cb);
        model = new PSLModel(this, ds);
    }

    // Predicates
    private void definePredicates(){
        model.add predicate: "Segment", types: [ArgumentType.UniqueID]
        model.add predicate: "StartLocation", types: [ArgumentType.UniqueID, ArgumentType.Double, ArgumentType.Double];
        model.add predicate: "EndLocation", types: [ArgumentType.UniqueID, ArgumentType.Double, ArgumentType.Double];
        model.add predicate: "StartTime", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "EndTime", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "Mode", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "AnchorTime", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.String];
        model.add predicate: "AnchorMode", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.String];
        model.add predicate: "Anchor", types: [ArgumentType.Double, ArgumentType.Double];

        // Frequent trips
        model.add predicate: "FrequentTrip", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double];
        model.add predicate: "FrequentTripTime", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.String, ArgumentType.String]
        model.add predicate: "FrequentTripMode", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.String]
        model.add predicate: "SegmentDay", types: [ArgumentType.UniqueID, ArgumentType.String]
    }

    // Functions
    private void defineFunctions(){
        model.add function: "EqualLocations", implementation: new LocationComparison();
        model.add function: "Near", implementation: new ManhattanNear();
    }

    private void defineRules(){
        log.info("Defining model rules");
        // Anchor locations
        model.add rule: (Segment(S) & StartLocation(S, X, Y) & StartTime(S, T)) >> AnchorTime(X, Y, T),
                  weight: 1;
        model.add rule: (Segment(S) & EndLocation(S, X, Y) & EndTime(S, T)) >> AnchorTime(X, Y, T), weight: 1;
        model.add rule: (Segment(S) & StartLocation(S, X, Y) & Mode(S, M)) >> AnchorMode(X, Y, M), weight: 1;
        model.add rule: (Segment(S) & EndLocation(S, X, Y) & Mode(S, M)) >> AnchorMode(X, Y, M), weight: 1;
        model.add rule: (AnchorMode(X, Y, M)) >> Anchor(X, Y), weight: 3;
        model.add rule: (AnchorTime(X, Y, T)) >> Anchor(X, Y), weight: 3;
        model.add rule: (AnchorTime(X1, Y1, T) & AnchorTime(X2, Y2, T) & ~EqualLocations(X1, Y1, X2, Y2)) >> ~Anchor(X2, Y2), weight: 1;
        model.add rule: (Anchor(X1, Y1) & Near(X1, Y1, X2, Y2) & ~EqualLocations(X1, Y1, X2, Y2)) >> ~Anchor(X2, Y2), weight: 1;
        model.add rule: ~Anchor(X, Y), weight: 2;

        // Frequent Trips
        model.add rule: (Segment(S) & Anchor(X1, Y1) & Anchor(X2, Y2)
                                    & StartLocation(S, X1, Y1) & EndLocation(S, X2, Y2))
                                    >> FrequentTrip(X1, Y1, X2, Y2), weight: 1;

        // TODO: Add time requirements
        model.add rule: (Segment(S1) & Segment(S2) & Anchor(X1, Y1) & Anchor(X2, Y2)
                                     & StartLocation(S1, X1, Y1) & EndLocation(S2, X2, Y2)
                                     & SegmentDay(S1, D) & SegmentDay(S2, D))
                                     >> FrequentTrip(X1, Y1, X2, Y2)
        model.add rule:
    }

    class ManhattanNear implements ExternalFunction {

        @Override
        public int getArity(){
            return 4;
        }

        @Override
        public ArgumentType[] getArgumentTypes(){
            return [ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double];
        }

        @Override
        public double getValue(ReadOnlyDatabase db, GroundTerm... args){
            double mdist = (args[0].getValue() - args[2].getValue()).abs() + (args[1].getValue() - args[3].getValue()).abs();
            return mdist < 5.0 ? 1.0 : 0.0;
        }
    }

    class LocationComparison implements ExternalFunction {

        @Override
        public int getArity(){
            return 4;
        }

        @Override
        public ArgumentType[] getArgumentTypes(){
            return [ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double];
        }

        @Override
        public double getValue(ReadOnlyDatabase db, GroundTerm... args){
            return args[0].getValue() == args[2].getValue() && args[1].getValue() == args[3].getValue() ? 1.0 : 0.0;
        }
    }

    /*
     * getLocations
     * Access the observation partition, get the start and end locations and extract them into two
     * hashsets
     */
    private Tuple getLocations(Partition obsPartition){
        def obsDb = ds.getDatabase(obsPartition);
        Set startLocationSet = Queries.getAllAtoms(obsDb, StartLocation);
        Set endLocationSet = Queries.getAllAtoms(obsDb, EndLocation);
        Set<Term> locationX = new HashSet<Term>();
        Set<Term> locationY = new HashSet<Term>();
        for (Atom a: startLocationSet){
            Term[] arguments = a.getArguments();
            locationX.add(arguments[1]);
            locationY.add(arguments[2]);
        }
        for (Atom a: endLocationSet){
            Term[] arguments = a.getArguments();
            locationX.add(arguments[1]);
            locationY.add(arguments[2]);
        }
        obsDb.close();
        return new Tuple(locationX, locationY);
    }

    /*
     * crossAnchor
     * Fill the target partition with the cross product of locations
     */
    private void crossAnchor(Partition obsPartition, Partition targetPartition){
        def locations = getLocations(obsPartition);
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("LocationX"), locations[0]);
        popMap.put(new Variable("LocationY"), locations[1]);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((Anchor(LocationX, LocationY)).getFormula(), popMap);
        AtomPrintStream aps = new DefaultAtomPrintStream();
        targetDb.close();
    }

    /*
     * crossLocationMode
     * Access the observation partition, and cross the locations with each possible
     * mode
     */
    private void crossLocationMode(Partition obsPartition, Partition targetPartition){
        def locations = getLocations(obsPartition);
        def obsDb = ds.getDatabase(obsPartition);
        Set modeSet = Queries.getAllAtoms(obsDb, Mode);
        Set<Term> modes = new HashSet<Term>();
        for (Atom a: modeSet){
            Term[] arguments = a.getArguments();
            modes.add(arguments[1]);
        }
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("LocationX"), locations[0]);
        popMap.put(new Variable("LocationY"), locations[1]);
        popMap.put(new Variable("ModeTerm"), modes);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((AnchorMode(LocationX, LocationY, ModeTerm)).getFormula(), popMap);
        AtomPrintStream aps = new DefaultAtomPrintStream();
        obsDb.close();
        targetDb.close()
    }

    /*
     * crossLocationTime
     * Access the observation partition, and cross the locations with each possible
     * time of day
     */
    private void crossLocationTime(Partition obsPartition, Partition targetPartition){
        def locations = getLocations(obsPartition);
        def obsDb = ds.getDatabase(obsPartition);
        Set startTimeSet = Queries.getAllAtoms(obsDb, StartTime);
        Set endTimeSet = Queries.getAllAtoms(obsDb, EndTime);
        Set<Term> times = new HashSet<Term>();
        for (Atom a: startTimeSet){
            Term[] arguments = a.getArguments();
            times.add(arguments[1]);
        }
        for (Atom a: endTimeSet){
            Term[] arguments = a.getArguments();
            times.add(arguments[1])
        }
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("LocationX"), locations[0]);
        popMap.put(new Variable("LocationY"), locations[1]);
        popMap.put(new Variable("Time"), times);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((AnchorTime(LocationX, LocationY, Time)).getFormula(), popMap);
        obsDb.close();
        targetDb.close();
    }

    private void loadData(Partition obsPartition, Partition targetsPartition, Partition truthPartition) {
        log.info("Loading data into database");

        // Fill all of the obs partition from files
        Inserter inserter = ds.getInserter(Segment, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "segment_obs.txt").toString());

        inserter = ds.getInserter(StartLocation, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "start_location_obs.txt").toString());

        inserter = ds.getInserter(EndLocation, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "end_location_obs.txt").toString());

        inserter = ds.getInserter(StartTime, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "start_time_obs.txt").toString());

        inserter = ds.getInserter(EndTime, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "end_time_obs.txt").toString());

        inserter = ds.getInserter(Mode, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "mode_obs.txt").toString())

        // Run the cross functions to fill the targets partition
        crossLocationTime(obsPartition, targetsPartition);
        crossLocationMode(obsPartition, targetsPartition);
        crossAnchor(obsPartition, targetsPartition);

        inserter = ds.getInserter(Anchor, truthPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, 'anchor_truth.txt').toString());
    }

    // Run inference
    private FullInferenceResult runInference(Partition obsPartition, Partition targetsPartition) {
        log.info("Starting inference");

        Date infStart = new Date();
        HashSet closed = new HashSet<StandardPredicate>([StartLocation,EndLocation,StartTime,EndTime,Segment,Mode]);
        Database inferDB = ds.getDatabase(targetsPartition, closed, obsPartition);
        MPEInference mpe = new MPEInference(model, inferDB, config.cb);
        FullInferenceResult result = mpe.mpeInference();

        inferDB.close();
        mpe.close();

        log.info("Finished inference in {}", TimeCategory.minus(new Date(), infStart));
        return result;
    }

    /**
     * Writes the inference outputs to a file
     */
    private void writeOutput(Partition targetsPartition) {
        Database resultsDB = ds.getDatabase(targetsPartition);

        // Temporarily redirect stdout.
        PrintStream stdout = System.out;
        PrintStream ps = new PrintStream(new File(Paths.get(config.outputPath, "anchor_infer.txt").toString()));
        System.setOut(ps);

        AtomPrintStream aps = new DefaultAtomPrintStream();
        Set anchorSet = Queries.getAllAtoms(resultsDB, Anchor);
        for (Atom a : anchorSet) {
            aps.printAtom(a);
        }

        aps.close();
        ps.close();

        System.setOut(stdout);
        resultsDB.close();
    }

    /**
     * Evaluates the results of inference versus expected truth values
     */
    private void evalResults(Partition targetsPartition, Partition truthPartition) {
        Database resultsDB = ds.getDatabase(targetsPartition, [Anchor] as Set);
        Database truthDB = ds.getDatabase(truthPartition, [Anchor] as Set);
        DiscretePredictionComparator dpc = new DiscretePredictionComparator(resultsDB);
        dpc.setBaseline(truthDB);
        DiscretePredictionStatistics stats = dpc.compare(Anchor);
        log.info(
                "Stats: precision {}, recall {}",
                stats.getPrecision(DiscretePredictionStatistics.BinaryClass.POSITIVE),
                stats.getRecall(DiscretePredictionStatistics.BinaryClass.POSITIVE));

        resultsDB.close();
        truthDB.close();
    }

    /**
     * Runs the PSL program using configure options - defines a model, loads data,
     * performs inferences, writes output to files, evaluates results
     */
    public void run() {
        log.info("Running experiment {}", config.experimentName);

        Partition obsPartition = new Partition(PARTITION_OBSERVATIONS);
        Partition targetsPartition = new Partition(PARTITION_TARGETS);
        Partition truthPartition = new Partition(PARTITION_TRUTH);

        definePredicates();
        defineFunctions();
        defineRules();
        loadData(obsPartition, targetsPartition, truthPartition);
        runInference(obsPartition, targetsPartition);
        writeOutput(targetsPartition);
        evalResults(targetsPartition, truthPartition);

        ds.close();
    }

    /**
     * Populates the ConfigBundle for this PSL program using arguments specified on
     * the command line
     * @param args - Command line arguments supplied during program invocation
     * @return ConfigBundle with the appropriate properties set
     */
    public static ConfigBundle populateConfigBundle(String[] args) {
        ConfigBundle cb = ConfigManager.getManager().getBundle("bipedal");
        if (args.length > 0) {
            cb.setProperty('experiment.data.path', args[0]);
        }
        return cb;
    }

    /**
     * Runs the PSL program from the command line with specified arguments
     * @param args - Arguments for program options
     */
    public static void main(String[] args){
        ConfigBundle cb = populateConfigBundle(args);
        Bipedal bp = new Bipedal(cb);
        bp.run();
    }
}
