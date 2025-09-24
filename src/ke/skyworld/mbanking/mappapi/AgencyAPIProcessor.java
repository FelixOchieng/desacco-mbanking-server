package ke.skyworld.mbanking.mappapi;

import ke.skyworld.mbanking.agencyapi.AgencyAPI;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.lib.mbanking.mapp.MAPPResponse;

public class AgencyAPIProcessor {
    public MAPPResponse processAgencyAPI(MAPPRequest theMAPPRequest){

        MAPPResponse theMAPPResponse = null;

        AgencyAPI theAgencyAPI = new AgencyAPI();
        MAPPAPI theMAPPAPI = new MAPPAPI();

        try{
            String strAction = theMAPPRequest.getAction();

            switch (strAction){
                //Authentication
                case "LOGIN": {
                    theMAPPResponse = theAgencyAPI.fnLogin(theMAPPRequest, APIConstants.OTP_TYPE.LOGIN, true);
                    break;
                }
                case "PASSWORD_VERIFICATION": {
                    theMAPPResponse = theAgencyAPI.fnLogin(theMAPPRequest, APIConstants.OTP_TYPE.TRANSACTIONAL_WITH_AGENT_OTP, false);
                    break;
                }
                case "PASSWORD_VERIFICATION_WITH_CUSTOMER_OTP": {
                    theMAPPResponse = theAgencyAPI.fnLogin(theMAPPRequest, APIConstants.OTP_TYPE.TRANSACTIONAL_WITH_CUSTOMER_OTP, false);
                    break;
                }

                //One Time Password Generation
                case "GENERATE_OTP": {
                    theMAPPResponse = theAgencyAPI.fnGenerateOTP(theMAPPRequest, "");
                    break;
                }

                //One Time Password / Activation Code Validation
                case "VALIDATE_OTP": {
                    theMAPPResponse = theAgencyAPI.fnValidateOTP(theMAPPRequest, APIConstants.OTP_TYPE.TRANSACTIONAL);
                    break;
                }
                case "ACTIVATE_MOBILE_APP": {
                    theMAPPResponse = theAgencyAPI.fnValidateOTP(theMAPPRequest, APIConstants.OTP_TYPE.ACTIVATION);
                    break;
                }

                //Accounts & Search
                case "HOME_PAGE_REFRESH": {
                    theMAPPResponse = theAgencyAPI.fnRefreshHomePage(theMAPPRequest);
                    break;
                }
                case "GET_AGENT_ACCOUNTS": {
                    theMAPPResponse = theAgencyAPI.fnGetAgentAccounts(theMAPPRequest);
                    break;
                }
                case "FLOAT_DEPOSIT": {
                    theMAPPResponse = theAgencyAPI.fnFloatDeposit(theMAPPRequest);
                    break;
                }
                case "GET_CUSTOMER_DATA_AND_ACCOUNTS": {
                    theMAPPResponse = theAgencyAPI.fnGetCustomerSearchResult(theMAPPRequest);
                    break;
                }
                case "GET_EMPLOYEE_DATA": {
                    theMAPPResponse = theAgencyAPI.fnGetEmployeeData(theMAPPRequest);
                    break;
                }
                case "GET_EMPLOYERS": {
                    theMAPPResponse = theAgencyAPI.fnGetEmployers(theMAPPRequest);
                    break;
                }

                //Transact
                case "CASH_WITHDRAWAL":
                case "CASH_DEPOSIT":
                case "ACCOUNT_BALANCE":
                case "LOAN_BALANCE":
                case "ACCOUNT_STATEMENT":
                case "LOAN_PAYMENT": {
                    theMAPPResponse = theAgencyAPI.fnPerformTransaction(theMAPPRequest);
                    break;
                }

                case "MEMBER_REGISTRATION": {
                    theMAPPResponse = theAgencyAPI.fnRegisterMember(theMAPPRequest);
                    break;
                }

                case "UPLOAD_IMAGES": {
                    theMAPPResponse = theAgencyAPI.fnUploadImages(theMAPPRequest);
                    break;
                }

                case "MY_ACCOUNT_BALANCE": {
                    theMAPPResponse = theMAPPAPI.accountBalanceEnquiry(theMAPPRequest);
                    break;
                }

                case "MY_ACCOUNT_STATEMENT": {
                    theMAPPResponse = theAgencyAPI.fnGetAccountStatement(theMAPPRequest);
                    break;
                }

                case "GET_AGENT_REPORTS": {
                    theMAPPResponse = theAgencyAPI.fnAgentReports(theMAPPRequest);
                    break;
                }

                case "GET_TRANSACTION_PRINTOUTS": {
                    theMAPPResponse = theAgencyAPI.fnAgentReportPrintouts(theMAPPRequest);
                    break;
                }

                case "CHANGE_PASSWORD": {
                    theMAPPResponse = theAgencyAPI.fnChangePassword(theMAPPRequest);
                    break;
                }

                case "SENS_SMS": {
                    theMAPPResponse = theAgencyAPI.fnSendCustomerSMS(theMAPPRequest);
                    break;
                }

                default: {
                    theMAPPResponse = theAgencyAPI.fnMethodNotFound(theMAPPRequest);
                    break;
                }
            }
        } catch (Exception e){
            System.err.println("AgencyAPIProcessor.processAgencyAPI() ERROR : " + e.getMessage());
        }finally{
            theAgencyAPI = null;
        }

        return  theMAPPResponse;
    }
}
