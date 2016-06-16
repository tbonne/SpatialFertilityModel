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
	List<RepastEdge> myEdges;
	List<Node> myNeighs;
	int dead ;


	/***********************************************************Constructors ****************************************************************/

	//Initial population
	public Node(Context cont,ContinuousSpace <Object > space, Coordinate coordinate,Network net) {
		context =cont;
		dead=0;

		children = 0;
		timeSinceLastBirth=0;
		network=net;
		age=0;
		desired_Fertility = RandomHelper.nextDouble()*3;
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
		myEdges = null;
		myNeighs= null;
	}

	//population born during simulation
	public Node(Context cont,ContinuousSpace <Object > space, Coordinate coordinate,Network net,Node motherNode) {
		context = cont;
		children = 0;
		timeSinceLastBirth=0;
		network=net;
		age=0;
		desired_Fertility = motherNode.getDesired_Fertility();
		desired_ageFirstBirth = motherNode.getAge_FirstBirth(); 
		coord = coordinate;
		this.space=space;

		socialTime=0;
		freeTime=1;
		workTime=0;
		careTime=0;
		workLifeBalance=Math.min(Math.max(motherNode.workLifeBalance+RandomHelper.nextDoubleFromTo(-Params.workLifeBalanceVar, Params.workLifeBalanceVar),0),1);
		mWealth=0;
		workToWealthEfficiency = Params.workToWealthEfficiency;

		kinList = new ArrayList<Node>();
		kinList.add(motherNode);
		myEdges = null;
		myNeighs= null;
	}


	/***********************************************************each step ****************************************************************/

	public void step(){

		//System.out.println(this.toString()+" "+age);
		if(this.toString().contains("fertilityModel.Node@5513aff5")){
			System.out.println("age checker "+age);
		}

		age++;
		timeSinceLastBirth++;

		//update my surroundings
		myEdges = IteratorUtils.toList(network.getEdges(this).iterator());
		myNeighs = IteratorUtils.toList(network.getAdjacent(this).iterator());

		//allocate my time
		allocateTime();

		//Desired Age of First Birth
		if(numbChild==0 && age>=Params.ageSocial)desired_ageFirstBirth = calculateAgeFirstBirth(desired_ageFirstBirth);

		//make fertility choices
		if(this.age>=desired_ageFirstBirth){

			//Desired fertility
			desired_Fertility = calculateDesiredFertility(desired_Fertility, this.careTime); 

			////Is a new child born? (deterministic right now...)
			if(desired_Fertility>numbChild+1 && timeSinceLastBirth>=Params.minInterBirthPeriod){
				numbChild++;
				if(numbChild == 1)this.age_FirstBirth=this.age;
				Node newNode = new Node(context, space,this.coord,network,this);
				context.add(newNode);
				newNode.init();
				ModelSetup.allNodes.add(newNode);
				timeSinceLastBirth=0;
				System.out.println("birth occured : "+desired_Fertility + "  "+ newNode.toString());

				
			}

			////random chance of a birth
			if(RandomHelper.nextDouble()<Params.backgroundFertility && timeSinceLastBirth>=Params.minInterBirthPeriod){
				numbChild++;
				Node newNode = new Node(context, space,this.coord,network,this);
				context.add(newNode);
				newNode.init();
				ModelSetup.allNodes.add(newNode);
				timeSinceLastBirth=0;
				//System.out.println("chance birth occurred : "+desired_Fertility);

				if(numbChild == 1)this.age_FirstBirth=this.age;
			}
		}

		//finalize node for next step
		if(age>Params.maxAge)dead=1;
		if(age>Params.ageSocial)trimSocialTies();

	}

	/*****************************************methods**************************************************/



	private void allocateTime(){

		//only social time
		if(this.age>Params.ageSocial && this.age<Params.ageWork){

			this.socialTime = 1;
			this.workTime = 0;
			this.careTime = 0;
			this.freeTime = 1;

			generateSupport(this.socialTime);

			//social and work time
		} else if (this.age>=Params.ageWork){

			this.careTime = calculateCareTime();
			this.freeTime = 1 - this.careTime;
			this.socialTime = this.freeTime * this.workLifeBalance;
			this.workTime = this.freeTime * (1-this.workLifeBalance);

			generateSupport(this.socialTime);
			generateMWealth(this.workTime);

		}

		//if in crisis
		if(this.careTime>Params.crisisT){
			this.workLifeBalance= this.workLifeBalance + RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift_work;
		}

	}

	private void generateSupport(double sTime){

		//Divide social time to those in my social network
		double timeA = sTime;

		//how many edges do am i initiating?
		int edgeSize = 0;
		for(RepastEdge ee : this.getMyEdgeList()){
			if(ee.getSource()==this)edgeSize++;
		}


		while (timeA > 0){

			double probTie = 1.0/(1.0+edgeSize);
			int ages = this.age;

			double choosenTimeA=0;
			if(timeA>Params.minEdgeWeight+Params.timeResolution){
				choosenTimeA = RandomHelper.nextDoubleFromTo(Params.minEdgeWeight, timeA);	
			} else {
				choosenTimeA = timeA;
			}


			if(RandomHelper.nextDouble()>=probTie ){
				//agent chooses to strengthen an existing time

				if (myEdges.size()>0){
					Collections.shuffle(myEdges);
					myEdges.get(0).setWeight(myEdges.get(0).getWeight()+choosenTimeA);
				}


			} else {
				//agent chooses to create a new connection
				edgeSize++;

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
					if( Math.abs(potentialN.age-this.age)<Params.ageDiffMax){
						indSelected = potentialN;
						break;
					}
				}

				//2. allocate random amount of social time to this neighbour
				if(indSelected!=null){
					network.addEdge(this, indSelected, choosenTimeA);
				} else if (myEdges.size()>0){
					Collections.shuffle(myEdges);
					myEdges.get(0).setWeight(myEdges.get(0).getWeight()+choosenTimeA);
				} else {
					//nothing no suitable neigh...
				}
			}

			timeA = timeA - choosenTimeA;

		}

	}



	private void generateMWealth (double wTime){
		this.mWealth = this.workTime * this.workToWealthEfficiency;
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

		double careTime = this.numbChild * (Params.timePerChild) - calculateSupportReductionTime() - calculateMWealthReductionTime();
		return Math.max(careTime,0);

	}

	private double calculateSupportReductionTime(){

		double sumT = 0;

		for(Node n : myNeighs){
			RepastEdge w = network.getEdge(n,this);
			if(w!=null)sumT = sumT + w.getWeight(); 
		}

		sumT = 1 - (sumT/(Params.alpha+sumT));

		return sumT;
		//return 0;
	}

	private double calculateMWealthReductionTime(){
		return mWealth;
		//return 0;
	}

	private double calculateDesiredFertility(double dFert, double childCareT){

		double desiredF = dFert, support = 0;
		int count = 0;

		//support (local)

		//normative behaviour (local)
		double localD = 0;
		for(Object n : myNeighs){
			localD = localD + ((Node)n).getDesired_Fertility();
			count++;
		}

		//drift
		double drift = (RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift_fertility);

		if(count>0){
			localD = ((localD / (double)count));
			desiredF = Math.max(desiredF + Params.conformity*(localD-desiredF) + drift,0);
		} else {
			desiredF = desiredF + drift;
		}


		if(desiredF>Params.maxAgeRepro)desiredF=0;
		if(childCareT>Params.crisisT)desiredF=0;


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
		double drift = (RandomHelper.nextDoubleFromTo(-1, 1)*Params.rDrift_ageOfFirstBirth);

		//final desired age of first birth
		if(count>0){
			ageF= Math.max(ageF + Params.conformity * (ageF_norm - ageF) + drift,13);
		} else {
			ageF= Math.max(ageF + drift,13);
		}
		
		return ageF;
	}


	/*******************************************dispersal methods **********************************************/

	public void init(){
		boolean retval =true;
		int count=0;
		Coordinate coordNew = null;
		while(retval){
			coordNew = new Coordinate(coord.x+RandomHelper.nextDoubleFromTo(-Params.dispersalRadius,Params.dispersalRadius),coord.y+RandomHelper.nextDoubleFromTo(-Params.dispersalRadius,Params.dispersalRadius));

			if(coordNew.x<Params.landscapeSize && coordNew.y<Params.landscapeSize && coordNew.x>0 && coordNew.y>0){
				this.coord = coordNew;
				break;
			}

			count++;
			if(count>10){
				this.coord = this.kinList.get(0).coord;
				System.out.println("couldn't generate new location from mother ... "+ coord.toString());
			}
		}
		space.moveTo(this, coord.x,coord.y);
	}



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
	private List<RepastEdge> getMyEdgeList(){
		return myEdges;
	}
	public double getDesiredAge_FirstBirth(){
		return this.desired_ageFirstBirth;
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