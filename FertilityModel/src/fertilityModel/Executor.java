package fertilityModel;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import repast.simphony.space.gis.DefaultGeography;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.index.quadtree.Quadtree;


/*
 * 
 * This class controls the multithreading processes, breaking the model up into 3 seperate multithreaded sequences:
 * 			1) Nodes
 * 			2) Edges
 */


public class Executor {

	private static final int pThreads = Params.numThreads;
	private static ExecutorService executor;

	public Executor(){
		executor = Executors.newFixedThreadPool(pThreads);
	}

	//synchronous scheduling
	public static void processNodes(){

		
		Collection<Callable<Void>> tasks_inputs = new ArrayList<Callable<Void>>();
		ArrayList<Node> allNodesIn = ModelSetup.allNodes;
		Collections.shuffle(allNodesIn);
		for (Node n:allNodesIn){
			Runnable worker = new Runnable_Node(n);
			tasks_inputs.add(Executors.callable(worker,(Void)null));
		}

		try {
			for (Future<?> f : executor.invokeAll(tasks_inputs)) { //invokeAll() blocks until ALL tasks submitted to executor complete
				f.get(); 
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}catch (NullPointerException e){
			e.printStackTrace();
		}
		/*
		ArrayList<Node> allNodesIn = ModelSetup.getAllNodes();
		Collections.shuffle(allNodesIn);
		for (Node n:allNodesIn){
			n.step();
		}*/
	}


	public static void updateNodes(){

		
		ArrayList<Node> toBeRemoved = new ArrayList<Node>();
		Network net = ModelSetup.getNetwork();
		
		

		for (Node n:ModelSetup.allNodes){
			if(n.dead==1)toBeRemoved.add(n);
		}

		
		for(Node n:toBeRemoved){
			
			for(Edge e : n.myEdges){
				ModelSetup.getContext().remove(e);
			}
			
			ModelSetup.allNodes.remove(n);
			ModelSetup.getContext().remove(n);
			
			/*Iterable<RepastEdge> it = net.getEdges(n);
			if(it!=null){
				if(it.iterator().hasNext()){
					for(RepastEdge re : it ){
						net.removeEdge(re);	
					}
				}
			}*/

		}
		
	//	System.gc();
		
		



	}

	public static void processSpace(){
		Quadtree spaceEnv = new Quadtree();
		for (Node n:ModelSetup.allNodes){
			Envelope pointenv = new Envelope();
			pointenv.init(n.getCoord());
			pointenv.expandBy(1);
			spaceEnv.insert(pointenv, n);
		}

		ModelSetup.setQuadtree(spaceEnv);

	}


	public void shutdown(){
		executor.shutdown();
	}
}
