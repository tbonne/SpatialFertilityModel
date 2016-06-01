package fertilityModel;

import java.awt.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.java.plugin.Plugin;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.graph.ShortestPath;

import com.sun.media.sound.ModelDestination;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class Node {  

	int id,children,age;
	Coordinate coord;
	double desired_Fertility,desired_ageFirstBirth,freeTime,socialTime,workTime,careTime,mWealth,workToWealthEfficiency;
	double workLifeBalance;
	private ContinuousSpace <Object > space; 
	private NormalDistribution noise =null;
	Context context;
	Network network;
	int timeSinceLastBirth,age_FirstBirth,numbChild;
	ArrayList<Node> kinList;
	int dead ;


	public Node(Context cont,ContinuousSpace <Object > space, Coordinate coordinate,Network net) {
		context =cont;
		dead=0;
		
		children = 0;
		timeSinceLastBirth=0;
		network=net;
		age=0;
		desired_Fertility = 0;
		desired_ageFirstBirth = (RandomHelper.nextDouble()-0.5)*30+15; 
		coord = coordinate;
		this.space=space;

		socialTime=0;
		freeTime=1;
		workTime=0;
		careTime=0;
		workLifeBalance=Math.random();
		mWealth=0;
		workToWealthEfficiency = Params.workToWealthEfficiency;

		kinList = new ArrayList<Node>();

	}

	public Node(Context cont,ContinuousSpace <Object > space, Coordinate coordinate,Network net,Node motherNode) {
		children = 0;
		timeSinceLastBirth=0;
		network=net;
		age=0;
		desired_Fertility = RandomHelper.nextDouble()*3;
		desired_ageFirstBirth = (RandomHelper.nextDouble()-0.5)*30+15; 
		coord = coordinate;
		this.space=space;

		socialTime=0;
		freeTime=1;
		workTime=0;
		careTime=0;
		workLifeBalance=Math.random();
		mWealth=0;
		workToWealthEfficiency = Params.workToWealthEfficiency;

		kinList = new ArrayList<Node>();
		kinList.add(motherNode);

	}

	public void init(){

		boolean retval =true;
		while(retval){
			this.coord = new Coordinate(coord.x+Math.random()*Params.dispersalRadius*2-Params.dispersalRadius,coord.y+Math.random()*Params.dispersalRadius*2-Params.dispersalRadius);
			if(this.coord.x<Params.landscapeSize && this.coord.y<Params.landscapeSize && this.coord.x>0 && this.coord.y>0)retval=false;
		}
		space.moveTo(this, coord.x,coord.y);

	}

	public void step(){

		age++;
		if(this.id==1)System.out.println(age);
		timeSinceLastBirth++;

		//allocate my time
		allocateTime();

		//trim social ties
		trimSocialTies();


		//Desired Age of First Birth
		desired_ageFirstBirth = calculateAgeFirstBirth();

		if(age>=desired_ageFirstBirth){
			//Desired fertility
			desired_Fertility = calculateDesiredFertility(); this needs some work... the assumptions might have to be revised (global / immediate fertility goals)

			////Is a new child born
			//deterministic
			if(desired_Fertility>1 && timeSinceLastBirth>=2){
				numbChild++;
				Node newNode = new Node(context, space,this.coord,network);
				context.add(newNode);
				newNode.init();
				ModelSetup.allNodes.add(newNode);
				timeSinceLastBirth=0;
				//System.out.println("birth occured : "+desired_Fertility);
			}
			//random
			if(RandomHelper.nextDouble()>Params.backgroundFertility){

			}
		}
		if(age>Params.maxAge)dead=1;
	}

	/*****************************************methods**************************************************/


	private void allocateTime(){

		//only social time
		if(this.age>Params.ageSocial && this.age<Params.ageWork){

			this.socialTime = 1;
			this.workTime = 0;
			this.careTime = 0;
			this.freeTime = 0;

			//social and work time
		} else if (this.age>=Params.ageWork){

			this.careTime = calculateCareTime();
			this.freeTime = 1 - this.careTime;
			this.socialTime = this.freeTime * this.workLifeBalance;
			this.workTime = this.freeTime * (1-this.workLifeBalance);

		}

		generateSupport(this.socialTime);
		generateMWealth(this.workTime);


	}

	private void generateSupport(double sTime){

		//Divide social time to those in my social network
		double timeA = sTime;

		//1.list all agents within specified distance (Params.sdMax)
		Envelope pointEnv = new Envelope();
		pointEnv.init(this.getCoord());
		pointEnv.expandBy(Params.maxSocialDistance);
		java.util.List<Node> nodesNearMe = ModelSetup.getQuadtree().query(pointEnv);

		ArrayList<Node> nearMe = new ArrayList<Node>();
		ArrayList<Double> nearMeW = new ArrayList<Double>();

		for(Node n : nodesNearMe){
			if(n.getCoord().distance(this.getCoord())<Params.maxSocialDistance && n!=this){
				nearMe.add(n);
			}
		}

		//2. weight the individuals by physical distance, social distance, and age differences
		ShortestPath sp = new ShortestPath(network);
		for(Node n: nearMe){
			double w = 0;

			//physical distance
			w = w + (1-(n.getCoord().distance(this.getCoord())/Params.maxSocialDistance)); //higher indicated more attractive 

			//social distance
			double spd = 1/sp.getPathLength(this, n);
			if(spd>0&&spd<100)w = w + spd;

			//age difference
			if(this.kinList.contains(n)==false && (this.age-n.age)>Params.ageDiffMax  ){
				w = 0;
			}

			//add weight to list
			nearMeW.add(w);

		}


		//3.Distribute social time based on weights
		ArrayList<Node> selected = new ArrayList<Node>();

		while(timeA>0){
			int idN = selectFromList(nearMeW);
			if(idN>-1){
				Node indSelected = nearMe.get(idN);

				double socialTimeAtt = 0;
				if(timeA/sTime>0.2){
					socialTimeAtt = RandomHelper.nextDoubleFromTo(0, timeA);
				} else {
					socialTimeAtt=timeA;
				}

				if(selected.contains(indSelected)==false){
					//add weight to the connection
					RepastEdge e = network.getEdge(this, indSelected);
					if(e != null){
						e.setWeight(e.getWeight()+nearMeW.get(idN));
					} else {
						network.addEdge(this, indSelected, nearMeW.get(idN));
					}
				}

				timeA = timeA - socialTimeAtt;


			} else {
				break;
			}
		}

	}

	private int selectFromList(ArrayList<Double> wei){
		// Compute the total weight of all items together
		double totalWeight = 0.0d;
		for (Double i : wei)
		{
			totalWeight += i;
		}

		// Now choose a random item
		int randomIndex = -1;
		double random = Math.random() * totalWeight;
		for (int i = 0; i < wei.size(); ++i)
		{
			random -= wei.get(i);
			if (random <= 0.0d)
			{
				randomIndex = i;
				break;
			}
		}

		return randomIndex;
	}

	private void generateMWealth (double wTime){
		this.mWealth = this.workTime * this.workToWealthEfficiency;
	}

	private void trimSocialTies(){

		Iterable<Node> allNeigh = network.getAdjacent(this);

		for(Node n : allNeigh){
			RepastEdge edgeAdj =network.getEdge(this, n); 
			if(edgeAdj!=null){
				if(edgeAdj.getWeight()<Params.minEdgeWeight){
					network.removeEdge(edgeAdj);
				}
			}
		}
	}

	private double calculateCareTime(){

		double careTime = this.numbChild * (Params.timePerChild-0.4) - calculateSupportReductionTime() - calculateMWealthReductionTime();
		return Math.max(careTime,0);

	}

	private double calculateSupportReductionTime(){
		Iterable<Node> allNeigh = network.getAdjacent(this);
		double sumT = 0;

		for(Node n : allNeigh){
			RepastEdge w = network.getEdge(n,this);
			if(w!=null)sumT = sumT + w.getWeight(); 
		}

		sumT = 1 - (sumT/(Params.alpha+sumT));

		//return sumT;
		return 0;
	}

	private double calculateMWealthReductionTime(){
		//return 1-mWealth;
		return 0;
	}

	private double calculateDesiredFertility(){

		double desiredF = 0, support = 0;
		int count = 0;

			//support (local)

			//normative behaviour (local)
			for(Object n : network.getAdjacent(this)){
				desiredF = desiredF + ((Node)n).getDesired_Fertility();
				count++;
			}

			if(count>0){
				desiredF = ((desiredF / (double)count)+Math.random()*(Params.noise))*(freeTime);
			} else {
				desiredF = (Math.random()*Params.noise)*(freeTime);
			}
			
			//if(desiredF>Params.maxAgeRepro)desiredF=0;


		return desiredF;
	}

	private double calculateAgeFirstBirth(){

		int ageF = 0, count = 0;

		//parents age of first birth 


		//normative aspect
		//normative behaviour (local)
		for(Object n : network.getAdjacent(this)){
			ageF = ageF + ((Node)n).getAge_FirstBirth();
		}
		ageF=(int)(Math.round((double)ageF/(double)count));

		//return ageF;

		return 18;
	}

	/*	
	private void updateDesiredAge(){

		double average_DAge = 0;


		for(Edge e:inEdges){
			average_DAge= average_DAge + e.getStartNode().desired_age;	
		}

		if(inEdges.size()==0){
			//don't update	
		}else{
			if(noise==null){
				updated_DAGE=(average_DAge/inEdges.size());
			}else {
				updated_DAGE=(average_DAge/inEdges.size())+noise.sample();
			}
		}
	}

	 */


	/*****************************************get/set methods**************************************************/


	public Coordinate getCoord(){
		return coord;
	}
	public double getDesired_Fertility(){
		return desired_Fertility;
	}

	public int getAge_FirstBirth(){
		return age_FirstBirth;
	}

}
