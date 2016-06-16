package fertilityModel;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.IAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;

public class Edge extends RepastEdge{
	Node start, end;
	int ID,pulse;
	double length=0;
	
	
	public Edge(Node nodeStart, Node node, boolean b, double i) {
		this.target=node;
		this.source=nodeStart;
		this.directed=b;
		this.setWeight(i);
	
	}
	
	public Edge (boolean remove){
		ISchedule schedule = RunEnvironment.getInstance().getCurrentSchedule();
		ScheduleParameters parms = ScheduleParameters.createOneTime(0,ScheduleParameters.FIRST_PRIORITY);
		
		schedule.schedule(parms, new IAction(){
			@Override
			public void execute() {
				Context context = ContextUtils.getContext(Edge.this);
				context.remove(Edge.this);
			}
		});
	}
	
	
	public Node getStartNode(){
		return start;
	}
	public Node getEndNode(){
		return end;
	}
	public double getLength(){
		return length;
	}
}
