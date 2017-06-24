/*
 * InferFrequentTrips.groovy
 * Frequent Trip inference
 * Ankur Goswami, agoswam3@ucsc.edu
 */

package edu.ucsc.linqs.psl.example.infer_frequents;

import edu.umd.cs.psl.application.inference.MPEInference;
import edu.umd.cs.psl.application.learning.weight.em.HardEM;
import edu.umd.cs.psl.config.ConfigBundle;
import edu.umd.cs.psl.config.ConfigManager;
import edu.umd.cs.psl.database.Database;
import edu.umd.cs.psl.database.DatabasePopulator;
import edu.umd.cs.psl.database.DataStore;
import edu.umd.cs.psl.database.loading.Inserter;
import edu.umd.cs.psl.database.Partition;
import edu.umd.cs.psl.application.learning.weight.em.ExpectationMaximization;
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
import java.lang.Double;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.time.TimeCategory;
import java.nio.file.Paths

public class InferFrequentTrips{
    private static final PARTITION_OBSERVATIONS = 0;
    private static final PARTITION_TARGETS = 1;
    private static final PARTITION_TRUTH = 2;

    private Logger log;
    private DataStore ds;
    private PSLConfig config;
    private PSLModel model;
    public int cluster_count = 10;

    // Config
    private class PSLConfig {
        public ConfigBundle cb;
        public String experimentName;
        public String dbPath;
        public String dataPath;
        public String outputPath;
        public boolean useFunctionalConstraint = false;
        public boolean useFunctionalRule = false;

        public PSLConfig(ConfigBundle cb){
            this.cb = cb;

            this.experimentName = cb.getString('experiment.name', 'default');
            this.dbPath = cb.getString('experiment.dbpath', '/tmp');
            this.dataPath = cb.getString('experiment.data.path', 'data');
            this.outputPath = cb.getString('experiment.output.outputdir', Paths.get('output', this.experimentName).toString());
            this.useFunctionalConstraint = cb.getBoolean('model.constraints.functional', false);
            this.useFunctionalRule = cb.getBoolean('model.rules.functional', false);
        }
    }

    // Constructor
    public InferFrequentTrips(ConfigBundle cb) {
        log = LoggerFactory.getLogger(this.class);
        config = new PSLConfig(cb);
        ds = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, Paths.get(config.dbPath, 'finfer').toString(), true), cb);
        model = new PSLModel(this, ds);
    }

    // Predicates
    private void definePredicates(){
        model.add predicate: "Cluster", types: [ArgumentType.Integer, ArgumentType.String, ArgumentType.String]

        model.add predicate: "Segment", types: [ArgumentType.UniqueID];
        model.add predicate: "StartLocation", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "EndLocation", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "StartTime", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "EndTime", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "Mode", types: [ArgumentType.UniqueID, ArgumentType.String];
        model.add predicate: "Anchor", types: [ArgumentType.String];

        // Frequent trips
        model.add predicate: "FrequentTrip", types: [ArgumentType.String, ArgumentType.String];
        model.add predicate: "FrequentTripTime", types: [ArgumentType.String, ArgumentType.String, ArgumentType.String, ArgumentType.String]
        model.add predicate: "FrequentTripMode", types: [ArgumentType.String, ArgumentType.String, ArgumentType.String]
        model.add predicate: "SegmentDay", types: [ArgumentType.UniqueID, ArgumentType.String]
    }

    // Functions
    private void defineFunctions(){
        model.add function: "EqualLocations", implementation: new LocationComparison();
        model.add function: "Near", implementation: new ManhattanNear();
        model.add function: "LeftOf", implementation: new LeftSort();
    }

    private void defineRules(){
        log.info("Defining model rules");
        // Frequent Trips
        // for(int i = 0; i < this.cluster_count; i++){
        model.add rule: ~FrequentTrip(L1, L2), weight: 20;
        model.add rule: (Segment(S) & Anchor(L1) & Anchor(L2)
                                & StartLocation(S, L1) & EndLocation(S, L2) & ~EqualLocations(L1, L2) ) >> FrequentTrip(L1, L2), weight: 5;

        model.add rule: (Segment(S1) & Segment(S2) & Anchor(L1) & Anchor(L2)
                                    & StartLocation(S1, L1) & EndLocation(S2, L2)
                                    & SegmentDay(S1, D) & SegmentDay(S2, D) 
                                    & StartTime(S1, T) & EndTime(S2, T) & ~EqualLocations(L1, L2)) >> FrequentTrip(L1, L2), weight: 0.1;
        
        // model.add rule: (FrequentTrip(L1, L2) & FrequentTrip(L2, L3) & FrequentTrip(L3, L1) & Near(L1, L2) & Near(L2, L3) & ~Near(L1, L3) & LeftOf(L1, L3)) >> FrequentTrip(L1, L3), weight: 1.0
        // model.add rule: (FrequentTrip(L1, L2) & FrequentTrip(L2, L3) & FrequentTrip(L3, L1) & Near(L1, L2) & Near(L2, L3) & ~Near(L1, L3)) >> ~FrequentTrip(L2, L3), weight: 20.0
        // model.add rule: (FrequentTrip(L1, L2) & FrequentTrip(L2, L3) & FrequentTrip(L3, L1)) >> ~FrequentTrip(L2, L3), weight: 2.0
        // model.add rule: (FrequentTrip(L1, L2) & FrequentTrip(L3, L1)) >> FrequentTrip(L2, L3), weight: 0.1;
        // model.add rule: (FrequentTrip(L1, L2) & FrequentTrip(L3, L4) & FrequentTrip(L4, L1)) >> FrequentTrip(L2, L3), weight: 0.05;
        // model.add rule: (FrequentTrip(L1, L2) & StartLocation(S1, L1) & EndLocation(S2, L2)
                                                    //  & StartTime(S1, T1) & EndTime(S2, T2)) >> FrequentTripTime(L1, L2, T1, T2), weight: 1;
        // model.add rule: (FrequentTrip(L1, L2) & StartLocation(S1, L1) & EndLocation(S2, L2) & Mode(S1, M) & Mode(S2, M)) >> FrequentTripMode(L1, L2, M), weight: 1;
        // }
    }

    public double[] deserializeLocations(String s1, String s2){
        String[] split1 = s1.split(" ");
        String[] split2 = s2.split(" ");
        double x1 = Double.parseDouble(split1[0]);
        double y1 = Double.parseDouble(split1[1]);
        double x2 = Double.parseDouble(split2[0]);
        double y2 = Double.parseDouble(split2[1]);
        return [x1, y1, x2, y2];
    }

    class ManhattanNear implements ExternalFunction {

        @Override
        public int getArity(){
            return 2;
        }

        @Override
        public ArgumentType[] getArgumentTypes(){
            return [ArgumentType.String, ArgumentType.String];
        }

        @Override
        public double getValue(ReadOnlyDatabase db, GroundTerm... args){
            String s1 = args[0].getValue();
            String s2 = args[1].getValue();
            double[] values = deserializeLocations(s1, s2);
            double mdist = (values[0] - values[2]).abs() + (values[1] - values[3]).abs();
            // Take the inverse of the mdist to get a value between 0 and 1 and return it.
            double inv_mdist = 1 / mdist;
            if(inv_mdist > 1)
                inv_mdist = 1
            return inv_mdist;
        }
    }

    class LocationComparison implements ExternalFunction {

        @Override
        public int getArity(){
            return 2;
        }

        @Override
        public ArgumentType[] getArgumentTypes(){
            return [ArgumentType.String, ArgumentType.String];
        }

        @Override
        public double getValue(ReadOnlyDatabase db, GroundTerm... args){
            String s1 = args[0].getValue();
            String s2 = args[1].getValue();
            double[] values = deserializeLocations(s1, s2);
            return values[0] == values[2] && values[1] == values[3] ? 1.0 : 0.0;
        }
    }

    class LeftSort implements ExternalFunction{
        @Override
        public int getArity(){
            return 2;
        }

        @Override
        public ArgumentType[] getArgumentTypes(){
            return [ArgumentType.String, ArgumentType.String];
        }

        @Override
        public double getValue(ReadOnlyDatabase db, GroundTerm... args){
            String s1 = args[0].getValue();
            String s2 = args[1].getValue();
            double[] values = deserializeLocations(s1, s2);
            log.info(Double.toString(values[0]));
            log.info(Double.toString(values[2]));
            log.info('------------------------')
            return values[0] - values[2];
        }
    }

    /*
     * getLocations
     * Access the observation partition, get the start and end locations and extract them into two
     * hashsets
     */
    private Set<Term> getLocations(Partition obsPartition){
        def obsDb = ds.getDatabase(obsPartition);
        Set anchorSet = Queries.getAllAtoms(obsDb, Anchor);
        Set<Term> locations = new HashSet<Term>();
        for (Atom a: anchorSet){
            Term[] arguments = a.getArguments();
            locations.add(arguments[0]);
        }
        obsDb.close();
        return locations;
    }

    /*
     * crossAnchor
     * Fill the target partition with the cross product of locations
     */
    private void crossAnchor(Partition obsPartition, Partition targetPartition){
        log.info("Started loading anchor into target partition");
        def locations = getLocations(obsPartition);
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("Location"), locations);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((Anchor(Location)).getFormula(), popMap);
        targetDb.close();
        log.info("Finished loading anchor into target partition");
    }

    /*
     * getTwoLocationPopMap
     * Get the set of locations, make a pop map and fill it twice with the locations
     */
    private Map<Variable, Set<Term>> getTwoLocationPopMap(Partition obsPartition){
        def locations = getLocations(obsPartition);
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("Location1"), locations);
        popMap.put(new Variable("Location2"), locations);
        return popMap;
    }

    /*
     * crossFrequentTrips
     * Same as crossAnchor, but for frequent trips, and add the second set of locations
     */
    private void crossFrequentTrips(Partition obsPartition, Partition targetPartition){
        log.info("Started loading FrequentTrips into target partition");
        Map<Variable, Set<Term>> popMap = getTwoLocationPopMap(obsPartition);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((FrequentTrip(Location1, Location2)).getFormula(), popMap);
        Set s = Queries.getAllAtoms(targetDb, FrequentTrip);
        log.info('There are ' + s.size() + ' frequent trips in target partition');
        targetDb.close();
        log.info("Finished loading FrequentTrips into target partition");
        
    }

    /*
     * crossFrequentTripModes
     * Take the two location popmap and add modes to it, then add it to the target partition
     */
    private void crossFrequentTripModes(Partition obsPartition, Partition targetPartition){
        log.info("Started loading FrequentTripsModes into target partition");
        Map<Variable, Set<Term>> popMap = getTwoLocationPopMap(obsPartition);
        Set<Term> modes = new HashSet<Term>();
        def obsDb = ds.getDatabase(obsPartition);
        Set modeSet = Queries.getAllAtoms(obsDb, Mode)
        for (Atom a: modeSet){
            Term[] arguments = a.getArguments();
            modes.add(arguments[1]);
        }
        popMap.put(new Variable("ModeTerm"), modes);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((FrequentTripMode(Location1, Location2, ModeTerm)).getFormula(), popMap);
        Set s = Queries.getAllAtoms(targetDb, FrequentTripMode);
        log.info('There are ' + s.size() + ' frequent trips + modes in target partition')
        obsDb.close();
        targetDb.close();
        log.info("Finished loading FrequentTripModes into target partition");
    }

    // private void crossSameDaySegments(Partition obsPartition, Partition targetPartition){
    //     log.info("Started loading SameDaySegments into the database");
    //     def obsDb = ds.getDatabase(obsPartition);
    //     Set segmentsSet = Queries.getAllAtoms(obsDb, Segment);
    //     Set<Term> segments = new HashSet<Term>();
    //     for (Atom a: segmentsSet){
    //         Term[] arguments = a.getArguments();
    //         segments.add(arguments[1]);
    //     }
    //     obsDb.close();
    //     Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>()
    //     popMap.put(new Variable("Segment"), segments);
    //     def targetDb = ds.getDatabase(targetsPartition);
    //     DatabasePopulator dbPop = new DatabasePopulator(targetDb);
    //     dbPop.populate((SameDaySegment))
    // }

    /*
     * crossFrequentTripTimes
     * Same as crossFrequentTripModes but for times
     */
    private void crossFrequentTripTimes(Partition obsPartition, Partition targetPartition){
        log.info("Started loading FrequentTripTimes into target partition");
        Map<Variable, Set<Term>> popMap = getTwoLocationPopMap(obsPartition);
        Set<Term> times = getTimesSet(obsPartition);
        log.info("# Times: " + times.size());
        popMap.put(new Variable("Time1"), times);
        popMap.put(new Variable("Time2"), times);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((FrequentTripTime(Location1, Location2, Time1, Time2)).getFormula(), popMap);
        targetDb.close();
        log.info("Finished loading FrequentTripTimes into target partition");
    }



    /*
     * getTimesSet
     * Return a Set consisting of the time terms from StartTime and EndTime
     */
    Set<Term> getTimesSet(Partition obsPartition){
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
            times.add(arguments[1]);
        }
        obsDb.close();
        return times;
    }

    
    private void loadData(Partition obsPartition, Partition targetsPartition, Partition truthPartition) {
        log.info("Loading data into database");

        Inserter inserter = ds.getInserter(Cluster, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "clusters.txt").toString());

        // Fill all of the obs partition from files
        inserter = ds.getInserter(Segment, obsPartition);
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
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "mode_obs.txt").toString());

        inserter = ds.getInserter(SegmentDay, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "segment_days_obs.txt").toString());

        inserter = ds.getInserter(Anchor, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "grounded_anchors.txt").toString());

        // Run the cross functions to fill the targets partition
        crossFrequentTripTimes(obsPartition, targetsPartition);
        crossFrequentTrips(obsPartition, targetsPartition);
        crossFrequentTripModes(obsPartition, targetsPartition);
    }

    // Run inference
    private FullInferenceResult runInference(Partition obsPartition, Partition targetsPartition) {
        log.info("Starting inference");

        Date infStart = new Date();
        HashSet closed = new HashSet<StandardPredicate>([Anchor,StartLocation,EndLocation,StartTime,EndTime,Segment,Mode,SegmentDay]);
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
        PrintStream ps = new PrintStream(new File(Paths.get(config.outputPath, "frequents_infer.txt").toString()));
        System.setOut(ps);

        AtomPrintStream aps = new DefaultAtomPrintStream();
        Set frequentSet = Queries.getAllAtoms(resultsDB, FrequentTrip);
        for (Atom a : frequentSet) {
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
        ConfigBundle cb = ConfigManager.getManager().getBundle("finfer");
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
        InferFrequentTrips bp = new InferFrequentTrips(cb);
        bp.run();
    }
}