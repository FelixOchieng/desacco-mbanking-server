package ke.skyworld.mbanking.ussdapi;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;

import java.lang.reflect.Method;

public class USSDAPIThread extends Thread {
	USSDRequest ussdRequest = null;
	String strMethodName = null;
	
	public USSDAPIThread(String theMethodName, USSDRequest theUSSDRequest){
		ussdRequest = theUSSDRequest;
		strMethodName = theMethodName;
		start();
	}
	
	public void run(){
		try{
			System.out.println("CALLING METHOD: " + strMethodName + "() ...");
			
			USSDAPI theUSSDAPI = new USSDAPI();
			
			Method methodMenus = USSDAPI.class.getDeclaredMethod(strMethodName, USSDRequest.class);
			boolean theResponse = (boolean) methodMenus.invoke(theUSSDAPI, ussdRequest);
			
			
			if(theResponse){
				System.out.println("Request Processed Successfully\n");
			}
			else{
				System.out.println("System got no response\n");
			}
		}
		catch (Exception e){
			System.err.println("Thread - " + this.getId() + " error: " + e.getMessage());
		}
		finally{
		}
		System.out.println("Thread - " + this.getId() +  " RUN is over" );
	}
}
