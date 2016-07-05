package fertilityModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.java.plugin.Plugin;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.IAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.space.graph.ShortestPath;
import repast.simphony.util.ContextUtils;

import com.sun.media.sound.ModelDestination;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

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
	ArrayList<Node> kidList;
	List<Edge> myEdges;
	List<Node> myNeighs;
	List<Edge> outEdges;
	List<Edge> inEdges;
	ArrayList<Node> outNodes;
	int dead ;
	GeometryFactory fac = new GeometryFactory();
	Geography myGeog;



	/***********************************************************Constructors ****************************************************************/

	//Initial population
	public Node(Context cont,ContinuousSpace <Object > space, Coordinate coordinate,Network net, Geography geog) {
		context =cont;
		dead=0;

		children = 0;
		timeSinceLastBirth=0;
		network=net;
		age=0;//RandomHelper.nextIntFromTo(0, Params.maxAge);
		desired_Fertility = RandomHelper.nextDouble()*5;
		desired_ageFirstBirth = RandomHelper.nextDoubleFromTo(16, 35); 
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
		kidList = new ArrayList<Node>();
		myEdges = null;
		myNeighs= null;
		myGeog = geog;
		outNodes = new ArrayList<Node>();

		context.add(this);
		//Point geom = fac.createPoint(this.getCoord());
		//myGeog.move(this,geom);
	}

	//population born during simulation
	public Node(Context cont,ContinuousSpace <Object > space, Coordinate coordinate,Network net,Node motherNode, Geography geog) {
		context = cont;
		children = 0;
		timeSinceLastBirth=0;
		network=net;
		age=0;
		//desired_Fertility = motherNode.getDesired_Fertility();
		desired_ageFirstBirth = motherNode.getAge_FirstBirth(); 
		desired_ageFirstBirth = Math.min(Math.max(motherNode.getAge_FirstBirth()+(RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift_ageFirstBirth),Params.minAgeBirth),Params.maxAgeRepro);
		coord = coordinate;
		this.space=space;

		socialTime=0;
		freeTime=1;
		workTime=0;
		careTime=0;
		workLifeBalance=Math.min(Math.max(motherNode.workLifeBalance+(RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift_work),0),1);
		mWealth=0;
		workToWealthEfficiency = Params.workToWealthEfficiency;

		kinList = new ArrayList<Node>();
		kinList.add(motherNode);
		kidList = new ArrayList<Node>();
		myEdges = null;
		myNeighs= null;
		myGeog = geog;
		outNodes = new ArrayList<Node>();

		context.add(this);
		//Point geom = fac.createPoint(this.getCoord());
		//myGeog.move(this,geom);
	}

	/*
	public Node (boolean remove){
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters parms = ScheduleParameters.createOneTime(0,ScheduleParameters.FIRST_PRIORITY);

		schedule.schedule(parms, new IAction(){
			@Override
			public void execute() {
				Context context = ContextUtils.getContext(Node.this);
				context.remove(Node.this);
			}
		});
	}
	 */


	/***********************************************************each step ****************************************************************/

	public void step(){

		//System.out.println(this.toString()+" "+age);
		if(this.toString().contains("fertilityModel.Node@30f5002e")){
			System.out.println("age checker "+age);
		}

		//if(workLifeBalance<0.1){
		//System.out.println("bewha checker "+age);
		//}
		//System.out.println("age checker "+this.toString()+"  age = "+age + "  "+ this.workLifeBalance);   

		age++;
		timeSinceLastBirth++;

		//update my surroundings
		myNeighs = IteratorUtils.toList(network.getAdjacent(this).iterator());
		myEdges = IteratorUtils.toList(network.getEdges(this).iterator()); //can remove this ...?
		inEdges = IteratorUtils.toList(network.getInEdges(this).iterator());
		outEdges = IteratorUtils.toList(network.getOutEdges(this).iterator());

		//allocate my time
		double childCareT = allocateTime();

		//Update desired Age of First Birth and desired Fertility
		//if(age>=Params.ageSocial){
			//if(numbChild==0)desired_ageFirstBirth = calculateAgeFirstBirth(desired_ageFirstBirth);
			//desired_Fertility = calculateDesiredFertility(desired_Fertility, this.careTime); 
			//this.workLifeBalance = calculateWorkLifeBalance(this.workLifeBalance);
		//}


		//make fertility choices
		if(this.age>=desired_ageFirstBirth && this.age<Params.maxAgeRepro){

			////Is a new child born? (deterministic right now...)
			if(timeSinceLastBirth>Params.minInterBirthPeriod ){ //desired_Fertility>numbChild+1 &&

				if(checkTimeAvailability() ){
					numbChild++;
					if(numbChild == 1)this.age_FirstBirth=this.age;
					Node newNode = new Node(context, space,this.coord,network,this,myGeog);
					newNode.init(this.coord);
					ModelSetup.allNodes.add(newNode);
					timeSinceLastBirth=0;
					kidList.add(newNode);
					//System.out.println("birth occured : "+desired_Fertility + "  "+ newNode.toString());

					//else adjust until it is possible to meet desired fertility
				} //else{
					//this.workLifeBalance= Math.min(1,Math.max(this.workLifeBalance + RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift_work,0));
					//this.desired_Fertility=this.desired_Fertility-Params.desiredFertilityDecrease; 
				//}
			}

			////random chance of a birth
			if(RandomHelper.nextDouble()<Params.backgroundFertility && timeSinceLastBirth>=Params.minInterBirthPeriod){ 
				numbChild++;
				Node newNode = new Node(context, space,this.coord,network,this,myGeog);
				newNode.init(this.coord);
				ModelSetup.allNodes.add(newNode);
				timeSinceLastBirth=0;
				kidList.add(newNode);
				//System.out.println("chance birth occurred : "+desired_Fertility);

				if(numbChild == 1)this.age_FirstBirth=this.age;
			}
		}

		//decay of social ties
		if(age>Params.ageSocial)trimSocialTies();

		//finalize node for next step
		if(age>Params.maxAge || Math.random()<Params.probDeath){
			dead=1;
			myEdges = IteratorUtils.toList(network.getEdges(this).iterator());
		}
	}

	/*****************************************methods**************************************************/



	private double allocateTime(){

		//only social time
		if(this.age>=Params.ageSocial && this.age<Params.ageWork){

			this.careTime = calculateCareTime();
			this.freeTime = Math.max(1 - this.careTime,0);
			this.socialTime = this.freeTime;
			this.workTime = 0;

			generateSupport(this.socialTime);

			//social and work time
		} else if (this.age>=Params.ageWork){

			this.careTime = calculateCareTime();
			this.freeTime = Math.max(1 - this.careTime,0);
			this.socialTime = this.freeTime * this.workLifeBalance;
			this.workTime = this.freeTime * (1-this.workLifeBalance);

			if(this.socialTime>Params.socialDecrease)generateSupport(this.socialTime);
			generateMWealth(this.workTime);

		}

		return careTime;

	}

	private boolean checkTimeAvailability(){
		if(calculateCareTimePlusONE()<0.5)return true;
		return false;
	}

	private void generateSupport(double sTime){

		//Divide social time to those in my social network
		double timeA = sTime;

		if(outEdges.size()>Params.maxSocialTies+1){
			//System.out.println("some thing is up: age " + this.age);
		}

		while (timeA > 0){

			//calculate probability of forming new ties or strengthening old ones
			double probTie = 1.0/(1.0+outEdges.size());
			//if(outEdges.size()>Params.maxSocialTies)probTie=0;

			//choose random proportion of total time
			double choosenTimeA=0;
			if(timeA>Params.minEdgeWeight+Params.timeResolution){
				choosenTimeA = RandomHelper.nextDoubleFromTo(Params.socialDecrease, timeA);	
			} else {
				choosenTimeA = timeA;
			}

			//assign the proportion of time to a social tie
			if(RandomHelper.nextDouble()>=probTie ){
				//agent chooses to strengthen an existing tie (or reciprocate one)
				reinforceExistingTie(choosenTimeA);

			} else {
				//reciprocate ties (if none present create a random tie)
				reciprocateATie(choosenTimeA);
			}

			timeA = timeA - choosenTimeA;

		}
	}

	private void reinforceExistingTie(double choosenTimeA){
		if (outEdges.size()>0){
			Collections.shuffle(outEdges);
			Edge target = (Edge) outEdges.get(0);
			target.setWeight(target.getWeight()+choosenTimeA);
		} else {
			reciprocateATie(choosenTimeA);
		}
	}

	private void reciprocateATie(double choosenTimeA){

		boolean recipTie = false;
		for(Object in : inEdges){
			boolean alreadyThere = false;
			for(Object out: outEdges){
				if( ((Edge)out).getTarget()==((Edge)in).getSource()  )alreadyThere=true;
			}

			if(alreadyThere = false && outEdges.size()>0){
				Edge re = new Edge(this, ((Node)((Edge)in).getSource()),true, choosenTimeA);
				context.add(re);
				network.addEdge(re);
				outEdges.add(re);
				recipTie=true;
				break;	
			}
		}

		//if no recipt possible, create a new random tie
		if (recipTie == false)createNewRandomTie(choosenTimeA);

	}

	private void createNewRandomTie(double choosenTimeA){
		//agent chooses to create a new connection

		//1.list all agents within specified social distance (Params.sdMax)
		ArrayList<Node> nearMe = new ArrayList<Node>();
		Envelope pointEnv = new Envelope();
		pointEnv.init(this.getCoord());
		pointEnv.expandBy(Params.maxSocialDistance);
		java.util.List<Node> nodesNearMe = ModelSetup.getQuadtree().query(pointEnv);
		for(Node n : nodesNearMe){
			if(n.getCoord().distance(this.getCoord())<Params.maxSocialDistance && n!=this){
				if(myNeighs.contains(n)==false){
					nearMe.add(n);	
				}

			}
		}

		Collections.shuffle(nearMe);
		Node indSelected = null; 
		for(int i = 0; i<nearMe.size();i++){
			Node potentialN = nearMe.get(i);
			if( Math.abs(potentialN.age-this.age)<Params.ageDiffMax && outNodes.contains(potentialN)==false){
				indSelected = potentialN;
				break;
			}
		}

		//2. allocate social time to this neighbour
		if(indSelected!=null){
			Edge re = new Edge(this, indSelected,true, choosenTimeA);
			context.add(re);
			network.addEdge(re);
			outEdges.add(re);
			//Coordinate[] coords = { ((Node)re.getSource()).getCoord(),((Node)re.getTarget()).getCoord()};
			//LineString line = fac.createLineString(coords);
			//myGeog.move(re,line);
			//} else if (myEdges.size()>0){
			//	Collections.shuffle(myEdges);
			//	myEdges.get(0).setWeight(myEdges.get(0).getWeight()+choosenTimeA); 
		} else {
			//nothing no suitable neigh... time lost
		}
	}

	private void generateMWealth (double wTime){
		this.mWealth = this.workTime * this.workToWealthEfficiency ;
	}

	private double density(Coordinate c){

		//1.list all agents within specified social distance (Params.sdMax)
		ArrayList<Node> nearMe = new ArrayList<Node>();
		Envelope pointEnv = new Envelope();
		pointEnv.init(c);
		pointEnv.expandBy(Params.maxSocialDistance);
		java.util.List<Node> nodesNearMe = ModelSetup.getQuadtree().query(pointEnv);

		for(Node n : nodesNearMe){
			if(n.getCoord().distance(c)<Params.maxSocialDistance && n!=this){
				nearMe.add(n);	
			}
		}

		return nearMe.size();

	}

	private void trimSocialTies(){

		ArrayList<RepastEdge> toRemove = new ArrayList<RepastEdge>();

		for(RepastEdge ee : myEdges){
			if(ee.getSource()==this){

				ee.setWeight(ee.getWeight()-Params.socialDecrease);

				if(ee.getWeight()<Params.minEdgeWeight){
					network.removeEdge(ee);
				}
			}
		}
	}

	private double calculateCareTime(){

		//int numbOfSmallChildren = 0;
		//for(Node nn : this.kidList){
		//	if(nn.getAge()<Params.ageWork)numbOfSmallChildren ++;
		//}
		//double careTime = numbOfSmallChildren * (Params.timePerChild) - calculateSupportReductionTime() - calculateMWealthReductionTime();
		double careTime = this.numbChild * (Params.timePerChild) - calculateSupportReductionTime() - calculateMWealthReductionTime();
		return Math.max(careTime,0);

	}

	private double calculateCareTimePlusONE(){

		//int numbOfSmallChildren = 0;
		//for(Node nn : this.kidList){
		//	if(nn.getAge()<Params.ageWork)numbOfSmallChildren ++;
		//}

		//double careTime = (numbOfSmallChildren+1) * (Params.timePerChild) - calculateSupportReductionTime() - calculateMWealthReductionTime();
		double careTime = (this.numbChild+1) * (Params.timePerChild) - calculateSupportReductionTime() - calculateMWealthReductionTime();
		return Math.max(careTime,0);

	}

	private double calculateSupportReductionTime(){

		double sumT = 0;
		int count = inEdges.size();

		for(Edge ee : inEdges){
			sumT=sumT + ee.getWeight(); //in edges only
		}

		//sumT = (sumT/(Params.alpha+sumT));

		return sumT*Params.alpha;
		//return count*Params.alpha;
		//return 0;
	}

	private double calculateMWealthReductionTime(){
		return mWealth;
		//return 0;
	}

	private double calculateDesiredFertility(double dFert, double childCareT){

		double desiredF = dFert, support = 0;
		int count = 0;

		//normative behaviour (local)
		double localD = 0;
		for(Object n : myNeighs){
			localD = localD + ((Node)n).getDesired_Fertility();
			count++;
		}

		//drift
		double drift = (RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift);

		if(count>0){
			localD = ((localD / (double)count));
			desiredF = Math.max(desiredF + Params.conformity*(localD-desiredF) + drift,0);
		} else {
			desiredF = desiredF + drift;
		}

		//if(desiredF>Params.maxAgeRepro)desiredF=0;
		//if(childCareT>Params.crisisT)desiredF=0;

		return desiredF;
	}

	private double calculateAgeFirstBirth(double ageF_birth){

		double ageF = ageF_birth, count = 0;

		//normative aspect
		double ageF_norm = 0;
		for(Object n : myNeighs){
			ageF_norm = ageF_norm + ((Node)n).getDesiredAge_FirstBirth();
			count++;
		}
		ageF_norm = ageF_norm / (double)count;

		//drift aspect
		double drift = (RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift);

		//final desired age of first birth
		if(count>0){
			ageF= Math.max(ageF + Params.conformity * (ageF_norm - ageF) + drift,Params.minAgeBirth);
		} else {
			ageF= Math.max(ageF + drift,Params.minAgeBirth);
		}

		return ageF;
	}

	private double calculateWorkLifeBalance(double ws){

		double ws_norm = 0;
		int count=0;
		//normative aspect
		for(Object n : myNeighs){
			ws_norm = ws_norm + ((Node)n).getWorkLifeBalance();
			count++;
		}
		ws_norm = ws_norm / (double)count;
		
		//drift aspect
		double drift = (RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift);

		//final desired age of first birth
		if(count>0){
			ws= Math.min(ws + Params.conformity * (ws_norm - ws) + drift,1);
		} else {
			ws= Math.min(ws + drift,1);
		}

		return Math.max(ws,0);

	}


	/*******************************************dispersal methods **********************************************/

	public void init(Coordinate mCoord){
		boolean retval =true;
		int count=0;
		Coordinate coordNew = chooseLocation(Params.maxSocialDistance);
		//Coordinate coordNew = chooseRandomLocation(mCoord);
		if(coordNew!=null){
			space.moveTo(this, coordNew.x,coordNew.y);
			this.coord = coordNew;
		} else {
			//System.out.println("something is wrong with the dispersal code!");
		}
	}
	private Coordinate chooseRandomLocation(Coordinate m){
		double angle = Math.random()*Math.PI*2;
		double rDistance = Math.random()*Params.maxSocialDistance;
		Coordinate testCoord = new Coordinate(m.x+Math.cos(angle)*rDistance,m.y+Math.sin(angle)*rDistance);

		while( (testCoord.x>0 && testCoord.y>0 && testCoord.x<Params.landscapeSize && testCoord.y<Params.landscapeSize) == false){
			angle = Math.random()*Math.PI*2;
			rDistance = Math.random()*Params.maxSocialDistance;
			testCoord = new Coordinate(m.x+Math.cos(angle)*rDistance,m.y+Math.sin(angle)*rDistance);
		}

		return testCoord;
	}
	private Coordinate chooseLocation(double dist){

		Coordinate startCoord = this.kinList.get(0).coord;
		boolean retval=true;
		Coordinate finalCoord = null;
		int count = 0;

		//if there is no room in the simulation the agent is not born (density effect?) 
		if(dist>Params.landscapeSize){
			this.dead=1;
			retval=false;
		}

		while(retval){
			double angle = Math.random()*Math.PI*2;
			double rDistance = Math.random()*dist;
			Coordinate testCoord = new Coordinate(startCoord.x+Math.cos(angle)*rDistance,startCoord.y+Math.sin(angle)*rDistance);
			//System.out.println("dist "+ testCoord.distance(startCoord));

			if( (testCoord.x<Params.landscapeSize && testCoord.y<Params.landscapeSize && testCoord.x>0 && testCoord.y>0)== true && freeSpace(testCoord)==true){
				finalCoord = testCoord;
				break;
			}

			count++;
			if(count>10){
				finalCoord = chooseLocation(dist*1.1);
				break;
				//System.out.println("couldn't generate new location from mother ... "+ coord.toString());
			}
		}
		return finalCoord;
	}

	private boolean freeSpace(Coordinate c){

		boolean free= true;

		//1.list all agents within specified social distance (Params.sdMax)
		ArrayList<Node> nearMe = new ArrayList<Node>();
		Envelope pointEnv = new Envelope();
		pointEnv.init(c);
		pointEnv.expandBy(Params.maxSocialDistance);
		java.util.List<Node> nodesNearMe = ModelSetup.getQuadtree().query(pointEnv);

		for(Node n : nodesNearMe){
			if(n.getCoord().distance(c)<Params.maxSocialDistance && n!=this){
				nearMe.add(n);	
			}
		}

		//System.out.println("near me : "+nearMe.size());
		if(nearMe.size()>Params.maxDensity)free=false;

		return free;

	}



	/*****************************************get/set methods**************************************************/


	public Coordinate getCoord(){
		return coord;
	}
	public double getDesired_Fertility(){
		return desired_Fertility;
	}
	public double getNumberChildren(){
		return numbChild;
	}
	public int getAge_FirstBirth(){
		return age_FirstBirth;
	}
	private List<Edge> getMyEdgeList(){
		return myEdges;
	}
	public double getDesiredAge_FirstBirth(){
		return this.desired_ageFirstBirth;
	}
	public double getAge(){
		return age;
	}
	public double getWorkLifeBalance(){
		return workLifeBalance;
	}

}



/* old code
private int socialDistance(Node target){

int sdist = 0;

//look for first neigh
for (RepastEdge e : myEdges){
	if(e.getTarget()==target){
		sdist=1;
		break;
	}
}


//if not found (i.e., sdist == 0) then look at second neigh
if (sdist == 0){
	for (RepastEdge e : myEdges){
		for(RepastEdge e2 : ((Node)(e.getTarget())).myEdges){
			if(e2.getTarget()==target){
				sdist=2;
				break;
			}

		}
	}	
}

return sdist;
}

private int socialDistanceRec(Node target ,Node source, int s,int c){

int sdist= s, count = c++;
//only go 2 deep
if(c<3){
	for(RepastEdge e:source.myEdges){

		if(e.getTarget()==target){
			sdist = count;

		} else {
			//count++;
			//count = socialDistance(target, (Node) e.getTarget(), sdist,count);
		}
	} 
}
return sdist;

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
}*/
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