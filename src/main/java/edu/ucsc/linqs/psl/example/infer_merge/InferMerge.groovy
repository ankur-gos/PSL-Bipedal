/*
 * InferMerge.groovy
 * Trip inference
 * Ankur Goswami, agoswam3@ucsc.edu
 */

package edu.ucsc.linqs.psl.example.infer_merge;

//*import org.linqs.psl.application.inference.MPEInference;
//import org.linqs.psl.application.learning.weight.em.HardEM;
//import org.linqs.psl.config.ConfigBundle;
//import org.linqs.psl.config.ConfigManager;
//import org.linqs.psl.database.Database;
//import org.linqs.psl.database.DatabasePopulator;
//import org.linqs.psl.database.DataStore;
//import org.linqs.psl.database.loading.Inserter;
//import org.linqs.psl.database.Partition;
//import org.linqs.psl.application.learning.weight.em.ExpectationMaximization;
//import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver;
//import org.linqs.psl.database.rdbms.driver.H2DatabaseDriver.Type;
//import org.linqs.psl.database.rdbms.RDBMSDataStore;
//import org.linqs.psl.database.ReadOnlyDatabase;
//import org.linqs.psl.evaluation.result.FullInferenceResult;
//import org.linqs.psl.evaluation.resultui.printer.AtomPrintStream;
//import org.linqs.psl.evaluation.resultui.printer.DefaultAtomPrintStream;
//import org.linqs.psl.evaluation.statistics.ContinuousPredictionComparator;
//import org.linqs.psl.evaluation.statistics.DiscretePredictionComparator;
//import org.linqs.psl.evaluation.statistics.DiscretePredictionStatistics;
//import org.linqs.psl.groovy.PSLModel;
//import org.linqs.psl.model.argument.ConstantType;
//import org.linqs.psl.model.atom.Atom;
//import org.linqs.psl.model.predicate.StandardPredicate;
//import org.linqs.psl.ui.loading.InserterUtils;
//import org.linqs.psl.util.database.Queries;

import org.linqs.psl.model.function.ExternalFunction;
import org.linqs.psl.model.term.Constant;
//import org.linqs.psl.model.argument.Term;
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

import java.util.HashSet;
//import edu.umd.cs.psl.model.argument.Variable;
import java.lang.Double;
import java.lang.Math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.time.TimeCategory;
import java.nio.file.Paths

public class InferMerge{
    private static final String PARTITION_OBSERVATIONS = "observations";
    private static final PARTITION_TARGETS = "targets";
    private static final PARTITION_TRUTH = "truth";

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
    public InferMerge(ConfigBundle cb) {
        log = LoggerFactory.getLogger(this.class);
        config = new PSLConfig(cb);
        ds = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, Paths.get(config.dbPath, 'fmerge').toString(), true), cb);
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
        model.add predicate: "Anchor", types: [ConstantType.String];

        // Frequent trips
        model.add predicate: "FrequentTrip", types: [ConstantType.String, ConstantType.String];
        model.add predicate: "FrequentTripTime", types: [ConstantType.String, ConstantType.String, ConstantType.String, ConstantType.String];
        model.add predicate: "FrequentTripMode", types: [ConstantType.String, ConstantType.String, ConstantType.String];
        model.add predicate: "SegmentDay", types: [ConstantType.UniqueID, ConstantType.String];
        model.add predicate: "FrequentTripModeTime", types: [ConstantType.String,ConstantType.String,ConstantType.String,ConstantType.String,ConstantType.String];
    }

    // Functions
    private void defineFunctions(){
        model.add function: "LongerTrip", implementation: new TripComparison();
        model.add function: "Before", implementation: new BeforeCompare();
		model.add function: "SimilarTimes", implementation: new SimilarTimes();
        model.add function: "SlowerTransport", implementation: new RankTransport();
    }

    private void defineRules(){
        log.info("Defining model rules");
        model.add rule: ~FrequentTripModeTime(L1, L2, T1, T2, M), weight: 5;
        model.add rule: ~FrequentTripModeTime(L, L, T1, T2, M), weight: 10000;
        
        model.add rule: (FrequentTripTime(L1, L2, T1, T2) & FrequentTripMode(L1, L2, M)) >> FrequentTripModeTime(L1, L2, M, T1, T2), weight: 1;
        
        model.add rule: (FrequentTripMode(L1, L2, M) & FrequentTripMode(L2, L3, M) & FrequentTripTime(L1, L2, T1, T2) & FrequentTripTime(L2, L3, T1, T3) & Before(T2, T3)) >> FrequentTripModeTime(L1, L3, M, T1, T3), weight: 5;

        model.add rule: (FrequentTripMode(L1, L2, M) & FrequentTripMode(L2, L3, M) & FrequentTripTime(L1, L2, T1, T2) & FrequentTripTime(L2, L3, T2, T3)) >> FrequentTripModeTime(L1, L3, M, T1, T3), weight: 3;

        //model.add rule: (FrequentTripTime(L1, L2, T1, T2) & FrequentTripTime(L1, L3, T1, T3) & Before(T2, T3)) & FrequentTripMode(L1, L3, M) >> FrequentTripModeTime(L1, L3, M, T1, T3), weight: 1;


        //model.add rule: (FrequentTripModeTime(L1, L2, M, T1, T2) & SimilarTimes(T1, T2)) >> FrequentTripModeTime(L1, L2, M, T1, T2), weight: 1;
        model.add rule: (FrequentTripTime(L1, L2, T1, T2) & FrequentTripTime(L1, L2, T3, T4) & SimilarTimes(T1, T3) & SimilarTimes(T2, T4) & Before(T1, T3) & Before(T3, T4) & FrequentTripMode(L1, L2, M)) >> ~FrequentTripModeTime(L1, L2, M, T2, T4), weight: 1000;
        model.add rule: (FrequentTripMode(L1, L2, M1) & FrequentTripMode(L1, L2, M2) & SlowerTransport(M1, M2) & FrequentTripTime(L1, L2, T1, T2)) >> ~FrequentTripModeTime(L1, L2, M1, T1, T2), weight: 1000; 
   }

    /*
        TODO: Why arbitrary two locations deserializations...
    */
    public double[] deserializeLocations(String s1, String s2){
        String[] split1 = s1.split(" ");
        String[] split2 = s2.split(" ");
        double x1 = Double.parseDouble(split1[0]);
        double y1 = Double.parseDouble(split1[1]);
        double x2 = Double.parseDouble(split2[0]);
        double y2 = Double.parseDouble(split2[1]);
        return [x1, y1, x2, y2];
    }

    public double[] deserializeTimes(String s1, String s2){
				String[] split1 = s1.split(":");
				String[] split2 = s2.split(":");
				double x1 = Double.parseDouble(split1[0]);
        double y1 = Double.parseDouble(split1[1]);
        double x2 = Double.parseDouble(split2[0]);
        double y2 = Double.parseDouble(split2[1]);
        return [x1, y1, x2, y2];
 
		}




    class BeforeCompare implements ExternalFunction {
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
            double[] values = deserializeTimes(s1, s2);
	    if(values[0] < values[2])
		return 1;
	    else if(values[0] == values[2])
                return values[1] <= values[3] ? 1 : 0;
	    return 0;
        }
    }
    class RankTransport implements ExternalFunction{
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
            String m1 = args[0].getValue();
            String m2 = args[1].getValue();
            double[] vals = [5, 5];
            for(int i = 0; i < 2; i++){
                if(args[i].getValue() == "automotive")
                    vals[i] = 3;
                if(args[i].getValue() == "cycling")
                    vals[i] = 2;
                if(args[i].getValue() == "walking")
                    vals[i] = 0;
                if(args[i].getValue() == "running")
                    vals[i] = 1;
            }
            //log.info(vals)
            return vals[0] < vals[1] ? 1.0 : 0.0;
        }
    }

		class SimilarTimes implements ExternalFunction {

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
            double[] values = deserializeTimes(s1, s2);
						double tdist = (values[0] - values[2]).abs();
						if(tdist <= 1)
							return 1;
            return 1 / tdist;
        }
    }

    class TripComparison implements ExternalFunction{
        @Override
        public int getArity(){
            return 4;
        }

        @Override
        public ConstantType[] getArgumentTypes(){
            return [ConstantType.String, ConstantType.String, ConstantType.String, ConstantType.String].toArray();
        }

        @Override
        public double getValue(ReadOnlyDatabase db, Constant... args){
            String s1 = args[0].getValue();
            String s2 = args[1].getValue();
            String s3 = args[2].getValue();
            String s4 = args[3].getValue();
            double[] values = deserializeLocations(s1, s2);
            double[] values2 = deserializeLocations(s3, s4);
            double x_squared = (values[0] - values[2]) * (values[0] - values[2])
            double y_squared = (values[1] - values[3]) * (values[1] - values[3])
            // No need to square, since we are just comparing
            double dist1 = x_squared + y_squared

            x_squared = (values2[0] - values2[2]) * (values2[0] - values2[2])
            y_squared = (values2[1] - values2[3]) * (values2[1] - values2[3])
            double dist2 = x_squared + y_squared
            return (dist1 >= dist2) ? 1 : 0;
        }
    }

    private Set<Term> getFrequentTrips(Partition obsPartition){
        def obsDb = ds.getDatabase(obsPartition);
        Set frequentsSet = Queries.getAllAtoms(obsDb, FrequentTrip);
        Set<Term> frequents = new HashSet<Term>();
        for (Atom a: frequentsSet){
            Term[] arguments = a.getArguments();
            frequents.add(arguments[0]);
            frequents.add(arguments[1]);
        }
        obsDb.close();
        return frequents;
    }

    private Set<Term> getFrequentTripModes(Partition obsPartition){
        def obsDb = ds.getDatabase(obsPartition);
        Set<Term> frequents = new HashSet<Term>();
        Set frequentsSet = Queries.getAllAtoms(obsDb, FrequentTripMode);
        for (Atom a: frequentsSet){
            Term[] arguments = a.getArguments();
            for(int i = 0; i < 3; ++i){
                frequents.add(arguments[i]);
            }
        }
        obsDb.close();
        return frequents;
    }

    private Set<Term> getFrequents(Partition obsPartition){
        def obsDb = ds.getDatabase(obsPartition);
        Set<Term> frequents = new HashSet<Term>();
        Set frequentsTimes = Queries.getAllAtoms(obsDb, FrequentTripTime);
        Set frequentsModes = Queries.getAllAtoms(obsDb, FrequentTripMode);

        for (Atom a: frequentsTimes){
            Term[] arguments = a.getArguments();
            for(int i = 0; i < 2; ++i){
                frequents.add(arguments[i]);
            }
        }
        for (Atom a: frequentsModes){
            Term[] arguments = a.getArguments();
            for(int i = 0; i < 2; ++i){
                frequents.add(arguments[i]);
            }
        }

        obsDb.close();
        return frequents;
    }


    /*
     * getTwoLocationPopMap
     * Get the set of locations, make a pop map and fill it twice with the locations
     */
    private Map<Variable, Set<Term>> getTwoLocationPopMap(Partition obsPartition){
        def locations = getFrequents(obsPartition);
        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        popMap.put(new Variable("Location1"), locations);
        popMap.put(new Variable("Location2"), locations);
        return popMap;
    }

    private void crossFrequentModeTrips(Partition obsPartition, Partition targetPartition){
        log.info("Started loading FrequntModeTrips into target partition");

        Map<Variable, Set<Term>> popMap = new HashMap<Variable, Set<Term>>();
        def obsDb = ds.getDatabase(obsPartition);

        Set frequentMode = Queries.getAllAtoms(obsDb, FrequentTripMode);
        Set frequentTimes = Queries.getAllAtoms(obsDb, FrequentTripTime);
        log.info(frequentTimes.size() + " #FTT");
        log.info(frequentMode.size() + " #FTM");

        Set<Term> startLocations = new HashSet<Term>();
        for (Atom a: frequentMode){
            Term[] arguments = a.getArguments();
            startLocations.add(arguments[0]);
        }
        popMap.put(new Variable("StartLocation1"), startLocations);

        Set<Term> endLocations = new HashSet<Term>();
        for (Atom a: frequentMode){
            Term[] arguments = a.getArguments();
            endLocations.add(arguments[1]);
        }
        for (Atom a: frequentTimes){
            Term[] arguments = a.getArguments();
            endLocations.add(arguments[1]);
        }
        popMap.put(new Variable("EndLocation1"), endLocations);

        Set<Term> modes = new HashSet<Term>();
        for (Atom a: frequentMode){
            Term[] arguments = a.getArguments();
            modes.add(arguments[2]);
        }
        popMap.put(new Variable("Mode1"), modes);

        Set<Term> startTimes = new HashSet<Term>();
        for (Atom a: frequentTimes){
            Term[] arguments = a.getArguments();
            startTimes.add(arguments[2]);
        }
        popMap.put(new Variable("StartTime1"), startTimes);


        Set<Term> endTimes = new HashSet<Term>();
        for (Atom a: frequentTimes){
            Term[] arguments = a.getArguments();
            endTimes.add(arguments[3]);
        }
        popMap.put(new Variable("EndTime1"), endTimes);

        def targetDb = ds.getDatabase(targetPartition);
        DatabasePopulator dbPop = new DatabasePopulator(targetDb);
        dbPop.populate((FrequentTripModeTime(StartLocation1, EndLocation1, Mode1, StartTime1, EndTime1)).getFormula(), popMap);
        Set s = Queries.getAllAtoms(targetDb, FrequentTripModeTime);
        
        //AtomPrintStream aps = new DefaultAtomPrintStream();
        //for (Atom a : s) {
        //    aps.printAtom(a);
        //}

        //aps.close();
        log.info('There are ' + s.size() + ' FrequentTripModeTime in target partition.')
        obsDb.close();
        targetDb.close();
        log.info("Finished loading FrequentTripTimes into target partition");
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

        inserter = ds.getInserter(SegmentDay, obsPartition);
        InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "segment_days_obs.txt").toString());

        inserter = ds.getInserter(Anchor, obsPartition);
        InserterUtils.loadDelimitedDataTruth(inserter, Paths.get(config.dataPath, "grounded_anchors.txt").toString());

        inserter = ds.getInserter(FrequentTrip, obsPartition);
        InserterUtils.loadDelimitedDataTruth(inserter, Paths.get(config.dataPath, "grounded_frequents.txt").toString());

        inserter = ds.getInserter(FrequentTripTime, obsPartition);
        InserterUtils.loadDelimitedDataTruth(inserter, Paths.get(config.dataPath, "grounded_frequent_times.txt").toString());

        inserter = ds.getInserter(FrequentTripMode, obsPartition);
        InserterUtils.loadDelimitedDataTruth(inserter, Paths.get(config.dataPath, "grounded_frequent_modes.txt").toString());
        // Run the cross functions to fill the targets partition
        crossFrequentModeTrips(obsPartition, targetsPartition);
    }

    // Run inference
    private void runInference(Partition obsPartition, Partition targetsPartition) {
        log.info("Starting inference");

        Date infStart = new Date();
        HashSet closed = new HashSet<StandardPredicate>([Anchor,StartLocation,EndLocation,StartTime,EndTime,Segment,Mode,SegmentDay, FrequentTrip, FrequentTripMode, FrequentTripTime]);
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
        PrintStream ps = new PrintStream(new File(Paths.get(config.outputPath, "frequent_modes_times_infer.txt").toString()));
        AtomPrintStream aps = new DefaultAtomPrintStream(ps);
        Set atomSet = Queries.getAllAtoms(resultsDB, FrequentTripModeTime);
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
        //evalResults(targetsPartition, truthPartition);

        ds.close();
    }

    /**
     * Populates the ConfigBundle for this PSL program using arguments specified on
     * the command line
     * @param args - Command line arguments supplied during program invocation
     * @return ConfigBundle with the appropriate properties set
     */
    public static ConfigBundle populateConfigBundle(String[] args) {
        ConfigBundle cb = ConfigManager.getManager().getBundle("fmerge");
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
        InferMerge bp = new InferMerge(cb);
        bp.run();
    }
}
