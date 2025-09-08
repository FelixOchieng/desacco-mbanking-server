package ke.skyworld.mbanking.msgapi;

import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.lib.mbanking.msg.*;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.mbanking.ussdapi.APIConstants;
import ke.skyworld.mbanking.ussdapi.USSDAPI;

import java.util.HashMap;
import java.util.UUID;

import static ke.skyworld.mbanking.ussdapi.APIUtils.getUserIdentifierDetails;

public class MSGAPI {

    public static void processOnlineMOMSG(MSG theMOMSG, MOMSGOnlineResponse theMOMSGOnlineResponse){

        String strOriginatorID = "";
        String strMessage = "";
        String strCategory = "";

        try{
            String strReceiver = theMOMSG.getReceiver();
            String strReceiverType = theMOMSG.getReceiverType();
            String strSourceApplication = theMOMSG.getSourceApplication();
            String strRequestApplication = theMOMSG.getRequestApplication();
            String strRequestCorrelationID = theMOMSG.getRequestCorrelationID();
            String strDestinationReference = theMOMSG.getDestinationReference();
            String strSender = theMOMSG.getSender();
            String strSenderType = theMOMSG.getSenderType();
            String strMOMSG = theMOMSG.getMessage();
            String strSourceReference = theMOMSG.getSourceReference();

            //Request
            System.out.println();
            System.out.println("*************************************************");
            System.out.println("            MO MSG Request Details");
            System.out.println("*************************************************");
            System.out.println("Sender               : "+strSender);
            System.out.println("SenderType           : "+strSenderType);
            System.out.println("MO MSG               : "+strMOMSG);
            System.out.println("SourceReference      : "+strSourceReference);
            System.out.println("Receiver             : "+strReceiver);
            System.out.println("ReceiverType         : "+strReceiverType);
            System.out.println("SourceApplication    : "+strSourceApplication);
            System.out.println("RequestApplication   : "+strRequestApplication);
            System.out.println("RequestCorrelationID : "+strRequestCorrelationID);
            System.out.println("DestinationReference : "+strDestinationReference);
            System.out.println("*************************************************");
            System.out.println("            MO MSG Request Details");
            System.out.println("*************************************************");

            //Response
            strOriginatorID = UUID.randomUUID().toString().toUpperCase();
            //strNAVResponse = Navision.getPort().accountBalanceForShortCode(strMobileNumber);
            strMessage = "Dear member, your balance enquiry request has been received successfully. Please wait as it is being processed.";
            strCategory = "BALANCE_ENQUIRY";
            String str_MT_MSG_ReceiverType = strSenderType;
            String str_MT_MSG_Receiver = strSender;
            int intPriority = 210;//ke.skyworld.mbanking.mbankingapi.APIConstants.MSG_PRIORITY.TRANSACTION_NOTIFICATION.getValue();

            //Thread to do Balance Enquiry and send message
            String finalStrCategory = strCategory;
            Thread worker = new Thread(() -> {
                USSDAPI theUSSDAPI = new USSDAPI();

                //Check User
                // APIConstants.CheckUserReturnVal checkUserReturnVal = theUSSDAPI.checkUser(str_MT_MSG_Receiver);
                HashMap<String, String> userIdentifierDetails = getUserIdentifierDetails(str_MT_MSG_Receiver);


                String checkUserMessage = "";

                if (userIdentifierDetails == null || userIdentifierDetails.isEmpty()) {
                    checkUserMessage = "Dear user, You are not registered for our mobile banking services. Please contact your nearest branch for assistance.";
                    sendMSG(MSGConstants.MSGMode.SAF, str_MT_MSG_ReceiverType, str_MT_MSG_Receiver, checkUserMessage, strRequestApplication,
                            strSourceApplication, intPriority, finalStrCategory, MSGConstants.Sensitivity.NORMAL, strRequestCorrelationID, strSourceReference);
                } else {
                    checkUserMessage = "Dear member, an error occurred while processing your request. Please try again later.";
                    sendMSG(MSGConstants.MSGMode.SAF, str_MT_MSG_ReceiverType, str_MT_MSG_Receiver, checkUserMessage, strRequestApplication,
                            strSourceApplication, intPriority, finalStrCategory, MSGConstants.Sensitivity.NORMAL, strRequestCorrelationID, strSourceReference);
                }

            });
            worker.start();
        }catch (Exception e){
            strOriginatorID = UUID.randomUUID().toString().toUpperCase();
            strMessage = "Dear member, there was an error processing your balance enquiry request. Please try again later.";
            strCategory = "BALANCE_ENQUIRY";
        }finally {
            theMOMSGOnlineResponse.setDestinationReference(strOriginatorID);
            theMOMSGOnlineResponse.setSensitivity(MSGConstants.Sensitivity.NORMAL.getValue());
            theMOMSGOnlineResponse.setMessageFormat(MSGConstants.MessageFormat.TEXT.getValue());
            theMOMSGOnlineResponse.setMessage(strMessage);
            theMOMSGOnlineResponse.setCategory(strCategory);

            theMOMSGOnlineResponse.setResponse(PESAConstants.PESAResponse.SUCCESS.getValue());
            theMOMSGOnlineResponse.setResponseDescription("CBS RESPONSE: " + "SUCCESS");
            theMOMSGOnlineResponse.setDateCreated(MBankingDB.getDBDateTime());
        }
    }

    public static void processOfflineMOMSG(MSG theMOMSG, MOMSGOfflineResponse theMOMSGOfflineResponse){
        try{

        }catch (Exception e){

        }finally {

        }
    }

    public static void processMSGResult(MTMSGResult theMTMSGResult, MTMSGResultResponse theMTMSGResultResponse){
        try{

        }catch (Exception e){

        }finally {

        }
    }

    public static int sendMSG(MSGConstants.MSGMode theMSGMode, String theReceiverType, String theReceiver, String theMessage, String theRequestApplication,
                              String theSourceApplication, int thePriority, String theCategory,
                              MSGConstants.Sensitivity theSensitivity, String theRequestCorrelationID, String theSourceReference){
        int status = -1;
        try {
            String theOriginatorID = UUID.randomUUID().toString().toUpperCase();
            String theSenderType = MSGConstants.SenderType.SENDER_ID.getValue();
            String theMessageFormat = "TEXT";
            long theProductID = Long.parseLong(MBankingAPI.getValueFromLocalParams(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/PRODUCT_ID"));
            String theSender = MBankingAPI.getValueFromLocalParams(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/SENDER");
            String theCommand = "BulkSMS";
            String theCharge = "YES";

            status = MSGProcessor.sendMSG(theOriginatorID, theProductID, theSenderType, theSender, theReceiverType, theReceiver, theMessageFormat, theMessage,
                    theCommand, theSensitivity, theCategory, thePriority, theCharge, theMSGMode, theRequestApplication, theRequestCorrelationID,
                    theSourceApplication, theSourceReference);

            if(status <= 0){
                System.err.println("ERROR Sending "+theCategory+" - "+theMessage+" to " + theReceiver + "\n");
            }
        } catch (Exception e){
            System.err.println("USSDAPI.sendMSG() ERROR : " + e.getMessage());
        }

        return status;
    }
}
