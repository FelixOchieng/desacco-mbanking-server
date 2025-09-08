package ke.skyworld.mbanking.msgapi;
//import java.io.*;

import ke.skyworld.lib.mbanking.utils.Crypto;
import ke.skyworld.lib.mbanking.utils.NamedParameterStatement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.sql.*;

import static ke.skyworld.mbanking.nav.NavisionUtils.format;

public  class MSGAPIDB
{
	public static void fixMSGs(MsgLogDB msgLogDB) {
		if(msgLogDB != null){
			Connection connection = msgLogDB.getConnection();
			String rVal = "";
			NamedParameterStatement p = null;
			ResultSet rs = null;
			String strSQL = null;
			String strMSGTableName = msgLogDB.getTable();;

			try {
				strSQL = "SELECT * FROM " + strMSGTableName + " WHERE ((msg_status_code = :msg_status_code AND msg_mode = :msg_mode) OR (msg_status_code = :msg_status_code AND msg = :msg) OR (msg_status_code = :msg_status_code AND sender = :sender)) OR (msg = :msg AND msg_status_code != 104)";
				p = new NamedParameterStatement(connection, strSQL);
				p.setInt("msg_status_code", 10);
				p.setString("msg_mode", "");
				p.setString("msg", "");
				p.setString("sender", "");
				rs = p.executeQuery();
				while (rs.next()) {
					//rVal = rs.getString("");
					String strOriginatorId = rs.getString("originator_id");
					System.out.println("strOriginatorId: "+strOriginatorId);

					strSQL = "UPDATE " + strMSGTableName + " SET msg_status_code = :msg_status_code, msg_mode = :msg_mode, msg_status_description = :msg_status_description WHERE originator_id = :originator_id";
					p = new NamedParameterStatement(connection, strSQL);
					p.setInt("msg_status_code", 104);
					p.setString("msg_status_description", "FAILED");
					p.setString("originator_id", strOriginatorId);
					p.setString("msg_mode", "SAF");
					p.execute();
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
				connection = null;
			}
		} else {
			System.err.println("Could not establish a connection to MSG Logs Table");
		}
	}

	public static MsgLogDB LogsDBConnect() {
		Connection connMSGLogsDB;
		MsgLogDB rVal = new MsgLogDB();
		try {
			connMSGLogsDB = null;

			rVal = getLogMSGDBParameters();

			if(rVal != null){
				try {
					DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());

					System.out.println("MSG Logs Database Driver found");
					connMSGLogsDB = DriverManager.getConnection(rVal.getUrl(), rVal.getUsername(), rVal.getPassword());
					connMSGLogsDB.setAutoCommit(true);
					System.out.println(rVal.getUrl() + "\nMSG Logs Database connection established");
				} catch (Exception var8) {
					var8.printStackTrace();
					System.err.println("Cannot connect to the Logs database server");
					System.err.println("Error message: " + var8.getMessage());
				}

				rVal.setConnection(connMSGLogsDB);

				return rVal;
			}
		} catch (Exception e){
			e.printStackTrace();
		} finally {
			connMSGLogsDB = null;
		}
		return null;
	}

	public static MsgLogDB getLogMSGDBParameters() {
		BufferedReader bufferedReader;
		StringBuilder stringBuilder;
		try {
			String strFilePath = System.getProperty("user.dir")+ File.separator+ "msg_log_conf.xml";

			bufferedReader = new BufferedReader(new FileReader(new File(strFilePath)));
			String strLine;
			stringBuilder = new StringBuilder();

			while((strLine=bufferedReader.readLine())!= null){
				stringBuilder.append(strLine.trim());
			}

			String strConfig = stringBuilder.toString();

			InputSource source = new InputSource(new StringReader(strConfig));
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document xmlDocument = builder.parse(source);
			XPath configXPath = XPathFactory.newInstance().newXPath();

			String strURL = configXPath.evaluate("/CONFIG/DATABASE/URL", xmlDocument, XPathConstants.STRING).toString();
			String strDriver = configXPath.evaluate("/CONFIG/DATABASE/DRIVER", xmlDocument, XPathConstants.STRING).toString();
			String strSchema = configXPath.evaluate("/CONFIG/DATABASE/SCHEMA", xmlDocument, XPathConstants.STRING).toString();
			String strTable = configXPath.evaluate("/CONFIG/DATABASE/TABLE", xmlDocument, XPathConstants.STRING).toString();
			String strUsername = configXPath.evaluate("/CONFIG/DATABASE/USERNAME", xmlDocument, XPathConstants.STRING).toString();
			String strPassword = configXPath.evaluate("/CONFIG/DATABASE/PASSWORD", xmlDocument, XPathConstants.STRING).toString();
			String strPasswordType = configXPath.evaluate("/CONFIG/DATABASE/PASSWORD/@TYPE", xmlDocument, XPathConstants.STRING).toString();

			String strEncryptionKey = "Vx@3GhTu*7nbHJg^)SYTDhs>pij?2H";

			if(strPasswordType.equalsIgnoreCase("CLEARTEXT")){
				// Get the root element
				NodeList nlCoreBanking= xmlDocument.getFirstChild().getChildNodes().item(0).getChildNodes();
				Node ndPassword = nlCoreBanking.item(5);

				Crypto crypto = new Crypto();
				String strEncryptedPassword = crypto.encrypt(strEncryptionKey, strPassword);
				ndPassword.setTextContent(strEncryptedPassword);
				ndPassword.getAttributes().getNamedItem("TYPE").setTextContent("ENCRYPTED");

				// write the content into xml file
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();

				DOMSource dOMSource = new DOMSource(format(xmlDocument));
				StreamResult result = new StreamResult(new File(strFilePath));
				transformer.transform(dOMSource, result);
			} else if(strPasswordType.equalsIgnoreCase("ENCRYPTED")){
				strPassword = new Crypto().decrypt(strEncryptionKey, strPassword);
			} else {
				System.err.println("MSGAPIDB.getLogMSGDBParameters() Error. Unknown password type");
				return null;
			}

			MsgLogDB localParams = new MsgLogDB();
			localParams.setUrl(strURL);
			localParams.setUsername(strUsername);
			localParams.setPassword(strPassword);
			localParams.setDriver(strDriver);
			localParams.setSchema(strSchema);
			localParams.setTable(strTable);

			return localParams;

		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("MSGAPIDB.getLogMSGDBParameters() Error. "+e.getMessage());
		}

		return null;
	}

	public static class MsgLogDB {
		String url;
		String driver;
		String schema;
		String table;
		String username;
		String password;
		String password_type;
		Connection connection;

		public MsgLogDB(){}

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getDriver() {
			return driver;
		}

		public void setDriver(String driver) {
			this.driver = driver;
		}

		public String getSchema() {
			return schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public String getTable() {
			return table;
		}

		public void setTable(String table) {
			this.table = table;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getPassword_type() {
			return password_type;
		}

		public void setPassword_type(String password_type) {
			this.password_type = password_type;
		}

		public Connection getConnection() {
			return connection;
		}

		public void setConnection(Connection connection) {
			this.connection = connection;
		}
	}
}

