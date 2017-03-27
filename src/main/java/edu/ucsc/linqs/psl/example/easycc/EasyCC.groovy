package edu.ucsc.linqs.psl.example.easycc;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import groovy.time.TimeCategory;
import java.nio.file.Paths;

/**
 * A simple Collective Classification example that mirrors the example for the
 * PSL command line tool.
 * This PSL program uses social relationships to determine where people live.
 * It optionally uses a functional constraint that specified that a person
 * can only live in one location.
 *
 * @author Jay Pujara <jay@cs.umd.edu>
 */
public class EasyCC {
	private static final PARTITION_OBSERVATIONS = 0;
	private static final PARTITION_TARGETS = 1;
	private static final PARTITION_TRUTH = 2;

	private Logger log;
	private DataStore ds;
	private PSLConfig config;
	private PSLModel model;

	/**
	 * Class containing options for configuring the PSL program
	 */
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

	public EasyCC(ConfigBundle cb) {
		log = LoggerFactory.getLogger(this.class);
		config = new PSLConfig(cb);
		ds = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, Paths.get(config.dbPath, 'easycc').toString(), true), cb);
		model = new PSLModel(this, ds);
	}

	/**
	 * Defines the logical predicates used by this program
	 */
	private void definePredicates() {
		model.add predicate: "Lives", types: [ArgumentType.UniqueID, ArgumentType.UniqueID];
		model.add predicate: "Knows", types: [ArgumentType.UniqueID, ArgumentType.UniqueID];
	}

	/**
	 * Defines the rules used to infer unknown variables in the PSL program
	 */
	private void defineRules() {
		log.info("Defining model rules");

		model.add(
			rule: ( Knows(P1,P2) & Lives(P1,L) ) >> Lives(P2,L),
			squared: config.sqPotentials,
			weight : config.weightMap["Knows"]
		);

		model.add(
			rule: ( Knows(P2,P1) & Lives(P1,L) ) >> Lives(P2,L),
			squared: config.sqPotentials,
			weight : config.weightMap["Knows"]
		);

		if (config.useFunctionalRule) {
			model.add(
				rule: Lives(P,L1) >> ~Lives(P,L2),
				squared:config.sqPotentials,
				weight: config.weightMap["Functional"]
			);
		}

		model.add(
			rule: ~Lives(P,L),
			squared:config.sqPotentials,
			weight: config.weightMap["Prior"]
		);

		log.debug("model: {}", model);
	}

	/**
	 * Loads the evidence, inference targets, and evaluation data into the DataStore
	 */
	private void loadData(Partition obsPartition, Partition targetsPartition, Partition truthPartition) {
		log.info("Loading data into database");

		Inserter inserter = ds.getInserter(Knows, obsPartition);
		InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "knows_obs.txt").toString());

		inserter = ds.getInserter(Lives, obsPartition);
		InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "lives_obs.txt").toString());

		inserter = ds.getInserter(Lives, targetsPartition);
		InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "lives_targets.txt").toString());

		inserter = ds.getInserter(Lives, truthPartition);
		InserterUtils.loadDelimitedData(inserter, Paths.get(config.dataPath, "lives_truth.txt").toString());
	}

	/**
	 * Performs inference using the defined model and evidence, storing results in DataStore
	 * @return the FullInferenceResults object from MPE inference
	 */
	private FullInferenceResult runInference(Partition obsPartition, Partition targetsPartition) {
		log.info("Starting inference");

		Date infStart = new Date();
		HashSet closed = new HashSet<StandardPredicate>([Knows]);
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
		PrintStream ps = new PrintStream(new File(Paths.get(config.outputPath, "lives_infer.txt").toString()));
		System.setOut(ps);

		AtomPrintStream aps = new DefaultAtomPrintStream();
		Set atomSet = Queries.getAllAtoms(resultsDB,Lives);
		for (Atom a : atomSet) {
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
		Database resultsDB = ds.getDatabase(targetsPartition, [Lives] as Set);
		Database truthDB = ds.getDatabase(truthPartition, [Lives] as Set);
		DiscretePredictionComparator dpc = new DiscretePredictionComparator(resultsDB);
		dpc.setBaseline(truthDB);
		DiscretePredictionStatistics stats = dpc.compare(Lives);
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
		ConfigBundle cb = ConfigManager.getManager().getBundle("easycc");
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
		EasyCC cc = new EasyCC(cb);
		cc.run();
	}
}
