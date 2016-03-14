package fertilityModel;

public class Runnable_Edge implements Runnable {
	
Edge edge;
	
	Runnable_Edge(Edge e){
		edge = e;
	}
	
	@Override
	public void run(){
		Throwable thrown = null;
	    try {
	//	edge.step();
	    } catch (Throwable e) {
	        thrown = e;
	        System.out.println("Problem lies in edge step code" + thrown);
	    } finally {
	    	return;
	    }
	}

}
