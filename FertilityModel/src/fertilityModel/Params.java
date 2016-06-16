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
	public static int landscapeSize = 1000;
	
	//nodes
	public static int numberOfNodes = 50;
	public static int maxAge = 80;
	
	//nodes fertility
	public final static double backgroundFertility = 0.0;   	//random chance of having a baby
	public final static int ageSocial = 10;
	public final static int ageWork = 18;
	public final static double timePerChild = 0.9;
	public final static int maxAgeRepro = 40;
	public final static int minInterBirthPeriod = 2;
	public final static double rDrift_fertility = 0.01; 						//max amount of random drift in fertility desire
	public final static double rDrift_ageOfFirstBirth = 0.01; 					//max amount of random drift in age at first birth
	public final static double conformity = 0.05; 								//rate of adjustment to the average within an individuals social network (per year max)
	
	//nodes social network
	public final static double maxSocialDistance = 30;
	public final static int ageDiffMax = 10;
	public final static double minEdgeWeight=0.1;
	public final static double alpha=1;
	public final static double timeResolution = 0.1; 			//resolution to divide the time when randomly attributing it to social connections
	public final static double socialDecrease = 0.05;			//rate at which social ties decrease
	
	//node economy
	public final static double workToWealthEfficiency = 0.1;		//each unit of work time equals X number of units of material wealth
	public final static double workLifeBalanceVar = 0.1;
	
	//spatial distribution
	public final static double dispersalRadius = 30;
	
	public final static double crisisT = 0.6;
	public final static double rDrift_work = 0.1; 						//max amount of random drift in fertility desire
	

	//assuming asymetric network ties: individuals can be connected to someone and the individual not be connected back (could change)
	
	
	//Constructor: used to set values from batch runs or the GUI
	public Params(){
			//randomSeed = (Integer)RandomHelper.nextIntFromTo(0, 1000000);
			//numThreads = (Integer)p.getValue("numbThreads");
			//numberOfNodes = (Integer)p.getValue("numbNodes");
			//radiusOfConnections = (Integer)p.getValue("radiusC");
			//connection_mean = (Integer)p.getValue("meanC");
			//connection_sd = (Integer)p.getValue("sdC");
			//sterr_noise = (Integer)p.getValue("noise");
			//landscapeSize = (Integer)p.getValue("landscapeSize");
		}
}
