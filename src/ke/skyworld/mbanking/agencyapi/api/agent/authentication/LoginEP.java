package ke.skyworld.mbanking.agencyapi.api.agent.authentication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.mbanking.agencyapi.AgencyAPI;
import ke.skyworld.mbanking.agencyapi.AgencyAPIConstants;
import ke.skyworld.mbanking.agencyapi.AgencyAPIUtils;
import ke.skyworld.mbanking.agencyapi.api.agent.data.HomeRefreshEP;
import ke.skyworld.mbanking.agencyapi.models.AgencyAPIResponse;
import ke.skyworld.mbanking.agencyapi.utils.PrettyPrint;
import ke.skyworld.mbanking.agencyapi.utils.XMLUtils;
import ke.skyworld.mbanking.mappapi.APIConstants;
import ke.skyworld.mbanking.nav.cbs.CBSAgencyAPI;
import ke.skyworld.mbanking.ussdapi.APIUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.Date;

import static ke.skyworld.lib.mbanking.mapp.MAPPConstants.ResponseAction.CON;

public class LoginEP {
    public static AgencyAPIResponse fnAgentLogin(MAPPRequest theMAPPRequest, APIConstants.OTP_TYPE theOTPType, boolean blFirstLogin) {
        AgencyAPIResponse arRVal = new AgencyAPIResponse();
        try {
            /*Dynamic Request - Generated Using Passed Parameters*/

            String strUserName =theMAPPRequest.getUsername();
            String strPassword=theMAPPRequest.getPassword();
            strPassword= APIUtils.hashAgentPIN(strPassword, strUserName);

            boolean blOTPVerificationRequired = AgencyAPI.fnCheckOTPRequirement(theMAPPRequest, APIConstants.OTP_CHECK_STAGE.GENERATION).isEnabled();

            String strDeviceIMEI = AgencyAPIUtils.getStringFromNode("DEVICE/IMEI", theMAPPRequest.getMSG()).trim();
            String strDeviceSerialNumber = AgencyAPIUtils.getStringFromNode("DEVICE/SERIAL_NUMBER", theMAPPRequest.getMSG()).trim();
            String strDeviceMake = AgencyAPIUtils.getStringFromNode("DEVICE/MAKE", theMAPPRequest.getMSG()).trim();
            String strDeviceModel = AgencyAPIUtils.getStringFromNode("DEVICE/MODEL", theMAPPRequest.getMSG()).trim();
            String strDeviceProcessorID = AgencyAPIUtils.getStringFromNode("DEVICE/PROCESSOR_ID", theMAPPRequest.getMSG()).trim();
            String strAppIdentifier = AgencyAPIUtils.getStringFromNode("APP/IDENTIFIER", theMAPPRequest.getMSG()).trim();
            String strAppVersion = AgencyAPIUtils.getStringFromNode("APP/VERSION_CODE", theMAPPRequest.getMSG()).trim();
            String strAppEnvironment = AgencyAPIUtils.getStringFromNode("APP/ENVIRONMENT", theMAPPRequest.getMSG()).trim();

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("JSON LOGIN REQUEST TO CBS");
                System.out.println("--------------------------------------------------");
                System.out.println("UserName: " + strUserName);
                System.out.println("Password: ***********");
                System.out.println("DeviceIMEI: " + strDeviceIMEI);
                System.out.println("DeviceSerialNumber: " + strDeviceSerialNumber);
                System.out.println("DeviceMake: " + strDeviceMake);
                System.out.println("DeviceModel: " + strDeviceModel);
                System.out.println("DeviceProcessorID: " + strDeviceProcessorID);
                System.out.println("DeviceProcessorID: " + strDeviceProcessorID);
                System.out.println("AppIdentifier: " + strAppIdentifier);
                System.out.println("AppVersion: " + strAppVersion);
                System.out.println("AppEnvironment: "+ strAppEnvironment);
                System.out.println("**************************************************\n\n");
            }

            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            /*CALL NAV HERE*/
            String strResponse = CBSAgencyAPI.agentLogin(
                    "AGENT_LOGIN",
                    false,
                    strUserName,
                    strPassword,
                    strDeviceIMEI,
                    strDeviceSerialNumber,
                    strDeviceMake,
                    strDeviceModel,
                    strDeviceProcessorID,
                    strDeviceProcessorID,
                    strAppVersion,
                    strAppEnvironment
            );

            System.out.println("NAV Responded with: "+strResponse);

            JsonObject joResponseRoot = new JsonParser().parse(strResponse).getAsJsonObject();
            String strJsonResponse = PrettyPrint.fnPrettifyJson(joResponseRoot);

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("JSON CHANGE AGENT LOGIN RESPONSE FROM CBS");
                System.out.println("--------------------------------------------------");
                System.out.println(strJsonResponse);
                System.out.println("**************************************************\n\n");
            }
            /*Declare Strings from JsonPath */
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            Object obJsonResponse = Configuration.defaultConfiguration().jsonProvider().parse(strResponse);

            String strResponseStatus = JsonPath.parse(obJsonResponse).read("$.response.status");

            String strTitle = JsonPath.parse(obJsonResponse).read("$.response.payload.title");
            String strMessage= JsonPath.parse(obJsonResponse).read("$.response.payload.message");

            /*XML Response - From CBS JSON*/
            /*------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------*/
            DocumentBuilderFactory dfDocFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dblDocBuilder = dfDocFactory.newDocumentBuilder();
            Document dcDocument = dblDocBuilder.newDocument();

            Element elAllData = dcDocument.createElement("DATA");
            dcDocument.appendChild(elAllData);

            MAPPConstants.ResponseAction enResponseAction;
            MAPPConstants.ResponseStatus enResponseStatus;

            arRVal.setResponseAction(MAPPConstants.ResponseAction.CON);
            arRVal.setResponseStatus(MAPPConstants.ResponseStatus.ERROR);
            arRVal.setTitle(strTitle);
            arRVal.setResponseText(strMessage);

            switch(strResponseStatus){
                case "SUCCESS":{
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.SUCCESS;
                    break;
                }
                case "INCORRECT_PIN": {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    if (theOTPType != APIConstants.OTP_TYPE.TRANSACTIONAL) {
                        enResponseAction = MAPPConstants.ResponseAction.CON;
                    }
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;

                    String strUserLoginAttemptsCount = JsonPath.parse(obJsonResponse).read("$.response.payload.data.incorrect_pin_attempts");
                    String strUserNameFromLogin = JsonPath.parse(obJsonResponse).read("$.response.payload.data.user_name");

                    int intUserLoginAttemptsCount = Integer.parseInt(strUserLoginAttemptsCount);

                    String strResponseMessage = AgencyAPI.fnSuspendUserAccess(strUserName, intUserLoginAttemptsCount, "LOGIN", strUserNameFromLogin, theOTPType).get("MESSAGE");
                    if (!strResponseMessage.equals("")) {
                        strMessage = strResponseMessage;
                    }

                    break;
                }
                case "SUSPENDED": {
                    String strAuthType = "LOGIN";
                    String strLoginAttemptAction = CBSAgencyAPI.getUserLoginAttemptAction("", true, strUserName, strAuthType);
                    if (!strLoginAttemptAction.equals("SUSPENDED")) {
                        strAuthType = "OTP";
                        strLoginAttemptAction = CBSAgencyAPI.getUserLoginAttemptAction("", true, strUserName, strAuthType);
                    }

                    XMLGregorianCalendar gcExpiryDate = CBSAgencyAPI.getUserLoginAttemptExpiry("", true, strUserName, strAuthType);
                    Date dtExpiryDate = gcExpiryDate.toGregorianCalendar().getTime();

                    Date dtNow = new Date();

                    long dblDuration = dtExpiryDate.getTime() - dtNow.getTime();

                    String strTryAgainIn = "Please try again in " + APIUtils.getPrettyDateTimeDifferenceRoundedUp(dtNow, dtExpiryDate);


                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                case "BLOCKED":
                case "ERROR": {
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                case "NOT_FOUND": {
                    enResponseAction = CON;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                    break;
                }
                default:{
                    enResponseAction = MAPPConstants.ResponseAction.END;
                    enResponseStatus = MAPPConstants.ResponseStatus.ERROR;
                }
            }

            arRVal.setResponseAction(enResponseAction);
            arRVal.setResponseStatus(enResponseStatus);


            Element elTitle = dcDocument.createElement("TITLE");
            elTitle.setTextContent(strTitle);
            elAllData.appendChild(elTitle);

            Element elMessage = dcDocument.createElement("MESSAGE");
            elMessage.setTextContent(strMessage);
            elAllData.appendChild(elMessage);

            String strResponseXML = XMLUtils.fnTransformXMLDocument(dcDocument);
            String strPrettyResponseXML = PrettyPrint.fnPrettifyXML(strResponseXML, 4);

            if(AgencyAPIConstants.AGENCY_API_VERSION == AgencyAPIConstants.AgencyAPIVersion.DEVELOPMENT) {
                System.out.println("**************************************************");
                System.out.println("XML RESPONSE GENERATED FROM CBS JSON");
                System.out.println("--------------------------------------------------");
                System.out.println(strPrettyResponseXML);
                System.out.println("**************************************************");
            }

            arRVal.setResponseXML(strResponseXML);

            if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.SUCCESS.getValue())){
                arRVal.setCharge("YES");

                AgencyAPIResponse arHomeRefresh = HomeRefreshEP.fnRefresh(theMAPPRequest);
                if(arHomeRefresh.getResponseStatus() == MAPPConstants.ResponseStatus.SUCCESS) {
                    arRVal.setResponseXML(arHomeRefresh.getResponseXML());

                    String strMobileNUmber = MBankingXMLFactory.getXPathValueFromXMLString("/DATA/USER/MOBILE_NUMBER", arHomeRefresh.getResponseXML());

                    if(strResponseStatus.equals("SUCCESS")){
                        if(blOTPVerificationRequired || blFirstLogin){
                            AgencyAPI.fnGenerateOTP(theMAPPRequest, strMobileNUmber);
                        }
                    }
                }
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.ERROR.getValue())){
                arRVal.setCharge("NO");
            } else if(strResponseStatus.equalsIgnoreCase(AgencyAPIConstants.AgencyAPIResponseStatus.FAILED.getValue())){
                arRVal.setCharge("NO");
                arRVal.setResponseStatus(MAPPConstants.ResponseStatus.FAILED);
            } else {
                arRVal.setCharge("NO");
            }
        } catch (Exception e){
            System.err.println(LoginEP.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return arRVal;
    }
}
