package fertilityModel;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/*
 * 
 * This class controls the multithreading processes, breaking the model up into 3 seperate multithreaded sequences:
 * 			1) Nodes
 * 			2) Edges
 * 			3) Pulses
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
		for (Node n:ModelSetup.allNodes){
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
	}
	
	
	public static void updateNodes(){

		
		Collection<Callable<Void>> tasks_inputs = new ArrayList<Callable<Void>>();
		for (Node n:ModelSetup.allNodes){
			Runnable worker = new Runnable_Node_Update(n);
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
	}
	
	
	public void shutdown(){
		executor.shutdown();
	}
}
