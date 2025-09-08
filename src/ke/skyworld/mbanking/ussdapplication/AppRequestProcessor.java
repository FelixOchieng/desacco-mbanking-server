package ke.skyworld.mbanking.ussdapplication;
import ke.skyworld.lib.mbanking.ussd.USSDConstants;
import ke.skyworld.lib.mbanking.ussd.USSDLocalParameters;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import ke.skyworld.mbanking.ussdapplication.AppConstants;
//import ke.skyworld.mbanking.ussdapplication.AppActions;

public class AppRequestProcessor {
	public AppRequestProcessor(){
	}

	public USSDResponse processRequest(USSDRequest theUSSDRequest){

		USSDResponse theUSSDResponse = null;

		final String strUSSDDataType = theUSSDRequest.getUSSDDataType();
		AppConstants.USSDDataType theUSSDDataType = AppUtils.getUSSDDataTypeFromValue( strUSSDDataType );

		try{
			if(theUSSDRequest.getUSSDData().size() > 0){

				String strLastKey = (String) theUSSDRequest.getUSSDData().keySet().toArray()[theUSSDRequest.getUSSDData().size() -1];
				String strLastValue = (String) theUSSDRequest.getUSSDData().values().toArray()[theUSSDRequest.getUSSDData().size() -1];

				if(USSDLocalParameters.getSystemDebugLevel() == USSDConstants.SystemDebugLevel.LEVEL_1.getValue()){
					System.out.println("strLastKey : " + strLastKey);
					System.out.println("strLastValue : " + strLastValue);
				}

				switch (strLastKey) {
					case USSDConstants.strHOME:
					case USSDConstants.strLINK: {
						theUSSDDataType = AppUtils.getUSSDDataTypeFromName(strLastValue);
						if( theUSSDDataType.equals(AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_DATA_TYPE_NOT_FOUND) ){
							theUSSDDataType = AppConstants.USSDDataType.INIT;
						}
						break;
					}
					case USSDConstants.strBACK:{ //Go to One Step Back
						String backUSSDDataType = strUSSDDataType;
						if(backUSSDDataType.indexOf("-") > 0){
							backUSSDDataType = backUSSDDataType.substring(0, backUSSDDataType.lastIndexOf("-"));
							theUSSDDataType = AppUtils.getUSSDDataTypeFromValue( backUSSDDataType );
						}else{
							theUSSDDataType = AppConstants.USSDDataType.INIT;
						}
						break;
					}
					case USSDConstants.strINIT: { //Go to INIT Menu
						theUSSDDataType = AppConstants.USSDDataType.INIT;
						break;
					}
				}
			}

			theUSSDResponse = executeMethod(theUSSDRequest, theUSSDDataType);

		}
		catch(Exception e){
			System.err.println("AppRequestProcessor.processRequest() :" + e.getMessage());
		}
		finally{

		}

		return theUSSDResponse;

	} //End  processRequest

	public static USSDResponse executeMethod(USSDRequest theUSSDRequest, String theUSSDDataTypeName){
		USSDResponse theUSSDResponse = null;
		try{
			AppConstants.USSDDataType ussdDataType = AppUtils.getUSSDDataTypeFromName(theUSSDDataTypeName);
			theUSSDResponse = executeMethod(theUSSDRequest,ussdDataType);
		} catch(Exception e){
			System.err.println("AppRequestProcessor.executeMethod(USSDRequest, String) :" + e.getMessage());
		}

		return theUSSDResponse;
	}

	public static USSDResponse executeMethod(USSDRequest theUSSDRequest, AppConstants.USSDDataType theUSSDDataType){
		USSDResponse theUSSDResponse = null;
		String theMethodName = AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_ERROR.getValue();
		String theMethodParam= "UNKNOWN_ERROR";

		try{
			String strMethodWithParam = null;

			if(theUSSDDataType != AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_DATA_TYPE_NOT_FOUND){

				String[] arrayUSSDDataTypeMethods = theUSSDDataType.getValue().split("-");
				System.out.println("Methods Found : " + arrayUSSDDataTypeMethods.length);

				int intCurrentIndex = arrayUSSDDataTypeMethods.length - 1;
				strMethodWithParam =arrayUSSDDataTypeMethods[intCurrentIndex];

			}else{
				strMethodWithParam = AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_DATA_TYPE_NOT_FOUND.getValue();
			}

			System.out.println("Method with Param : " + strMethodWithParam);

			//GET METHOD NAME
			Pattern patternMethod = Pattern.compile("(.+?)\\(.*");
			Matcher matcherMethod = patternMethod.matcher(strMethodWithParam);
			if (matcherMethod.matches()) {
				theMethodName = matcherMethod.group(1);
				System.out.println("METHOD NAME: " + matcherMethod.group(1));
				//GET METHOD PARAM
				Pattern patternParam = Pattern.compile("^.*\\((.*)\\)$");
				Matcher matcherParam = patternParam.matcher(strMethodWithParam);
				if (matcherParam.matches()) {
					theMethodParam = matcherParam.group(1);
					System.out.println("METHOD PARAM: " + matcherParam.group(1));
				}else{
					theMethodName = AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_ERROR.getValue();
					theMethodParam = "ERROR";
					throw new Exception("METHOD PARAM NOT FOUND");
				}
			}else{
				theMethodName = AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_ERROR.getValue();
				theMethodParam = "ERROR";
				throw new Exception("METHOD NAME NOT FOUND");
			}
		} catch(Exception e){
			System.err.println("ERROR: METHOD and/or PARAM Could not be retrieved from strUSSDDataType\nSystem Error Message: " + e.getMessage());
		}
		finally{
			try{

				String strMethodName = USSDConstants.strUSSDMenuMethodPrefix + theMethodName;
				String strMethodParam = theMethodParam;


				System.out.println("CALLING METHOD: " + strMethodName + "(" + strMethodParam +") ...");

				AppActions theUSSDActions = new AppActions(theUSSDRequest.getUSSDMobileNo());

				Method methodMenus = AppActions.class.getDeclaredMethod(strMethodName, USSDRequest.class, String.class);
				theUSSDResponse = (USSDResponse) methodMenus.invoke(theUSSDActions, theUSSDRequest, strMethodParam);
			}
			catch(Exception e){
				System.err.println("CALLING METHOD FAILED: " + e.getMessage());
				e.printStackTrace();
			}
			finally{

			}
		}

		return theUSSDResponse;
	}
} // End Class AppRequestProcessor
