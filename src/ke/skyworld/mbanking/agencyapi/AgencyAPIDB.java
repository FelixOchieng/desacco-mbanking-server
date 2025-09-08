package ke.skyworld.mbanking.agencyapi;

import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.utils.NamedParameterStatement;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.ResultSet;
import java.util.LinkedList;

public class AgencyAPIDB {
    static String strUserDataTableName = "agency_banking_user_data";
    static String strReceiptsTableName = "agency_banking_receipts";

    public static boolean fnInsertAgentData(String theUserName, String theFingerprintData) {
        boolean rVal = false;
        NamedParameterStatement p = null;
        String strSQL = null;

        try {
            strSQL = "INSERT INTO " + strUserDataTableName + " (username, fingerprint_data) VALUES (:username, :fingerprint_data);";
            p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
            p.setString("username", theUserName);
            p.setString("fingerprint_data", theFingerprintData);
            rVal = p.execute();

            p.close();
            p = null;
            strSQL = null;
        } catch (Exception var22) {
            System.err.println("Error message: " + var22.getMessage());
        } finally {
            strSQL = null;

            if (p != null) {
                try {
                    p.close();
                } catch (Exception var20) {
                }
            }

            p = null;
            strSQL = null;
        }

        return rVal;
    }

    public static boolean fnUpdateAgentData(String theUserName, String theFingerprintData) {
        boolean rVal = false;
        NamedParameterStatement p = null;
        String strSQL = null;

        try {
            strSQL = "UPDATE " + strUserDataTableName + " SET username = :username, fingerprint_data = :fingerprint_data;";
            p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
            p.setString("username", theUserName);
            p.setString("fingerprint_data", theFingerprintData);
            rVal = p.execute();

            p.close();
            p = null;
            strSQL = null;
        } catch (Exception var22) {
            System.err.println("Error message: " + var22.getMessage());
        } finally {
            strSQL = null;

            if (p != null) {
                try {
                    p.close();
                } catch (Exception var20) {
                }
            }

            p = null;
            strSQL = null;
        }

        return rVal;
    }

    public static String fnSelectAgentData(String theUserName, String theRowToFetch) {
        String rVal = "";
        NamedParameterStatement p = null;
        ResultSet rs = null;
        String strSQL = null;

        try {
            strSQL = "SELECT "+theRowToFetch+" FROM " + strUserDataTableName + " WHERE username = :username";
            p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
            p.setString("username", theUserName);
            rs = p.executeQuery();
            if (rs.next()) {
                rVal = rs.getString(theRowToFetch);
            }

            rs.close();
            p.close();
            rs = null;
            p = null;
            strSQL = null;
        } catch (Exception var22) {
            System.err.println("Error message: " + var22.getMessage());
        } finally {
            strSQL = null;
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception var21) {
                }
            }

            if (p != null) {
                try {
                    p.close();
                } catch (Exception var20) {
                }
            }

            rs = null;
            p = null;
            strSQL = null;
        }

        return rVal;
    }

    public static boolean fnCheckIfAgentExists(String theUserName) {
        boolean rVal = false;
        NamedParameterStatement p = null;
        ResultSet rs = null;
        String strSQL = null;

        try {
            strSQL = "SELECT COUNT(*) as agent_count FROM " + strUserDataTableName + " WHERE username = :username";
            p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
            p.setString("username", theUserName);
            rs = p.executeQuery();
            if (rs.next()) {
                rVal = Integer.parseInt(rs.getString("agent_count")) > 0;
            }

            rs.close();
            p.close();
            rs = null;
            p = null;
            strSQL = null;
        } catch (Exception var22) {
            System.err.println("Error message: " + var22.getMessage());
        } finally {
            strSQL = null;
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception var21) {
                }
            }

            if (p != null) {
                try {
                    p.close();
                } catch (Exception var20) {
                }
            }

            rs = null;
            p = null;
            strSQL = null;
        }

        return rVal;
    }

    public static boolean fnInsertReceipt(String theUserName, String theSessionID, String theTransactionType, String thePrint) {
        boolean rVal = false;
        NamedParameterStatement p = null;
        String strSQL = null;

        try {
            strSQL = "INSERT INTO " + strReceiptsTableName + " (username, session_id, transaction_type, print) VALUES (:username, :session_id, :transaction_type, :print);";
            p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
            p.setString("username", theUserName);
            p.setString("session_id", theSessionID);
            p.setString("transaction_type", theTransactionType);
            p.setString("print", thePrint);
            rVal = p.execute();

            p.close();
            p = null;
            strSQL = null;
        } catch (Exception var22) {
            System.err.println("Error message: " + var22.getMessage());
        } finally {
            strSQL = null;

            if (p != null) {
                try {
                    p.close();
                } catch (Exception var20) {
                }
            }

            p = null;
            strSQL = null;
        }

        return rVal;
    }

    public static String fnSelectReceipts(String theUsername, LinkedList<String> llsSessionIds) {
        String rVal = "";
        NamedParameterStatement p = null;
        ResultSet rs = null;
        String strSQL = null;
        JSONArray jsonArray = new JSONArray();

        try {
            StringBuilder strSessionIdFilter = new StringBuilder();
            for (int count = 0; count < llsSessionIds.size(); count++) {
                strSessionIdFilter.append(":session_id_").append(count);
                if(count <= (llsSessionIds.size()-2)){
                    strSessionIdFilter.append(", ");
                }
            }

            strSQL = "SELECT " +
                    " json_merge(" +
                    "     json_object('username', username)," +
                    "     json_object('session_id', session_id)," +
                    "     json_object('transaction_type', transaction_type)," +
                    "     json_object('print', print)" +
                    " )" +
                    " as json_response, count(session_id) as count FROM "+strReceiptsTableName+" WHERE session_id IN("+strSessionIdFilter+") AND username = :username GROUP BY session_id;".trim();
            p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
            p.setString("username", theUsername);
            for (int count = 0; count < llsSessionIds.size(); count++) {
                p.setString("session_id_"+count, llsSessionIds.get(count));
            }
            rs = p.executeQuery();
            while (rs.next()) {
                if(Integer.parseInt(rs.getString("count")) > 0){
                    JSONObject jsonObject = new JSONObject(rs.getString("json_response"));
                    jsonArray.put(jsonObject);
                }
            }

            rVal = jsonArray.toString();

            rs.close();
            p.close();
            rs = null;
            p = null;
            strSQL = null;
        } catch (Exception var22) {
            System.err.println("Error message: " + var22.getMessage());
        } finally {
            strSQL = null;
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception var21) {
                }
            }

            if (p != null) {
                try {
                    p.close();
                } catch (Exception var20) {
                }
            }

            rs = null;
            p = null;
            strSQL = null;
        }

        return rVal;
    }
}
