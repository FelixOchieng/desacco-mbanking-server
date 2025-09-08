package ke.skyworld.mbanking.ussdapplication;

import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.ussd.USSDResponse;

public class AppActions {

    public AppActions(Long theUSSDMobileNo) {
    }

    public static USSDResponse action_INIT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Init(theUSSDRequest, theParam);
    }

    public static USSDResponse action_ERRONEOUS(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_ErroneousTransactions(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOGIN(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Login(theUSSDRequest, theParam);
    }

    public static USSDResponse action_GENERAL(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_General(theUSSDRequest, theParam);
    }

    public static USSDResponse action_BUY_GOODS(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_BuyGoodsMenus(theUSSDRequest, theParam);
    }

    public static USSDResponse action_RESET_PIN(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_ResetPin(theUSSDRequest, theParam);
    }


    public static USSDResponse action_MAIN_IN(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_MainIn(theUSSDRequest, theParam);
    }

    public static USSDResponse action_SET_PIN(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_SetPIN(theUSSDRequest, theParam);
    }

    public static USSDResponse action_UPDATE_EMAIL(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_UpdateEmail(theUSSDRequest, theParam);
    }

    public static USSDResponse action_ATM_CARD(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_ATMCard(theUSSDRequest, theParam);
    }

    public static USSDResponse action_SYSTEM_REPORTS(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_SystemReports(theUSSDRequest, theParam);
    }

    public static USSDResponse action_CHECK_BENEFICIARIES(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_CheckBeneficiaries(theUSSDRequest, theParam);
    }

    public static USSDResponse action_CHANGE_PIN(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_ChangePIN(theUSSDRequest, theParam);
    }

    public static USSDResponse action_MY_ACCOUNT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_MyAccount(theUSSDRequest, theParam);
    }

    public static USSDResponse action_ACCOUNT_STATUS(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_CheckAccountStatus(theUSSDRequest, theParam);
    }

    public static USSDResponse action_MY_ACCOUNT_BALANCE(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_BalanceEnquiry(theUSSDRequest, theParam);
    }

    public static USSDResponse action_MY_ACCOUNT_MINI_STATEMENT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_MiniStatement(theUSSDRequest, theParam);
    }

    public static USSDResponse action_MAPP_ACTIVATION(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_MobileApp(theUSSDRequest, theParam);
    }

    public static USSDResponse action_DIVIDEND_PAYSLIP(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_DividendPayslip(theUSSDRequest, theParam);
    }

    public static USSDResponse action_ACCOUNT_REGISTRATION(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_AccountRegistration(theUSSDRequest, theParam);
    }

    public static USSDResponse action_SELF_REGISTRATION(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_SelfRegistration(theUSSDRequest, theParam);
    }

    public static USSDResponse action_WITHDRAWAL(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Withdrawal(theUSSDRequest, theParam);
    }

    public static USSDResponse action_CASH_WITHDRAWAL_MAINTENANCE_ACCOUNT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_CashWithdrawal_Maintain_Accounts(theUSSDRequest, theParam);
    }

    public static USSDResponse action_CASH_WITHDRAWAL_ACTIVATION(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_ActivateWithdrawal(theUSSDRequest, theParam);
    }

    public static USSDResponse action_AGENT_WITHDRAWAL(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_AgentWithdrawal(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOAN(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Loan(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOAN_APPLICATION(USSDRequest theUSSDRequest, String theParam) {
        String strLoanApplicationType = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_TYPE.name());
        if (strLoanApplicationType == null) {
            String strMainInMenu = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.MAIN_IN_MENU.name());
            switch (strMainInMenu) {
                case "M_BOOSTA_LOAN": {
                    strLoanApplicationType = "126";
                    break;
                }
                case "DIVIDEND_ADVANCE_LOAN": {
                    strLoanApplicationType = "136";
                    break;
                }
            }
        }
        return new AppMenus().displayMenu_LoanApplication(theUSSDRequest, theParam, strLoanApplicationType);
    }


    public static USSDResponse action_LOAN_TERMS_AND_CONDITIONS(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_TermsAndConditionsMenus(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOAN_REPAYMENT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_LoanRepayment(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOAN_GUARANTORS(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_LoanGuarantors(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOANS_GUARANTEED(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_LoansGuaranteed(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOAN_ACTION_GUARANTORSHIP(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_LoansActionGuarantorship(theUSSDRequest, theParam);
    }

    public static USSDResponse action_LOAN_QUALIFICATION(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_CheckLoanQualification(theUSSDRequest, theParam);
    }

    public static USSDResponse action_FUNDS_TRANSFER(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_FundTransfer(theUSSDRequest, theParam);
    }

    public static USSDResponse action_PAY_MOD_BILLS(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_PayMODBills(theUSSDRequest, theParam);
    }

    public static USSDResponse action_FUNDS_TRANSFER_INTERNAL(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_FundTransferInternal(theUSSDRequest, theParam);
    }

    public static USSDResponse action_FUNDS_TRANSFER_EXTERNAL(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_FundTransferExternal(theUSSDRequest, theParam);
    }


    public static USSDResponse action_DEPOSIT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Deposit(theUSSDRequest, theParam);
    }

    public static USSDResponse action_UTILITIES(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Utilities(theUSSDRequest, theParam);
    }

    public static USSDResponse action_ETOPUP(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Etopup(theUSSDRequest, theParam);
    }

    public static USSDResponse action_PAY_BILL(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_PayBill(theUSSDRequest, theParam);
    }

    public static USSDResponse action_PAY_BILL_MAINTENANCE_ACCOUNT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_Paybill_Maintain_Accounts(theUSSDRequest, theParam);
    }

    public static USSDResponse action_FUNDS_TRANSFER_EXTERNAL_MAINTENANCE_ACCOUNT(USSDRequest theUSSDRequest, String theParam) {
        return new AppMenus().displayMenu_FundTransferExternal_Maintain_Accounts(theUSSDRequest, theParam);
    }
} // End Class MainMenu
