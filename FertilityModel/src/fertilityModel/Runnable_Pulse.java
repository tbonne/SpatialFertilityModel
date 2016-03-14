package fertilityModel;

public class Runnable_Pulse implements Runnable {
	
Pulse pulse;
	
	Runnable_Pulse(Pulse p){
		pulse = p;
	}
	
	@Override
	public void run(){
		Throwable thrown = null;
	    try {
		//pulse.step();
	    } catch (Throwable e) {
	        thrown = e;
	        System.out.println("Problem lies in pulse step code" + thrown);
	    } finally {
	    	return;
	    }
	}
}
