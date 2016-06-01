package fertilityModel;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;

public class Edge extends RepastEdge{
	double weight;
	Node start, end;
	int ID,pulse;
	double length=0;
	
	
	public Edge(Node s, Node e,boolean directed,double weight){
		
		super(s, e, directed, weight);
		
		start = s;
		end = e;
		length=s.getCoord().distance(e.getCoord());
		pulse=0;
	}
	
	public void setWeight(double d){
		weight=d;
	}
	public Node getStartNode(){
		return start;
	}
	public double getWeight(){
		return weight;
	}
	public Node getEndNode(){
		return end;
	}
	public double getLength(){
		return length;
	}
	public int getPulse(){
		return pulse;
	}
	public void setPulse(int i ){
		pulse = i;
	}
}
