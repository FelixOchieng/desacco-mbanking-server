package ke.skyworld.mbanking.ussdapi;
//import java.io.*;

import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingLocalParameters;
import ke.skyworld.lib.mbanking.pesa.PESAConstants;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.utils.NamedParameterStatement;

import java.sql.*;
import java.time.Duration;

public class USSDAPIDB {
    public static ResultSet executeQuery(String query) {

        Statement st = null;
        ResultSet rs = null;

        System.out.println("executeQuery(String query): " + query);
        try {
            if (MBankingDB.getConnection() != null) {
                st = MBankingDB.getConnection().createStatement();
				rs = st.executeQuery(query);
			}
		}
		catch (Exception e) {
			System.err.println ("\n\nexecuteQuery(String query): Error message: " + e.getMessage ()); /* ignore close errors */
		}
		finally {
			//try{st.close();}catch(Exception e){}
			//st = null;
		}
		return rs;
	}

	public static boolean isConnected()
	{
		if (MBankingDB.getConnection() != null)
		{
			try {
				return !MBankingDB.getConnection().isClosed();
			} catch (SQLException e) {
				return false;
			}
		}
		else
		{
			return false;		
		}
	}
	
	public static int isConnectionNull()
	{
		if (MBankingDB.getConnection() != null)
		{
			return 0;
		}
		else
		{
			return 1;		
		}
	}
	
	public static String getDBDateTime()
	{
		Statement st = null;
		ResultSet rs = null;
		String strDateTime=null;
		String strSQL = null;
		try
		{
			switch (MBankingLocalParameters.getDatabaseType()) {
			case MySQL: 
				strSQL = "SELECT NOW() AS DB_DATETIME";	
				break;
			case MicrosoftSQL: 
				strSQL = "SELECT CONVERT(VARCHAR, GETDATE(), 120) AS DB_DATETIME";
				break;     
			case Oracle:
				strSQL = "SELECT TO_CHAR(CURRENT_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS') AS DB_DATETIME FROM DUAL";
				break;
			case PostgreSQL: 
				strSQL = "SELECT TO_CHAR(CURRENT_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS') AS DB_DATETIME FROM DUAL";
				break;
			case MicrosoftAccess:
				strSQL = "SELECT CONVERT(VARCHAR, GETDATE(), 120) AS DB_DATETIME";
				break;
			default:	 
				System.err.println("Invalid Database Type selected\n");
				break;
			} 
			
			st = MBankingDB.getConnection().createStatement();
			rs = st.executeQuery(strSQL);
			rs.next();
			
			strDateTime = rs.getString("DB_DATETIME").substring(0,19);
			//System.out.println("strDateTime: " + strDateTime);
		}
		catch (Exception e)
		{
			System.err.println ("Error message: " + e.getMessage ());
		}
		finally
		{
			try{rs.close();}catch(Exception e){}
			try{st.close();}catch(Exception e){}
			
			rs = null;
			st = null;
			strSQL = null;
		}
		return strDateTime;
	}
	
	public static String getDBTimeStamp()
	{
		Statement st = null;
		ResultSet rs = null;
		String strDateTime=null;
		String strSQL = null;
		try
		{
			switch (MBankingLocalParameters.getDatabaseType()) {
			case MySQL: 
				strSQL = "SELECT DATE_FORMAT(NOW(),'%Y%m%d%H%i%s') AS DB_DATETIME";	
				break;
			case MicrosoftSQL: 
				strSQL ="DECLARE @today DATETIME = SYSDATETIME(); " +
						"SELECT CONVERT(VARCHAR,@today,112) + REPLACE(CONVERT(VARCHAR, @today, 108),':','')  AS DB_DATETIME;";
				break;     
			case Oracle:
				strSQL = "SELECT TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDDHH24MISS') AS DB_DATETIME FROM DUAL";
				break;
			case PostgreSQL: 
				strSQL = "SELECT TO_CHAR(CURRENT_TIMESTAMP, 'YYYYMMDDHH24MISS') AS DB_DATETIME FROM DUAL";
				break;
			case MicrosoftAccess:
				System.err.println("Microsoft Access Database Type is not yet supported\n");
				break;
			default:	 
				System.err.println("Invalid Database Type selected\n");
				break;
			} 
			
			st = MBankingDB.getConnection().createStatement();
			rs = st.executeQuery(strSQL);
			if(rs.next()){
				strDateTime = rs.getString("DB_DATETIME");
			}
			
		}
		catch (Exception e)
		{
			System.err.println ("Error message 123: " + e.getMessage ());
		}
		finally
		{
			try{rs.close();}catch(Exception e){}
			try{st.close();}catch(Exception e){}
			
			rs = null;
			st = null;
			strSQL = null;
		}
		return strDateTime;
	}

	public static String getPESABalance(PESAConstants.PESAType thePESAType) {
		String rVal = "";
		NamedParameterStatement p = null;
		ResultSet rs = null;
		String strSQL = null;
		String strPESATableName = "pesa_log";
		if (thePESAType.equals(PESAConstants.PESAType.PESA_OUT)) {
            strPESATableName = PESALocalParameters.get_PESA_OUT_TableName();
        } else if (thePESAType.equals(PESAConstants.PESAType.PESA_IN)) {
            strPESATableName = PESALocalParameters.get_PESA_IN_TableName();
        }

		try {
			strSQL = "SELECT top 1 * FROM " + strPESATableName + " WHERE pesa_type = :pesa_type AND pesa_status_code = 105 AND pesa_category = 'MPESA_WITHDRAWAL' ORDER BY date_created DESC;";
			p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
			p.setString("pesa_type", thePESAType.getValue());
			rs = p.executeQuery();
			if (rs.next()) {
				rVal = rs.getString("pesa_result_xml_data");
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


	public static String getMinimumLowFloatValue() {
		String rVal = "";
		NamedParameterStatement p = null;
		ResultSet rs = null;
		String strSQL = null;
		try {
			strSQL = "SELECT top 1 * FROM client_parameters WHERE parameters_id = 2;";
			p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
			rs = p.executeQuery();
			if (rs.next()) {
				rVal = rs.getString("client_xml_parameters");
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

	public static void fnProfileCallsToCBS(String theFunctionName, Duration theTTL, String theEndpoint){
		try {
			/*Duration finalTheTTL = theTTL;
			String finalTheFunctionName = theFunctionName;

			new Thread(() -> {
				String strIPAndPort = "";
				NavisionLocalParams params = NavisionUtils.getNavisionLocalParameters();
				String strWS_URL = Objects.requireNonNull(params).getCoreBankingUrl();
				Pattern pattern = Pattern.compile("^(?:http(s)?://)?[\\w.-]+(?:\\.[\\w.-]+)+:\\d*");
				Matcher matcher = pattern.matcher(strWS_URL);
				while (matcher.find()) {
					strIPAndPort = matcher.group(0);
				}

				System.out.println("API Call to NAV Latency: "+ finalTheTTL.toMillis());

				String rVal = "";
				NamedParameterStatement p = null;
				String strSQL = null;
				String strTableName = "cbs_analytics";

				String strCBSAddress = "";
				String strMiddlewareAddress = "";

				try {
					strSQL = "INSERT INTO " + strTableName + " (function_name, latency, cbs_address, middleware_address, endpoint) VALUES (:function_name, :latency, :cbs_address, :middleware_address, :endpoint);";
					p = new NamedParameterStatement(MBankingDB.getConnection(), strSQL);
					p.setString("function_name", finalTheFunctionName);
					p.setInt("latency", Math.toIntExact(finalTheTTL.toMillis()));
					p.setString("cbs_address", strIPAndPort);
					p.setString("middleware_address", String.valueOf(InetAddress.getLocalHost()));
					p.setString("endpoint", theEndpoint);
					p.execute();

					p.close();
					p = null;
					strSQL = null;
				} catch (Exception e) {
					System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
				} finally {
					strSQL = null;
					if (p != null) {
						try {
							p.close();
						} catch (Exception ignored) {
						}
					}

					p = null;
					strSQL = null;
				}
			}).start();*/
		} catch (Exception e){
			System.err.println(USSDAPI.class.getSimpleName() + "." + new Object() {}.getClass().getEnclosingMethod().getName() + "() ERROR : " + e.getMessage());
		} finally {
			theFunctionName = null;
			theTTL = null;
		}
		System.gc();
		Runtime.getRuntime().gc();
	}
}