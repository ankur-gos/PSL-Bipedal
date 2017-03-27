/*
 * Bipedal.groovy
 * Bipedal main file
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
    private void addPredicates(){
        model.add predicate: "Segment", types: [ArgumentType.UniqueID]
        model.add predicate: "StartLocation", types: [ArgumentType.UniqueID, ArgumentType.Double, ArgumentType.Double]
        model.add predicate: "EndLocation", types: [ArgumentType.UniqueID, ArgumentType.Double, ArgumentType.Double]
        model.add predicate: "StartTime", types: [ArgumentType.UniqueID, ArgumentType.Date]
        model.add predicate: "EndTime", types: [ArgumentType.UniqueID, ArgumentType.Date]
        model.add predicate: "Mode", types: [ArgumentType.UniqueID, ArgumentType.String]
        model.add predicate: "AnchorTime", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.Date]
        model.add predicate: "AnchorMode", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.String]
        model.add predicate: "Anchor", types: [ArgumentType.Double, ArgumentType.Double]
    }

    
}
