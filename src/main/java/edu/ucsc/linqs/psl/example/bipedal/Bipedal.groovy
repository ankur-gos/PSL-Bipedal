/*
 * Bipedal.groovy
 * Main file for inference
 * Ankur Goswami, agoswam3@ucsc.edu
 */

package edu.ucsc.linqs.psl.example.bipedal;


import org.linqs.psl.model.function.ExternalFunction;
import org.linqs.psl.model.term.Constant;
import org.linqs.psl.model.term.Variable;
import org.linqs.psl.model.term.Term;
import org.linqs.psl.application.inference.MPEInference;
import org.linqs.psl.config.ConfigBundle;
import org.linqs.psl.config.ConfigManager;
import org.linqs.psl.database.Database;
import org.linqs.psl.database.DatabasePopulator;
import org.linqs.psl.database.DataStore;
import org.linqs.psl.database.Partition;
import org.linqs.psl.database.Queries;
import org.linqs.psl.database.ReadOnlyDatabase;
import org.linqs.psl.database.loading.Inserter;
import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver;
import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver.Type;
import org.linqs.psl.database.rdbms.RDBMSDataStore;
import org.linqs.psl.groovy.PSLModel;
import org.linqs.psl.model.atom.Atom;
import org.linqs.psl.model.predicate.StandardPredicate;
import org.linqs.psl.model.term.ConstantType;
import org.linqs.psl.utils.dataloading.InserterUtils;
import org.linqs.psl.utils.evaluation.printing.AtomPrintStream;
import org.linqs.psl.utils.evaluation.printing.DefaultAtomPrintStream;
import org.linqs.psl.utils.evaluation.statistics.ContinuousPredictionComparator;
import org.linqs.psl.utils.evaluation.statistics.DiscretePredictionComparator;
import org.linqs.psl.utils.evaluation.statistics.DiscretePredictionStatistics;
import java.util.HashSet;
import java.lang.Double;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.time.TimeCategory;
import java.nio.file.Paths

public class Bipedal{
    private static final String PARTITION_OBSERVATIONS = "observations";
    private static final String PARTITION_TARGETS = "targets";
    private static final String PARTITION_TRUTH = "truth";

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
    public Bipedal(ConfigBundle cb) {
        log = LoggerFactory.getLogger(this.class);
        config = new PSLConfig(cb);
        ds = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, Paths.get(config.dbPath, 'bipedal').toString(), true), cb);
        model = new PSLModel(this, ds);
    }

    // Predicates
    private void definePredicates(){
        model.add predicate: "Segment", types: [ConstantType.UniqueID];
        model.add predicate: "StartLocation", types: [ConstantType.UniqueID, ConstantType.String];
        model.add predicate: "EndLocation", types: [ConstantType.UniqueID, ConstantType.String];
        model.add predicate: "StartTime", types: [ConstantType.UniqueID, ConstantType.String];
        model.add predicate: "EndTime", types: [ConstantType.UniqueID, ConstantType.String];
        model.add predicate: "Mode", types: [ConstantType.UniqueID, ConstantType.String];
        model.add predicate: "AnchorTime", types: [ConstantType.String, ConstantType.String];
        model.add predicate: "AnchorMode", types: [ConstantType.String, ConstantType.String];
        model.add predicate: "Anchor", types: [ConstantType.String];
    }

    // Functions
    private void defineFunctions(){
        model.add function: "EqualLocations", implementation: new LocationComparison();
        model.add function: "Near", implementation: new ManhattanNear();
        model.add function: "LeftOf", implementation: new LeftSort();
    }

    private void defineRules(){
        log.info("Defining model rules");
        // Anchor locations
        model.add rule: (StartLocation(S, L)) >> Anchor(L), weight: 1
        model.add rule: (EndLocation(S, L)) >> Anchor(L), weight: 1
        // model.add rule: (Segment(S) & StartLocation(S, L) & StartTime(S, T)) >> AnchorTime(L, T),
        //           weight: 2;
        // model.add rule: (Segment(S) & EndLocation(S, L) & EndTime(S, T)) >> AnchorTime(L, T), weight: 2;
        // model.add rule: (Segment(S) & StartLocation(S, L) & Mode(S, M)) >> AnchorMode(L, M), weight: 2;
        // model.add rule: (Segment(S) & EndLocation(S, L) & Mode(S, M)) >> AnchorMode(L, M), weight: 2;
        // model.add rule: (AnchorMode(L, M)) >> Anchor(L), weight: 3;
        // model.add rule: (AnchorTime(L, T)) >> Anchor(L), weight: 3;

        model.add rule: ~Anchor(L), weight: 10;
        //model.add rule: ~AnchorMode(L, M), weght: 1;
        //model.add rule: ~AnchorTime(L, T), weight: 1;
    }

    public double[] deserializeLocations(String s1, String s2){
        String[] split1 = s1.split(" ")
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
        public ConstantType[] getArgumentTypes(){
            return [ConstantType.String, ConstantType.String].toArray();
        }

        @Override
        public double getValue(ReadOnlyDatabase db, Constant... args){
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
        public ConstantType[] getArgumentTypes(){
            return [ConstantType.String, ConstantType.String].toArray();
        }

        @Override
        public double getValue(ReadOnlyDatabase db, Constant... args){
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
        public ConstantType[] getArgumentTypes(){
            return [ConstantType.String, ConstantType.String].toArray();
        }

        @Override
        public double getValue(ReadOnlyDatabase db, Constant... args){
            String s1 = args[0].getValue();
            String s2 = args[1].getValue();
            double[] values = deserializeLocations(s1, s2);
            //log.info(Double.toString(values[0]));
            //log.info(Double.toString(values[2]));
            //log.info('------------------------')
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
        Set startLocationSet = Queries.getAllAtoms(obsDb, StartLocation);
        Set endLocationSet = Queries.getAllAtoms(obsDb, EndLocation);
        Set<Term> locations = new HashSet<Term>();
        for (Atom a: startLocationSet){
            Term[] arguments = a.getArguments();
            locations.add(arguments[1]);
        }
        for (Atom a: endLocationSet){
            Term[] arguments = a.getArguments();
            locations.add(arguments[1]);
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
     * crossLocationMode
     * Access the observation partition, and cross the locations with each possible
     * mode
     */
    private void crossLocationMode(Partition obsPartition, Partition targetPartition){
        log.info("Started loading LocationMode into target partition");
        def locations = getLocations(obsPartition);
        def obsDb = ds.getDatabase(obsPartition);
        Set modeSet = Queries.getAllAtoms(obsDb, Mode);
        Set<Term> modes = new HashSet<Term>();
        for (Atom a: modeSet){
            Term[] arguments = a.getArguments();
            modes.add(arguments[1]);
        }
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("Location"), locations);
        popMap.put(new Variable("ModeTerm"), modes);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((AnchorMode(Location, ModeTerm)).getFormula(), popMap);
        obsDb.close();
        targetDb.close()
        log.info("Finished loading LocationMode into target partition");
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

    /*
     * crossLocationTime
     * Access the observation partition, and cross the locations with each possible
     * time of day
     */
    private void crossLocationTime(Partition obsPartition, Partition targetPartition){
        log.info("Started loading LocationTimes into target partition");
        def locations = getLocations(obsPartition);
        Set<Term> times = getTimesSet(obsPartition);
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("Location"), locations);
        popMap.put(new Variable("Time"), times);
        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((AnchorTime(Location, Time)).getFormula(), popMap);
        targetDb.close();
        log.info("Finished loading LocationTime into target partition");
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
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "mode_obs.txt").toString());

        inserter = ds.getInserter(Anchor, truthPartition);
        InserterUtils.loadDelimitedDataTruth(inserter, Paths.get(config.dataPath, "anchor_truth.txt").toString());

        // Run the cross functions to fill the targets partition
        crossLocationTime(obsPartition, targetsPartition);
        crossLocationMode(obsPartition, targetsPartition);
        crossAnchor(obsPartition, targetsPartition);
    }


    // Run inference
    private void runInference(Partition obsPartition, Partition targetsPartition) {
        log.info("Starting inference");

        Date infStart = new Date();
        HashSet closed = new HashSet<StandardPredicate>([StartLocation,EndLocation,StartTime,EndTime,Segment,Mode]);
        Database inferDB = ds.getDatabase(targetsPartition, closed, obsPartition);
        MPEInference mpe = new MPEInference(model, inferDB, config.cb);
        mpe.mpeInference();

        inferDB.close();
        mpe.close();

        log.info("Finished inference in {}", TimeCategory.minus(new Date(), infStart));
    }

    /**
     * Writes the inference outputs to a file
     */
    private void writeOutput(Partition targetsPartition) {
	Database resultsDB = ds.getDatabase(targetsPartition);
        PrintStream ps = new PrintStream(new File(Paths.get(config.outputPath, "anchors.txt").toString()));
        AtomPrintStream aps = new DefaultAtomPrintStream(ps);
        Set atomSet = Queries.getAllAtoms(resultsDB, Anchor);
        for (Atom a : atomSet) {
            aps.printAtom(a);
        }

        aps.close();
        ps.close();
        resultsDB.close();
    }

    /**
     * Evaluates the results of inference versus expected truth values
     */
    private void evalResults(Partition targetsPartition, Partition truthPartition) {
        Database resultsDB = ds.getDatabase(targetsPartition, [Anchor] as Set);
		Database truthDB = ds.getDatabase(truthPartition, [Anchor] as Set);
		DiscretePredictionComparator dpc = new DiscretePredictionComparator(resultsDB);
		ContinuousPredictionComparator cpc = new ContinuousPredictionComparator(resultsDB);
		dpc.setBaseline(truthDB);
		//	 dpc.setThreshold(0.99);
		cpc.setBaseline(truthDB);
		DiscretePredictionStatistics stats = dpc.compare(Anchor);
		double mse = cpc.compare(Anchor);
		log.info("MSE: {}", mse);
		log.info("Accuracy {}, Error {}",stats.getAccuracy(), stats.getError());
		log.info(
				"Positive Class: precision {}, recall {}",
				stats.getPrecision(DiscretePredictionStatistics.BinaryClass.POSITIVE),
				stats.getRecall(DiscretePredictionStatistics.BinaryClass.POSITIVE));
		log.info("Negative Class Stats: precision {}, recall {}",
				stats.getPrecision(DiscretePredictionStatistics.BinaryClass.NEGATIVE),
				stats.getRecall(DiscretePredictionStatistics.BinaryClass.NEGATIVE));

		resultsDB.close();
		truthDB.close();
    }

    /**
     * Runs the PSL program using configure options - defines a model, loads data,
     * performs inferences, writes output to files, evaluates results
     */
    public void run() {
        log.info("Running experiment {}", config.experimentName);

        Partition obsPartition = ds.getPartition(PARTITION_OBSERVATIONS);
        Partition targetsPartition = ds.getPartition(PARTITION_TARGETS);
        Partition truthPartition = ds.getPartition(PARTITION_TRUTH);

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
