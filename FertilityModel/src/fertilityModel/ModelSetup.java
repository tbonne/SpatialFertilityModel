package fertilityModel;

import java.util.ArrayList;
import java.util.Collections;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;

import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.gis.GeographyFactory;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.context.space.graph.NetworkBuilder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameter;
import repast.simphony.query.space.continuous.ContinuousWithin;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;
import repast.simphony.space.graph.Network;

public class ModelSetup implements ContextBuilder<Object>{

	private static Context mainContext;
	private static Geography geog;
	public static ContinuousSpace <Object > space;
	public static ArrayList<Node> allNodes;
	public static ArrayList<Node> allInputNodes;
	public static ArrayList<Edge> allEdges;
	public static Quadtree spaceEnv;
	public static Network network;

	private static int nodeSize ;
	private static int landSize ;

	public Context<Object> build(Context<Object> context){

		System.out.println("Running Fertility model");

		/********************************
		 * 								*
		 * initialize model parameters	*
		 * 								*
		 *******************************/

		mainContext = context; //static link to context
		allNodes = new ArrayList<Node>();	
		allInputNodes = new ArrayList<Node>();	
		allEdges = new ArrayList<Edge>();
		Params p = new Params();

		nodeSize = p.numberOfNodes;
		landSize = p.landscapeSize;

		System.out.println("Building geog");

		//Create Geometry factory; used to create gis shapes (points=primates; polygons=resources)
		GeometryFactory fac = new GeometryFactory();

		GeographyParameters<Object> params= new GeographyParameters<Object>();
		GeographyFactory factory = GeographyFactoryFinder.createGeographyFactory(null);
		geog = factory.createGeography("geog", context, params);

		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		space = spaceFactory.createContinuousSpace("space", context , new RandomCartesianAdder <Object >(), new repast.simphony.space.continuous.StrictBorders(), landSize, landSize); 

		NetworkBuilder <Object > netBuilder = new NetworkBuilder <Object > ("infection network", context , true); 
		Network globalNet = netBuilder.buildNetwork();
		network=globalNet;

		/************************************
		 * 							        *
		 * Adding Nodes to the landscape	*
		 * 							        *
		 * *********************************/

		System.out.println("adding nodes"); //altering this to be a network space might be best ... no /error for gis visualization...manual
		
		Coordinate groupCoord = new Coordinate(RandomHelper.nextDoubleFromTo((0),(landSize)), RandomHelper.nextDoubleFromTo(0,(landSize)));

		for (int j = 0; j < nodeSize; j++){

			//add node
			Coordinate coord = new Coordinate(RandomHelper.nextDoubleFromTo(groupCoord.x+(-Params.maxSocialDistance),(Params.maxSocialDistance)), groupCoord.y+RandomHelper.nextDoubleFromTo(-Params.maxSocialDistance,(Params.maxSocialDistance)));
			Node node = new Node(context,space,coord,globalNet);
			allNodes.add(node);
			context.add(node);
			//Point geom = fac.createPoint(coord);
			//geog.move(node,geom);
			space.moveTo(node, coord.x,coord.y);
		}

		/************************************
		 * 							        *
		 * Adding Edges to the landscape	*
		 * 							        *
		 * *********************************/		

		
		Network <Object > net = (Network <Object >)context.getProjection("infection network");

		int nEdges = 0;
		for(int i = 0;i<allNodes.size();i++){
			Node nodeStart = allNodes.get(i);
			//nEdges = ((Double)Params.numb_connections.sample()).intValue();
			//IndividualConnection.setConnections(nEdges, nodeStart, context, net);
			if(i>0)net.addEdge(nodeStart, allNodes.get(0));
		}


		/************************************
		 * 							        *
		 * Scheduler to synchronize runs	*
		 * 							        *
		 * *********************************/

		//executor takes care of the processing of the schedule
		Executor executor = new Executor();
		createSchedule(executor);

		return context;

	}


	private void createSchedule(Executor executor){

		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		
		ScheduleParameters agentStepParams_space = ScheduleParameters.createRepeating(1, 1, 7); //start, interval, priority (high number = higher priority)
		schedule.schedule(agentStepParams_space,executor,"processSpace");

		ScheduleParameters agentStepParams_Nodes = ScheduleParameters.createRepeating(1, 1, 6); //start, interval, priority (high number = higher priority)
		schedule.schedule(agentStepParams_Nodes,executor,"processNodes");

		ScheduleParameters agentStepParams_death = ScheduleParameters.createRepeating(1, 1, 5); //start, interval, priority (high number = higher priority)
		schedule.schedule(agentStepParams_death,executor,"updateNodes");

	}


	public static  Iterable<Node> getObjectsWithin(Coordinate coord,double radius, Class clazz){
		Envelope envelope = new Envelope();
		envelope.init(coord);
		envelope.expandBy(radius);
		Iterable<Node> objectsInArea = ModelSetup.getGeog().getObjectsWithin(envelope,clazz);
		envelope.setToNull();

		//if(objectsInArea.iterator().hasNext()==false)System.out.println("warning no objects found of class: "+ clazz.toString());
		return objectsInArea;
	}

	public static Context getContext(){
		return mainContext;
	}
	public static Geography getGeog(){
		return geog;
	}
	public static ContinuousSpace getSpace(){
		return space;
	}
	public static ArrayList<Node> getAllNodes(){
		return allNodes;
	}
	public static Quadtree getQuadtree(){
		return spaceEnv;
	}
	public static Network getNetwork(){
		return network;
	}
	public static void setQuadtree(Quadtree s){
		spaceEnv=s;
	}


}
