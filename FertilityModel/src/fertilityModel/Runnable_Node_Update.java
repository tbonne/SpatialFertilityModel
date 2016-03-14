package fertilityModel;

public class Runnable_Node_Update implements Runnable {
	
Node node;
	
	Runnable_Node_Update(Node n){
		node = n;
	}
	
	@Override
	public void run(){
		Throwable thrown = null;
	    try {
		node.step();
	    } catch (Throwable e) {
	        thrown = e;
	        System.out.println("Problem lies in node step code" + thrown);
	    } finally {
	    	return;
	    }
	}

}
