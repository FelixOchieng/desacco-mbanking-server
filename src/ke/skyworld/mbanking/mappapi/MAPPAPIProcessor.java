package ke.skyworld.mbanking.mappapi;

import ke.skyworld.lib.mbanking.mapp.MAPPConstants;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.lib.mbanking.mapp.MAPPResponse;
import ke.skyworld.mbanking.ussdapi.APIUtils;

public class MAPPAPIProcessor {
    public MAPPResponse processMAPPAPI(MAPPRequest theMAPPRequest){

        MAPPResponse theMAPPResponse = null;

        MAPPAPI theMAPPAPI = new MAPPAPI();

        try{
            String strAction = theMAPPRequest.getAction();

            //Log Request
            System.out.println("MAPP PROCESSING...");
            System.out.println("Server ID  : "+theMAPPRequest.getServerID());
            System.out.println("Session ID : "+theMAPPRequest.getSessionID());
            System.out.println("Mobile No  : "+theMAPPRequest.getUsername());
            System.out.println("getMAPPAction() = "+strAction);

            //System.out.println("********** REQUEST XML **************");
            //System.out.println(APIUtils.convertNodeToString(theMAPPRequest.getMSG()));
            //System.out.println("********** END OF REQUEST XML **************");
            //System.out.println();

            switch (strAction){
                case "LOGIN": {
                    theMAPPResponse = theMAPPAPI.userLogin(theMAPPRequest, APIConstants.OTP_TYPE.LOGIN);
                    break;
                }
                case "PASSWORD_VERIFICATION": {
                    theMAPPResponse = theMAPPAPI.userLogin(theMAPPRequest, ke.skyworld.mbanking.mappapi.APIConstants.OTP_TYPE.TRANSACTIONAL);
                    break;
                }
                case "ACTIVATE_MOBILE_APP": {
                    theMAPPResponse = theMAPPAPI.validateOTP(theMAPPRequest, APIConstants.OTP_TYPE.ACTIVATION);
                    break;
                }
                case "ACTIVATE_MOBILE_APP_WITH_KYC": {
                    theMAPPResponse = theMAPPAPI.activateMobileAppWithKYC(theMAPPRequest);
                    break;
                }
                case "GET_ACCOUNTS":{
                    boolean blForwithdrawal = false;
                    theMAPPResponse = theMAPPAPI.getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.ALL, blForwithdrawal, strAction);
                    break;
                }
                case "GET_ATM_CARDS":{
                    theMAPPResponse = theMAPPAPI.getATMCards(theMAPPRequest);
                    break;
                }
                case "GET_WITHDRAWAL_ACCOUNTS_AND_MOBILE_MONEY_SERVICES": {
                    theMAPPResponse = theMAPPAPI.getWithdrawalAccountsAndMobileMoneyServices(theMAPPRequest, MAPPConstants.AccountType.FOSA);
                    break;
                }
                case "WITHDRAW_MONEY_VIA_MPESA":{
                    theMAPPResponse = theMAPPAPI.mobileMoneyWithdrawal(theMAPPRequest);
                    break;
                }
                case "CASH_WITHDRAWAL":{
                    theMAPPResponse = theMAPPAPI.mobileMoneyWithdrawal(theMAPPRequest);
                    break;
                }
                case "WITHDRAW_MONEY_VIA_ATM": {
                    theMAPPResponse = theMAPPAPI.mobileMoneyWithdrawal(theMAPPRequest);
                    break;
                }
                case "WITHDRAW_MONEY_VIA_AGENT": {
                    theMAPPResponse = theMAPPAPI.mobileMoneyWithdrawal(theMAPPRequest);
                    break;
                }
                case "GET_TRANSACTION_ACCOUNTS_AND_DEPOSIT_SERVICES": {
                    boolean blForwithdrawal = false;
                    theMAPPResponse = theMAPPAPI.getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.ALL, blForwithdrawal, strAction);
                    break;
                }
                case "DEPOSIT_MONEY": {
                    theMAPPResponse = theMAPPAPI.depositMoney(theMAPPRequest);
                    break;
                }
                case "GET_WITHDRAWAL_ACCOUNTS": {
                    theMAPPResponse = theMAPPAPI.getWithdrawalAccounts(theMAPPRequest, MAPPConstants.AccountType.FOSA);
                    break;
                }
                case "GET_BANK_TRANSFER_ACCOUNTS": {
                    theMAPPResponse = theMAPPAPI.getWithdrawalAccountsAndBanks(theMAPPRequest, MAPPConstants.AccountType.FOSA);
                    break;
                }
                case "GET_WITHDRAWAL_ACCOUNTS_AND_PAY_BILL_SERVICES": {
                    theMAPPResponse = theMAPPAPI.getWithdrawalAccountsAndPaybillServices(theMAPPRequest, MAPPConstants.AccountType.FOSA);
                    break;
                }
                case "BUY_AIRTIME": {
                    theMAPPResponse = theMAPPAPI.buyAirtime(theMAPPRequest);
                    break;
                }
                case "GET_LOANS_WITH_PAYMENT_DETAILS": {
                    theMAPPResponse = theMAPPAPI.getMemberLoansWithPaymentDetails(theMAPPRequest);
                    break;
                }
                case "GET_LOAN_ACCOUNTS": {
                    theMAPPResponse = theMAPPAPI.getWithdrawalAccounts(theMAPPRequest, MAPPConstants.AccountType.FOSA);
                    break;
                }
                case "GET_LOAN_TYPES": {
                    theMAPPResponse = theMAPPAPI.getLoanTypes(theMAPPRequest);
                    break;
                }
                case "GET_LOANS": {
                    theMAPPResponse = theMAPPAPI.getMemberLoans(theMAPPRequest);
                    break;
                }
                case "GET_LOANS_WITH_GUARANTORS": {
                    theMAPPResponse = theMAPPAPI.getMemberLoans(theMAPPRequest);
                    break;
                }
                case "UPDATE_LOAN_GUARANTOR_STATUS": {
                    theMAPPResponse = theMAPPAPI.updateLoanGuarantorStatus(theMAPPRequest);
                    break;
                }
                case "PAY_LOAN": {
                    theMAPPResponse = theMAPPAPI.loanRepayment(theMAPPRequest);
                    break;
                }
                case "LOAN_BALANCE": {
                    theMAPPResponse = theMAPPAPI.loanBalanceEnquiry(theMAPPRequest);
                    break;
                }
                case "DISABLE_ATM_CARD": {
                    theMAPPResponse = theMAPPAPI.disableATMCard(theMAPPRequest);
                    break;
                }
                case "GET_TRANSFER_ACCOUNTS": {
                    theMAPPResponse = theMAPPAPI.getTransferAccounts(theMAPPRequest);
                    break;
                }
                case "GET_QR_TRANSACTION_ACCOUNTS": {
                    boolean blForwithdrawal = false;
                    theMAPPResponse = theMAPPAPI.getBankAccounts(theMAPPRequest, MAPPConstants.AccountType.ALL, blForwithdrawal, strAction);
                    break;
                }
                case "INTERNAL_FUNDS_TRANSFER": {
                    theMAPPResponse = theMAPPAPI.fundsTransfer(theMAPPRequest);
                    break;
                }
                case "BANK_TRANSFER": {
                    theMAPPResponse = theMAPPAPI.bankTransferViaB2B(theMAPPRequest);
                    break;
                }
                case "PAY_BILL": {
                    theMAPPResponse = theMAPPAPI.payBill(theMAPPRequest);
                    break;
                }
                case "ACCOUNT_BALANCE": {
                    theMAPPResponse = theMAPPAPI.accountBalanceEnquiry(theMAPPRequest);
                    break;
                }
                case "LOAN_LIMIT": {
                    theMAPPResponse = theMAPPAPI.checkLoanLimit(theMAPPRequest);
                    break;
                }
                case "ACCOUNT_STATEMENT": {
                    theMAPPResponse = theMAPPAPI.accountStatement(theMAPPRequest);
                    break;
                }
                case "ACCOUNT_STATEMENT_BASE64": {
                    theMAPPResponse = theMAPPAPI.accountStatementBase64(theMAPPRequest);
                    break;
                }
                case "CHANGE_PASSWORD": {
                    theMAPPResponse = theMAPPAPI.changePassword(theMAPPRequest);
                    break;
                }
                case "ENCRYPT_TEXT": {
                    theMAPPResponse = theMAPPAPI.encryptText(theMAPPRequest);
                    break;
                }
                case "DECRYPT_TEXT": {
                    theMAPPResponse = theMAPPAPI.decryptText(theMAPPRequest);
                    break;
                }
                case "UPDATE_EMAIL": {
                    theMAPPResponse = theMAPPAPI.updateEmailAddress(theMAPPRequest);
                    break;
                }
                case "APPLY_LOAN": {
                    theMAPPResponse = theMAPPAPI.applyLoan(theMAPPRequest);
                    break;
                }
                case "LOAN_STATEMENT": {
                    theMAPPResponse = theMAPPAPI.loanStatement(theMAPPRequest);
                    break;
                }
                case "LOAN_GUARANTORS": {
                    theMAPPResponse = theMAPPAPI.loanGuarantors(theMAPPRequest);
                    break;
                }
                case "GET_LOANS_PENDING_GUARANTORS": {
                    theMAPPResponse = theMAPPAPI.getMemberLoansWithPendingGuarantors(theMAPPRequest);
                    break;
                }
                case "ADD_LOAN_GUARANTORS": {
                    theMAPPResponse = theMAPPAPI.addLoanGuarantors(theMAPPRequest);
                    break;
                }
                case "LOANS_GUARANTEED":
                case "LOAN_GUARANTORSHIP_REQUESTS": {
                    theMAPPResponse = theMAPPAPI.loansGuaranteed(theMAPPRequest);
                    break;
                }
                case "GET_MEMBER_NAME": {
                    theMAPPResponse = theMAPPAPI.getMemberName(theMAPPRequest);
                    break;
                }
                case "GET_EMAIL_ADDRESS": {
                    theMAPPResponse = theMAPPAPI.getMemberEmailAddress(theMAPPRequest);
                    break;
                }
                case "ADD_UTILITY_AND_PAYBILL_ACCOUNT": {
                    theMAPPResponse = theMAPPAPI.addOrDeleteUtilityAndPaybillAccount(theMAPPRequest, "ADD");
                    break;
                }
                case "DELETE_UTILITY_AND_PAYBILL_ACCOUNT": {
                    theMAPPResponse = theMAPPAPI.addOrDeleteUtilityAndPaybillAccount(theMAPPRequest, "DELETE");
                    break;
                }
                case "GENERATE_OTP": {
                    theMAPPResponse = theMAPPAPI.generateOTP(theMAPPRequest);
                    break;
                }
                case "VALIDATE_OTP": {
                    theMAPPResponse = theMAPPAPI.validateOTP(theMAPPRequest, APIConstants.OTP_TYPE.TRANSACTIONAL);
                    break;
                }
                case "REGISTER_NEW_MEMBER": {
                    theMAPPResponse = theMAPPAPI.registerMember(theMAPPRequest);
                    break;
                }
                case "GET_HOME_PAGE_ADDONS": {
                    theMAPPResponse = theMAPPAPI.getHomePageAddons(theMAPPRequest);
                    break;
                }
                case "CHECK_MY_BENEFICIARIES": {
                    theMAPPResponse = theMAPPAPI.checkMyBeneficiaries(theMAPPRequest);
                    break;
                }

                case "GET_DIVIDEND_PAYSLIP": {
                    theMAPPResponse = theMAPPAPI.getDividendPayslipMapp(theMAPPRequest);
                    break;
                }

                case "UNKNOWN": {
                    theMAPPResponse = theMAPPAPI.accountBalanceEnquiry(theMAPPRequest);
                    break;
                }
                default: {
                    theMAPPResponse = theMAPPAPI.accountBalanceEnquiry(theMAPPRequest);
                    break;
                }
            }
        } catch (Exception e){
            System.err.println("MAPPAPIProcessor.processMAPPAPI() ERROR : " + e.getMessage());
        }finally{
            theMAPPAPI = null;
        }

        //System.out.println();
        //System.out.println("********** RESPONSE XML **************");
        //if(theMAPPResponse != null && theMAPPResponse.getMSG() != null) {
        //    System.out.println(APIUtils.convertNodeToString(theMAPPResponse.getMSG()));
        //} else {
        //    System.out.println("NULL MAPP Response MSG XML");
        //}
        //System.out.println("********** END OF RESPONSE XML **************");
        //System.out.println();

        return  theMAPPResponse;
    }
}
