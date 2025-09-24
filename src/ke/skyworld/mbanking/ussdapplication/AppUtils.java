package ke.skyworld.mbanking.ussdapplication;

public class AppUtils {

	public AppUtils(){
	}

	public static AppConstants.USSDDataType getUSSDDataTypeFromName(String theUSSDDataTypeName){
		AppConstants.USSDDataType theUSSDDataType = AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_DATA_TYPE_NOT_FOUND;

		for(AppConstants.USSDDataType  ussdDataType: AppConstants.USSDDataType.values()){
			if( theUSSDDataTypeName.equalsIgnoreCase(ussdDataType.name()) ) {
				theUSSDDataType = ussdDataType;
				break;
			}
		}
		return theUSSDDataType;
	}

	public static AppConstants.USSDDataType getUSSDDataTypeFromValue(String theUSSDDataTypeValue){
		AppConstants.USSDDataType theUSSDDataType = AppConstants.USSDDataType.USSD_PROCESS_OVERIDE_DATA_TYPE_NOT_FOUND;

		for(AppConstants.USSDDataType  ussdDataType: AppConstants.USSDDataType.values()){
			if( theUSSDDataTypeValue.equalsIgnoreCase(ussdDataType.getValue()) ) {
				theUSSDDataType = ussdDataType;
				break;
			}
		}
		return theUSSDDataType;
	}

    public static String maskPhoneNumber(String phoneNumber) {

        try {
            phoneNumber = phoneNumber.replaceAll("\\s*", "");

            if (phoneNumber.length() < 6) {
                return phoneNumber;
            }

            String lastThreeDigits = phoneNumber.substring(phoneNumber.length() - 3);

            String maskedPart = "******";

            String remainingDigits = phoneNumber.substring(0, phoneNumber.length() - 6);

            return remainingDigits + maskedPart + lastThreeDigits;
        }catch (Exception e){
            e.printStackTrace();
        }

        return phoneNumber;
    }

	public static String sanitizePhoneNumber(String thePhoneNumber){
		thePhoneNumber = thePhoneNumber.trim();
		try {
			if(thePhoneNumber.startsWith("+")){
				thePhoneNumber = thePhoneNumber.replaceFirst("^\\+", "");
			}

			if(thePhoneNumber.matches("^2547\\d{8}$")){
				return thePhoneNumber;
			}

			if(thePhoneNumber.matches("^07\\d{8}$")) {
				return thePhoneNumber.replaceFirst("^0", "254");
			}

			if(thePhoneNumber.matches("^7\\d{8}$")){
				return "254"+thePhoneNumber;
			}

			return "INVALID_MOBILE_NUMBER";
		}catch (Exception e){
			e.printStackTrace();
			return e.getMessage();
		}
	}
}
