package fertilityModel;

import java.util.ArrayList;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.java.plugin.Plugin;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;

import com.sun.media.sound.ModelDestination;
import com.vividsolutions.jts.geom.Coordinate;

public class Node {  

	ArrayList<Edge> inEdges;
	ArrayList<Edge> outEdges;
	int id;
	Coordinate coord;
	double desired_age,updated_DAGE;
	private ContinuousSpace <Object > space; 
	private NormalDistribution noise =null;


	public Node(ContinuousSpace <Object > space,int i, Coordinate coordinate) {
		inEdges = new ArrayList<Edge>();
		outEdges = new ArrayList<Edge>();
		id = i;
		desired_age = RandomHelper.nextDouble()*30+15;
		updated_DAGE = RandomHelper.nextDouble()*30+15;
		coord = coordinate;
		this.space=space;
		if(Params.sterr_noise>0)noise = new NormalDistribution(0,Params.sterr_noise);
	}

	public void nodeInitialize(){

		//set learning matrix

	}

	public void step(){

		//
		updateDesiredAge();


		//


	}

	public void updateDAges(){
		desired_age=Math.max(15,Math.min(45,updated_DAGE));
	}



	/*****************************************methods**************************************************/


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




	/*****************************************get/set methods**************************************************/

	public int getID(){
		return id;
	}
	public ArrayList<Edge> getBackEdges(){
		return inEdges;
	}

	public Coordinate getCoord(){
		return coord;
	}
	public ArrayList<Edge> getOutEdges(){
		return outEdges;
	}
	public ArrayList<Edge> getInEdges(){
		return inEdges;
	}
	public double getDesired_age(){
		return desired_age;
	}
	public void setDesired_age(double d){
		desired_age=d;
	}

}
