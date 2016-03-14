package fertilityModel;

import org.apache.commons.math3.distribution.NormalDistribution;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

public class Params {

	//List of parameters to set for each run
	final static Parameters p = RunEnvironment.getInstance().getParameters();

	//system
	public static int numThreads = 1;
	
	//landscape
	public static int landscapeSize = 100;
	
	//nodes
	public static int numberOfNodes = 100;
	public static int radiusOfConnections = 10;
	public static int connection_mean = 6;
	public static int connection_sd = 1;
	public final static NormalDistribution numb_connections = new NormalDistribution(connection_mean,connection_sd);
	
	//decision making
	public static double sterr_noise = 1;
	

	//assuming asymetric network ties: individuals can be connected to someone and the individual not be connected back (could change)
	
	
	//Constructor: used to set values from batch runs or the GUI
	public Params(){
			//randomSeed = (Integer)RandomHelper.nextIntFromTo(0, 1000000);
			numThreads = (Integer)p.getValue("numbThreads");
			numberOfNodes = (Integer)p.getValue("numbNodes");
			radiusOfConnections = (Integer)p.getValue("radiusC");
			connection_mean = (Integer)p.getValue("meanC");
			connection_sd = (Integer)p.getValue("sdC");
			sterr_noise = (Integer)p.getValue("noise");
			landscapeSize = (Integer)p.getValue("landscapeSize");
		}
}
