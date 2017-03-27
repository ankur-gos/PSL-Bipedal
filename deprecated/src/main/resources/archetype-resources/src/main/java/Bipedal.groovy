/*
 * Bipedal.groovy
 * Model creation for the bipedal project
 * Ankur Goswami, agoswam3@ucsc.edu
 */

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )

package ${package};

import java.text.DecimalFormat;

import edu.umd.cs.psl.application.inference.MPEInference
import edu.umd.cs.psl.application.learning.weight.maxlikelihood.MaxLikelihoodMPE
import edu.umd.cs.psl.config.*
import edu.umd.cs.psl.database.DataStore
import edu.umd.cs.psl.database.Database
import edu.umd.cs.psl.database.DatabasePopulator
import edu.umd.cs.psl.database.Partition
import edu.umd.cs.psl.database.ReadOnlyDatabase;
import edu.umd.cs.psl.database.rdbms.RDBMSDataStore
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver
import edu.umd.cs.psl.database.rdbms.driver.H2DatabaseDriver.Type
import edu.umd.cs.psl.groovy.PSLModel;
import edu.umd.cs.psl.groovy.PredicateConstraint
import edu.umd.cs.psl.groovy.SetComparison
import edu.umd.cs.psl.model.argument.ArgumentType
import edu.umd.cs.psl.model.argument.GroundTerm;
import edu.umd.cs.psl.model.argument.UniqueID;
import edu.umd.cs.psl.model.argument.Variable;
import edu.umd.cs.psl.model.atom.GroundAtom;
import edu.umd.cs.psl.model.function.ExternalFunction;
import edu.umd.cs.psl.ui.functions.textsimilarity.*
import edu.umd.cs.psl.ui.loading.InserterUtils;
import edu.umd.cs.psl.util.database.Queries;

// Set the configuration
ConfigManager cm = ConfigManager.getManager()
ConfigBundle config = cm.getBundle("bipedal")

/* Uses H2 as a DataStore and stores it in a temp. directory by default */
def defaultPath = System.getProperty("java.io.tmpdir")
String dbpath = config.getString("dbpath", defaultPath + File.separator + "bipedal")
DataStore data = new RDBMSDataStore(new H2DatabaseDriver(Type.Disk, dbpath, true), config)

PSLModel m = new PSLModel(this, data)

// Predicates
m.add predicate: "Segment", types: [ArgumentType.UniqueID]
m.add predicate: "StartLocation", types: [ArgumentType.UniqueID, ArgumentType.Double, ArgumentType.Double]
m.add predicate: "EndLocation", types: [ArgumentType.UniqueID, ArgumentType.Double, ArgumentType.Double]
m.add predicate: "StartTime", types: [ArgumentType.UniqueID, ArgumentType.Date]
m.add predicate: "EndTime", types: [ArgumentType.UniqueID, ArgumentType.Date]
m.add predicate: "Mode", types: [ArgumentType.UniqueID, ArgumentType.String]
m.add predicate: "AnchorTime", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.Date]
m.add predicate: "AnchorMode", types: [ArgumentType.Double, ArgumentType.Double, ArgumentType.String]
m.add predicate: "Anchor", types: [ArgumentType.Double, ArgumentType.Double]

// Functions
m.add function: "EqualLocations", implementation: new LocationComparison()
m.add function: "Near", implementation: new ManhattanNear()

// TODO: Add weights
// Rules
m.add rule: (Segment(S) && StartLocation(S, X, Y) && StartTime(S, T)) >> AnchorTime(X, Y, T)
m.add rule: (Segment(S) && EndLocation(S, X, Y) && EndTime(S, T)) >> AnchorTime(X, Y, T)
m.add rule: (Segment(S) && StartLocation(S, X, Y) && Mode(S, M)) >> AnchorMode(X, Y, M)
m.add rule: (Segment(S) && EndLocation(S, X, Y) && Mode(S, M)) >> AnchorMode(X, Y, M)
m.add rule: (AnchorMode(X, Y, M)) >> Anchor(X, Y)
m.add rule: (AnchorTime(X, Y, T)) >> Anchor(X, Y)
m.add rule: (AnchorTime(X1, Y1, T) && AnchorTime(X2, Y2, T) && ~EqualLocations(X1, Y1, X2, Y2)) >> ~Anchor(X2, Y2)
m.add rule: (Anchor(X1, Y1) && Near(X1, Y1, X2, Y2) && ~EqualLocations(X1, Y1, X2, Y2)) >> ~Anchor(X2, Y2)

class ManhattanNear implements ExternalFunction {

    @Override
    public int getArity(){
        return 4
    }

    @Override
    public ArgumentType[] getArgumentTypes(){
        return [ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double]
    }

    @Override
    public double getValue(ReadOnlyDatabase db, GroundTerm... args){
        mdist = (args[0].toDouble() - args[2].toDouble()).abs() + (args[1].toDouble() - args[3].toDouble()).abs()
        return mdist < 100 ? 1.0 : 0.0
    }
}

class LocationComparison implements ExternalFunction {

    @Override
    public int getArity(){
        return 4
    }

    @Override
    public ArgumentType[] getArgumentTypes(){
        return [ArgumentType.Double, ArgumentType.Double, ArgumentType.Double, ArgumentType.Double]
    }

    @Override
    public double getValue(ReadOnlyDatabase db, GroundTerm... args){
        return args[0].toDouble() == args[2].toDouble() && args[1].toDouble() == args[3].toDouble()
    }
}
