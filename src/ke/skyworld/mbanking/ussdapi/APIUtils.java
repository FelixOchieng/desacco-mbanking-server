package ke.skyworld.mbanking.ussdapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.xml.bind.DatatypeConverter;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.query_manager.query.FilterPredicate;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.skyworld.lib.mbanking.core.MBankingConstants;
import ke.skyworld.lib.mbanking.core.MBankingUtils;
import ke.skyworld.lib.mbanking.core.MBankingXMLFactory;
import ke.skyworld.lib.mbanking.mapp.MAPPRequest;
import ke.skyworld.lib.mbanking.msg.MSGConstants;
import ke.skyworld.lib.mbanking.msg.MSGProcessor;
import ke.skyworld.lib.mbanking.pesa.PESALocalParameters;
import ke.skyworld.lib.mbanking.register.MemberRegisterResponse;
import ke.skyworld.lib.mbanking.register.RegisterConstants;
import ke.skyworld.lib.mbanking.register.RegisterProcessor;
import ke.skyworld.lib.mbanking.ussd.USSDRequest;
import ke.skyworld.lib.mbanking.utils.Crypto;
import ke.skyworld.lib.mbanking.utils.InMemoryCache;
import ke.skyworld.mbanking.mappapi.MAPPAPIDB;
import ke.skyworld.mbanking.mappapi.MAPPAPI;
import ke.skyworld.mbanking.mbankingapi.MBankingAPI;
import ke.skyworld.mbanking.nav.cbs.CBSAPI;
import ke.skyworld.mbanking.nav.cbs.CBSAgencyAPI;
import ke.skyworld.mbanking.pesaapi.APIConstants;
import ke.skyworld.mbanking.ussdapplication.AppConstants;
import ke.skyworld.sp.manager.SPManager;
import ke.skyworld.sp.manager.SPManagerConstants;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_CUSTOMER_REGISTER_SIGNATORIES;
import static ke.skyworld.mbanking.ussdapplication.AppConstants.HONORIFICS;


public class APIUtils {
    public APIUtils(){
    }

    public final static long ONE_SECOND = 1000;
    public final static long SECONDS = 60;

    public final static long ONE_MINUTE = ONE_SECOND * 60;
    public final static long MINUTES = 60;

    public final static long ONE_HOUR = ONE_MINUTE * 60;
    public final static long HOURS = 24;

    public final static long ONE_DAY = ONE_HOUR * 24;

    public static String ENCRYPTION_KEY = "6l04zjBa*iuGSv6l(2akwfqA";
    public static String TEMPORARY_DATA_ENCRYPTION_KEY = "kbXxjOAEzvcSa5Wo4e4qWFymeAZdAoaZ";

    public static Object toHashMap(String objStr, TypeReference T){
        ObjectMapper objectMapper = new ObjectMapper();
        Map map = new HashMap();
        try {
            map = objectMapper.readValue(objStr, T);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static String serialize(Object obj){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return "";
    }


    public static HashMap<String, String[]> getXmlStringV2(String strLoansXML){

        HashMap<String, String[]> loans = new HashMap<>();

        try{
            InputSource source = new InputSource(new StringReader(strLoansXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            NodeList nlLoans = ((NodeList) configXPath
                    .evaluate("/Loans", xmlDocument, XPathConstants.NODESET))
                    .item(0).getChildNodes();

            for (int i = 0; i < nlLoans.getLength(); i++) {
                NodeList nlLoan = ((NodeList) configXPath
                        .evaluate("Product", nlLoans, XPathConstants.NODESET))
                        .item(i).getChildNodes();

                loans.put(nlLoan.item(2).getTextContent(),
                        new String[]{
                                nlLoan.item(0).getTextContent(),
                                nlLoan.item(1).getTextContent(),
                                nlLoan.item(3).getTextContent()
                        });
            }
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException e) {
            e.printStackTrace();
        }
        return loans;
    }

    /*public static String sanitizePhoneNumber(String thePhoneNumber){
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
    }*/

    public static String sanitizePhoneNumber(String thePhoneNumber){
        thePhoneNumber = thePhoneNumber.replaceAll("\\s","");
        thePhoneNumber = thePhoneNumber.replaceFirst("^\\+", "");
        try {
            if(thePhoneNumber.startsWith("+")){
                thePhoneNumber = thePhoneNumber.replaceFirst("^\\+", "");
            }

            if(thePhoneNumber.matches("^2547\\d{8}$") || thePhoneNumber.matches("^2541\\d{8}$")){
                return thePhoneNumber;
            }

            if(thePhoneNumber.matches("^07\\d{8}$") || thePhoneNumber.matches("^01\\d{8}$")) {
                return thePhoneNumber.replaceFirst("^0", "254");
            }

            if(thePhoneNumber.matches("^7\\d{8}$") || thePhoneNumber.matches("^1\\d{8}$")){
                return "254"+thePhoneNumber;
            }

            if(thePhoneNumber.matches("^25407\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25407", "2547");
            }

            if(thePhoneNumber.matches("^25401\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25401", "2541");
            }

            if(thePhoneNumber.matches("^254\\+254\\d{9}$")){
                return thePhoneNumber.replaceFirst("^254\\+254", "254");
            }

            if(thePhoneNumber.matches("^254254\\d{9}$")){
                return thePhoneNumber.replaceFirst("^254254", "254");
            }

            if(thePhoneNumber.matches("^254\\+25401\\d{8}$")){
                return thePhoneNumber.replaceFirst("^254\\+25401", "2541");
            }

            if(thePhoneNumber.matches("^254\\+25407\\d{8}$")){
                return thePhoneNumber.replaceFirst("^254\\+25407", "2547");
            }

            if(thePhoneNumber.matches("^25425401\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25425401", "2541");
            }

            if(thePhoneNumber.matches("^25425407\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25425407", "2547");
            }
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

            if(thePhoneNumber.matches("^25407\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25407", "2547");
            }

            if(thePhoneNumber.matches("^2541\\d{8}$")){
                return thePhoneNumber;
            }

            if(thePhoneNumber.matches("^01\\d{8}$")) {
                return thePhoneNumber.replaceFirst("^0", "254");
            }

            if(thePhoneNumber.matches("^1\\d{8}$")){
                return "254"+thePhoneNumber;
            }

            if(thePhoneNumber.matches("^25401\\d{8}$")){
                return thePhoneNumber.replaceFirst("^25401", "2541");
            }

            return "INVALID_MOBILE_NUMBER";
        }catch (Exception e){
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static String hashAgentPIN(String thePIN, String theUsername) {
        Crypto crypto = new Crypto();
        try {
            String strSalt = "kWr0v6grHgTkdP2BoUeRtvUeeHKRstsO8g2Y3oTioUZOzj4ll4d0j9T8dKPKtgoE";
            String strClearText = theUsername+strSalt+thePIN;
            String strHashed = crypto.hash("SHA-256", strClearText);
            strHashed = strHashed.toLowerCase();
            return strHashed;
        } catch (Exception e) {
            System.err.println("APIUtils.hashPIN(): ERROR " + e.getMessage());
        } finally {
            crypto = null;
        }
        return thePIN;
    }

    public static String hashPIN(String thePIN, String theUsername) {
        Crypto crypto = new Crypto();
        theUsername = sanitizePhoneNumber(theUsername);
        theUsername = "+"+theUsername;
        try {
            String strSalt = "tqkjOEbd2mPv5fBE5JgwnFgGNWafpLSaXXtkDsfdyGUUmSJb43VPr5I3zQ8YwmEV";
            String strClearText = theUsername+strSalt+thePIN;
            String strHashed = crypto.hash("SHA-256", strClearText);
            strHashed = strHashed.toLowerCase();

            return strHashed.substring(0,50);
        } catch (Exception e) {
            System.err.println("APIUtils.hashPIN(): ERROR " + e.getMessage());
        } finally {
            crypto = null;
        }
        return thePIN.substring(0, 50);
    }

    public static String hashOTP(String theCleartext) {
        Crypto crypto = new Crypto();
        try {
            String strSalt = "dpOt5G2WhNbdBsELXfaAYV7XEoaYgZ3CAV3wLhOOzClv9zXTs3vfhRcxm31s6DKT";
            String strClearText = theCleartext+strSalt+theCleartext;
            String strHashed = crypto.hash("MD5", strClearText);
            strHashed = strHashed.toLowerCase();
            return strHashed;
        } catch (Exception e) {
            System.err.println("APIUtils.hashPIN(): ERROR " + e.getMessage());
        } finally {
            crypto = null;
        }
        return theCleartext;
    }


    public static String shortenMemberName(String theMobileNmber) {
        String strNameFromNAV = CBSAPI.getMemberName(theMobileNmber);
        String strName = strNameFromNAV.split(" ")[0];
        //String strName = strNameFromNAV;
        String strNameForHonorificCheck = strName.toUpperCase();
        if(Arrays.asList(HONORIFICS).contains(strNameForHonorificCheck) || Arrays.asList(HONORIFICS).contains(strNameForHonorificCheck.replaceAll("\\.", ""))){
            try {
                strName = strNameFromNAV.split(" ")[1];
                //strName = strNameFromNAV;
            } catch (Exception e){
                strName = strNameFromNAV.split(" ")[0];
                //strName = strNameFromNAV;
                e.printStackTrace();
            }
        }

        strName = APIUtils.convertToTitleCaseIteratingChars(strName);
        return strName;
    }

    public static String shortenMemberNameProvidedName(String strNameFromNAV) {
        String strName = strNameFromNAV.split(" ")[0];
        //String strName = strNameFromNAV;
        String strNameForHonorificCheck = strName.toUpperCase();
        if(Arrays.asList(HONORIFICS).contains(strNameForHonorificCheck) || Arrays.asList(HONORIFICS).contains(strNameForHonorificCheck.replaceAll("\\.", ""))){
            try {
                strName = strNameFromNAV.split(" ")[1];
                //strName = strNameFromNAV;
            } catch (Exception e){
                strName = strNameFromNAV.split(" ")[0];
                //strName = strNameFromNAV;
                e.printStackTrace();
            }
        }

        strName = APIUtils.convertToTitleCaseIteratingChars(strName);
        return strName;
    }

    public static void hashPINsOnNAV() {
        try {
            //System.out.println("hashPINsOnNAV started");
            String strClearTextPINXML = CBSAPI.getUnhashedPINs();

            //System.out.println(strClearTextPINXML);

            if (!strClearTextPINXML.equals("ERROR")) {
                InputSource source = new InputSource(new StringReader(strClearTextPINXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList ndAccounts = ((NodeList) configXPath.evaluate("/ACCOUNTS", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                double lnStartTime = (double) System.currentTimeMillis();
                for (int i = 0; i < ndAccounts.getLength(); i++) {
                    String strPhoneNumber = ndAccounts.item(i).getAttributes().getNamedItem("PHONE_NUMBER").getTextContent();
                    String strAccountNumber = ndAccounts.item(i).getAttributes().getNamedItem("ACCOUNT_NUMBER").getTextContent();
                    String strPIN = ndAccounts.item(i).getAttributes().getNamedItem("PIN").getTextContent();

                    strPhoneNumber = sanitizePhoneNumber(strPhoneNumber);
                    strPhoneNumber = "+"+strPhoneNumber;

                    //System.out.println("PIN Hash Count: " + (i + 1));
                    //System.out.println("Account Number: " + strAccountNumber);
                    //System.out.println("Phone Number: " + strPhoneNumber);
                    //System.out.println("Cleartext PIN: " + strPIN);

                    String strHashedPIN = hashPIN(strPIN, strPhoneNumber);
                    //System.out.println("Hashed PIN: " + strHashedPIN);

                    String strResult = CBSAPI.setHashedPIN(strAccountNumber, strPhoneNumber, strHashedPIN);
                    //System.out.println("RESULT: " + strResult + "\n");
                }
                double lnEndTime = (double) System.currentTimeMillis();
                double lnTimeTaken = (lnEndTime - lnStartTime) / 1000;
                //System.out.println("Finished Task In " + lnTimeTaken + " Seconds");
            }
        } catch (Exception e) {
            System.err.println("USSDAPI.hashPINsOnNAV() ERROR : " + e.getMessage());
            //hashPINsOnNAV();
        } finally {
        }
    }

    public static void hashAgencyPINsOnNAV() {
        try {
            //System.out.println("hashPINsOnNAV started");
            String strClearTextPINXML = CBSAgencyAPI.getUnhashedPINs();

            //System.out.println(strClearTextPINXML);

            if (!strClearTextPINXML.equals("ERROR")) {
                InputSource source = new InputSource(new StringReader(strClearTextPINXML));
                DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = builderFactory.newDocumentBuilder();
                Document xmlDocument = builder.parse(source);
                XPath configXPath = XPathFactory.newInstance().newXPath();

                NodeList ndAccounts = ((NodeList) configXPath.evaluate("/ACCOUNTS", xmlDocument, XPathConstants.NODESET)).item(0).getChildNodes();

                double lnStartTime = (double) System.currentTimeMillis();
                for (int i = 0; i < ndAccounts.getLength(); i++) {
                    String strUsername = ndAccounts.item(i).getAttributes().getNamedItem("USERNAME").getTextContent();
                    String strAgentCode = ndAccounts.item(i).getAttributes().getNamedItem("AGENT_CODE").getTextContent();
                    String strPIN = ndAccounts.item(i).getAttributes().getNamedItem("PASSWORD").getTextContent();

                    //System.out.println("PIN Hash Count: " + (i + 1));
                    //System.out.println("Account Number: " + strAccountNumber);
                    //System.out.println("Phone Number: " + strPhoneNumber);
                    //System.out.println("Cleartext PIN: " + strPIN);

                    String strHashedPIN = hashAgentPIN(strPIN, strUsername);
                    //System.out.println("Hashed PIN: " + strHashedPIN);

                    String strResult = CBSAgencyAPI.setHashedPIN(strUsername, strAgentCode, strHashedPIN);
                    //System.out.println("RESULT: " + strResult + "\n");
                }
                double lnEndTime = (double) System.currentTimeMillis();
                double lnTimeTaken = (lnEndTime - lnStartTime) / 1000;
                //System.out.println("Finished Task In " + lnTimeTaken + " Seconds");
            }
        } catch (Exception e) {
            System.err.println("USSDAPI.hashAgencyPINsOnNAV() ERROR : " + e.getMessage());
            //hashPINsOnNAV();
        } finally {
        }
    }


    public static String millisToLongDHMS(long duration) {
        StringBuffer res = new StringBuffer();
        long temp = 0;
        boolean hasDay = false;
        boolean hasHasHour = false;
        boolean hasMinute = false;
        if (duration >= ONE_SECOND) {
            temp = duration / ONE_DAY;
            if (temp > 0) {
                hasDay = true;
                duration -= temp * ONE_DAY;
                res.append(temp).append(" day").append(temp > 1 ? "s" : "")
                        .append(duration >= ONE_MINUTE ? ", " : "");
            }

            temp = duration / ONE_HOUR;
            if (temp > 0) {
                hasHasHour = true;
                duration -= temp * ONE_HOUR;
                res.append(temp).append(" hour").append(temp > 1 ? "s" : "")
                        .append(duration >= ONE_MINUTE ? ", " : "");
            }

            if(!hasDay){
                temp = duration / ONE_MINUTE;
                if (temp > 0) {
                    hasMinute = true;
                    duration -= temp * ONE_MINUTE;
                    res.append(temp).append(" minute").append(temp > 1 ? "s" : "");
                }
            }

            /*if (!res.toString().equals("") && duration >= ONE_SECOND) {
                res.append(" and ");
            }

            temp = duration / ONE_SECOND;
            if (temp > 0) {
                res.append(temp).append(" second").append(temp > 1 ? "s" : "");
            }*/
            return res.toString();
        } else {
            return "0 second";
        }
    }

    public static Date convertDateStringToDate(String date) {
        SimpleDateFormat simpleDateFormat =
                new SimpleDateFormat(SPManagerConstants.DEFAULT_DATE_TIME_FORMAT);
        try {
            return (date == null) ? null : simpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static String getPrettyDateTimeDifference(Date startDate, Date endDate) {
        String seconds = "second";
        String minutes = "minute";
        String hours = "hour";
        String days = "day";
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;
        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;
        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;
        long elapsedSeconds = different / secondsInMilli;
        if (elapsedSeconds > 1) seconds = seconds + "s";
        if (elapsedMinutes > 1) minutes = minutes + "s";
        if (elapsedHours > 1) hours = hours + "s";
        if (elapsedDays > 1) days = days + "s";
        if (elapsedDays <= 0) {
            if (elapsedHours <= 0) {
                if (elapsedMinutes <= 0) {
                    if (elapsedSeconds <= 0) {
                        return "3 seconds";
                    } else {
                        return String.format("%d " + seconds + "%n", elapsedSeconds);
                    }
                } else {
                    if (elapsedSeconds > 0) {
                        return String.format("%d " + minutes + ", %d " + seconds + "%n",
                                elapsedMinutes, elapsedSeconds);
                    } else {
                        return String.format("%d " + minutes + "%n", elapsedMinutes);
                    }
                }
            } else {
                if (elapsedMinutes > 0) {
                    return String.format("%d " + hours + ", %d " + minutes + "%n",
                            elapsedHours, elapsedMinutes);
                } else {
                    return String.format("%d " + hours + "%n", elapsedHours);
                }
            }
        } else {
            if (elapsedHours > 0) {
                return String.format("%d " + days + ", %d " + hours + "%n",
                        elapsedDays, elapsedHours);
            } else {
                return String.format("%d " + days + "%n", elapsedDays);
            }
        }
    }


    public static String getPrettyDateTimeDifferenceRoundedUp(Date startDate, Date endDate) {
        //milliseconds
        long different = endDate.getTime() - startDate.getTime();
        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;
        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;
        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;
        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;
        long elapsedSeconds = different / secondsInMilli;
        if (elapsedDays > 0) {
            if (elapsedHours > 0) {
                elapsedDays += 1;
            }
            String days = (elapsedDays == 1) ? "DAY" : "DAYS";
            return String.format("%d " + days + "%n", elapsedDays);
        } else {
            //Days 0. Do for hours
            if (elapsedHours > 0) {
                if (elapsedMinutes > 0) {
                    elapsedHours += 1;
                }
                String hours = (elapsedHours == 1) ? "HOUR" : "HOURS";
                return String.format("%d " + hours + "%n", elapsedHours);
            } else {
                //Hours 0. Do for minutes
                if (elapsedMinutes > 0) {
                    if (elapsedSeconds > 0) {
                        elapsedMinutes += 1;
                    }
                    String minutes = (elapsedMinutes == 1) ? "MINUTE" : "MINUTES";
                    return String.format("%d " + minutes + "%n", elapsedMinutes);
                } else {
                    return "1 MINUTE";
                }
            }
        }
    }

    public static String titleCase(String inputString) {
        if (StringUtils.isBlank(inputString)) {
            return "";
        }

        if (StringUtils.length(inputString) == 1) {
            return inputString.toUpperCase();
        }

        StringBuffer resultPlaceHolder = new StringBuffer(inputString.length());

        Stream.of(inputString.split(" ")).forEach(stringPart ->
        {
            if (stringPart.length() > 1)
                resultPlaceHolder.append(stringPart.substring(0, 1)
                        .toUpperCase())
                        .append(stringPart.substring(1)
                                .toLowerCase());
            else
                resultPlaceHolder.append(stringPart.toUpperCase());

            resultPlaceHolder.append(" ");
        });
        return StringUtils.trim(resultPlaceHolder.toString());
    }

    public static String fnModifyMAPPSessionIDBkp(String theSessionID) {
        try {
            ZonedDateTime nowZoned = ZonedDateTime.now();
            Instant midnight = nowZoned.toLocalDate().atStartOfDay(nowZoned.getZone()).toInstant();
            Duration duration = Duration.between(midnight, Instant.now());
            long seconds = duration.getSeconds();
            return theSessionID + "_" + String.format("%05d", Integer.parseInt(String.valueOf(seconds)));
        } catch (Exception e) {
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return theSessionID;
    }

    public static boolean fnCreateFileFromBase64(String theBase64Data, String theImagePath){
        boolean rVal = false;
        try {
            byte[] data = DatatypeConverter.parseBase64Binary(theBase64Data);

            File file = new File(theImagePath);
            try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
                outputStream.write(data);
                rVal = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return rVal;
    }

    public static String fnModifyMAPPSessionID(MAPPRequest theMAPPRequest) {
        return MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.MAPP,theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());
    }

    public static String fnModifyAGNTSessionID(MAPPRequest theMAPPRequest) {
        return MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.AGENCY, theMAPPRequest.getSessionID(), theMAPPRequest.getSequence());
    }

    public static String fnModifyUSSDSessionID(USSDRequest theUSSDRequest) {
        return MBankingUtils.generateTransactionIDFromSession(MBankingConstants.AppTransID.USSD,theUSSDRequest.getUSSDSessionID(), theUSSDRequest.getSequence());
    }

    public static int fnSendSMS(String theReceiver, String theMessage, String theCharge, MSGConstants.MSGMode theMode, int thePriority, String theCategory, String theRequestApplication, String theSourceApplication, String theSessionID, String theCorrelationID){
        try {
            String strProductID = MBankingAPI.getValueFromLocalParams(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/PRODUCT_ID");
            long lnProductID = Long.parseLong(strProductID);
            String strSender = MBankingAPI.getValueFromLocalParams(APIConstants.APPLICATION_TYPE.MSG, "OTHER_DETAILS/CUSTOM_PARAMETERS/SMS/MT/SENDER");
            String strCommand = "BulkSMS";
            MSGConstants.Sensitivity theSensitivity = MSGConstants.Sensitivity.PERSONAL;


            Thread worker = new Thread(() -> {
                MSGProcessor.sendMSG(
                        lnProductID,
                        strSender,
                        theReceiver,
                        theMessage,
                        strCommand,
                        theSensitivity,
                        theCategory,
                        thePriority,
                        theCharge,
                        theMode,
                        theRequestApplication,
                        theCorrelationID,
                        theSourceApplication,
                        theSessionID
                );
            });
            worker.start();
            return 1;
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return 0;
    }

    public static class OTP {
        private int length;
        private int ttl;
        private String id;
        private String value;
        private boolean enabled;

        public OTP(int length, int ttl, String id, String value, boolean enabled) {
            this.length = length;
            this.ttl = ttl;
            this.value = value;
            this.id = id;
            this.enabled = enabled;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public static class MenuItem {
        private String title;
        private String value;

        public MenuItem(String title, String value) {
            this.title = title;
            this.value = value;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }


    public static class ServiceProviderAccount {
        private String strProviderCode;
        private String strProviderAccountCode;
        private String strProviderAccountName;
        private String strProviderAccountType;
        private String strProviderAccountIdentifier;
        private String strProviderAccountLongTag;
        private String strProviderBranchCode;
        private String dblMinTransactionAmount;
        private String dblMaxTransactionAmount;

        public ServiceProviderAccount(String theProviderCode, String theProviderAccountCode, String theProviderAccountName, String theProviderAccountType, String theProviderAccountIdentifier, String theProviderAccountLongTag, String theProviderBranchCode, String theMinTransactionAmount, String theMaxTransactionAmount) {
            this.strProviderCode = theProviderCode;
            this.strProviderAccountCode = theProviderAccountCode;
            this.strProviderAccountName = theProviderAccountName;
            this.strProviderAccountType = theProviderAccountType;
            this.strProviderAccountIdentifier = theProviderAccountIdentifier;
            this.strProviderAccountLongTag = theProviderAccountLongTag;
            this.strProviderBranchCode = theProviderBranchCode;
            this.dblMinTransactionAmount = theMinTransactionAmount;
            this.dblMaxTransactionAmount = theMaxTransactionAmount;
        }

        public String getProviderCode() {
            return strProviderCode;
        }

        public void setProviderCode(String strProviderCode) {
            this.strProviderCode = strProviderCode;
        }

        public String getProviderAccountCode() {
            return strProviderAccountCode;
        }

        public void setProviderAccountCode(String strProviderAccountCode) {
            this.strProviderAccountCode = strProviderAccountCode;
        }

        public String getProviderAccountName() {
            return strProviderAccountName;
        }

        public void setProviderAccountName(String strProviderAccountName) {
            this.strProviderAccountName = strProviderAccountName;
        }

        public String getProviderAccountType() {
            return strProviderAccountType;
        }

        public void setProviderAccountType(String strProviderAccountType) {
            this.strProviderAccountType = strProviderAccountType;
        }

        public String getProviderAccountIdentifier() {
            return strProviderAccountIdentifier;
        }

        public void setProviderAccountIdentifier(String strProviderAccountIdentifier) {
            this.strProviderAccountIdentifier = strProviderAccountIdentifier;
        }

        public String getProviderAccountLongTag() {
            return strProviderAccountLongTag;
        }

        public void setProviderAccountLongTag(String strProviderAccountLongTag) {
            this.strProviderAccountLongTag = strProviderAccountLongTag;
        }

        public String getProviderBranchCode() {
            return strProviderBranchCode;
        }

        public void setProviderBranchCode(String strProviderBranchCode) {
            this.strProviderBranchCode = strProviderBranchCode;
        }

        public String getMinTransactionAmount() {
            return dblMinTransactionAmount;
        }

        public void setMinTransactionAmount(String dblMinTransactionAmount) {
            this.dblMinTransactionAmount = dblMinTransactionAmount;
        }

        public String getMaxTransactionAmount() {
            return dblMaxTransactionAmount;
        }

        public void setMaxTransactionAmount(String dblMaxTransactionAmount) {
            this.dblMaxTransactionAmount = dblMaxTransactionAmount;
        }
    }

    public static LinkedList<ServiceProviderAccount> getSPAccounts(String theProviderAccountType){
        LinkedList<ServiceProviderAccount> rVal = new LinkedList<ServiceProviderAccount>();
        MAPPAPI mappapi = new MAPPAPI();
        SPManager spManager;
        try {
            String strIntegritySecret = PESALocalParameters.getIntegritySecret();
            spManager = new SPManager(strIntegritySecret);
            LinkedList<LinkedHashMap<String, String>> llHsB2CAccounts = spManager.getB2BCapabilitySPAccounts();
            for (LinkedHashMap<String, String> lhsB2CAccount : llHsB2CAccounts) {
                String strProviderCode = lhsB2CAccount.get("provider_code");
                String strProviderAccountCode = lhsB2CAccount.get("provider_account_code");
                String strProviderAccountName = lhsB2CAccount.get("provider_account_name");
                String strProviderAccountType = lhsB2CAccount.get("provider_account_type");
                String strProviderAccountIdentifier = lhsB2CAccount.get("provider_account_identifier");
                String strProviderAccountLongTag = lhsB2CAccount.get("provider_account_long_tag");
                String strProviderOtherDetails = lhsB2CAccount.get("provider_other_details");
                String dblMinTransactionAmount = lhsB2CAccount.get("min_transaction_amount");
                String dblMaxTransactionAmount = lhsB2CAccount.get("max_transaction_amount");

                if (strProviderAccountType.equals(theProviderAccountType)) {
                    String strProviderBranchCode = mappapi.getValueFromXMLUsingPath("/OTHER_DETAILS/DATA/PROVIDER_ACCOUNT_DETAILS/BRANCH_CODE", strProviderOtherDetails);
                    ServiceProviderAccount spaServiceProviderAccount = new ServiceProviderAccount(strProviderCode, strProviderAccountCode, strProviderAccountName, strProviderAccountType, strProviderAccountIdentifier, strProviderAccountLongTag, strProviderBranchCode, dblMinTransactionAmount, dblMaxTransactionAmount);
                    rVal.add(spaServiceProviderAccount);
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            mappapi = null;
            spManager = null;
        }

        return rVal;
    }

    public static class WithdrawalChannel {
        private String name;
        private String label;
        private String status;
        private boolean withdrawalToOtherNumber;

        public WithdrawalChannel(String name, String label, String status, boolean withdrawalToOtherNumber) {
            this.name = name;
            this.label = label;
            this.status = status;
            this.withdrawalToOtherNumber = withdrawalToOtherNumber;
        }

        public String getName() {
            return name;
        }

        public void setName(String theName) {
            this.name = theName;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String theLabel) {
            this.label = theLabel;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String theStatus) {
            this.status = theStatus;
        }

        public boolean hasWithdrawalToOtherNumberEnabled() {
            return withdrawalToOtherNumber;
        }

        public void setWithdrawalToOtherNumber(boolean theWithdrawalToOtherNumber) {
            this.withdrawalToOtherNumber = theWithdrawalToOtherNumber;
        }
    }

    public static LinkedList<WithdrawalChannel> getActiveWithdrawalChannels(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE applicationType){
        LinkedList<WithdrawalChannel> rVal = new LinkedList<>();
        NodeList nlWithdrawalChannels;
        Node ndChannel;
        WithdrawalChannel withdrawalChannel;
        try {
            nlWithdrawalChannels = MBankingAPI.getNodeListFromLocalParams(applicationType, "/OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/CONFIGURATION/CASH_WITHDRAWAL/CHANNELS/CHANNEL");
            for(int i = 0; i < nlWithdrawalChannels.getLength(); i++){
                ndChannel = nlWithdrawalChannels.item(i);

                if (ndChannel != null && ndChannel.getNodeType() == Node.ELEMENT_NODE) {
                    String strName = ndChannel.getAttributes().getNamedItem("NAME").getTextContent();
                    String strLabel = ndChannel.getAttributes().getNamedItem("LABEL").getTextContent();
                    String strStatus = ndChannel.getAttributes().getNamedItem("STATUS").getTextContent();
                    boolean blWithdrawalOtherNumberEnabled = ndChannel.getAttributes().getNamedItem("WITHDRAW_TO_OTHER_NUMBER").getTextContent().equals("ACTIVE");
                    if(strStatus.equalsIgnoreCase("ACTIVE")){
                        withdrawalChannel = new WithdrawalChannel(strName, strLabel, strStatus, blWithdrawalOtherNumberEnabled);
                        rVal.add(withdrawalChannel);
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        } finally {
            nlWithdrawalChannels = null;
            ndChannel = null;
            withdrawalChannel = null;
        }
        return rVal;
    }

    public static WithdrawalChannel getWithdrawalChannel(String theChannelName){
        WithdrawalChannel rVal = null;
        LinkedList<WithdrawalChannel> lsActiveWithdrawalChannels;
        try {
            if (theChannelName != null) {
                lsActiveWithdrawalChannels = getActiveWithdrawalChannels(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE.USSD);
                for (WithdrawalChannel lsActiveWithdrawalChannel : lsActiveWithdrawalChannels) {
                    String strName = lsActiveWithdrawalChannel.getName();
                    if (strName.equalsIgnoreCase(theChannelName)) {
                        rVal = lsActiveWithdrawalChannel;
                        break;
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            lsActiveWithdrawalChannels = null;
        }
        return rVal;
    }

    public static LinkedList<LinkedHashMap<String, String>> getStatementPeriods(ke.skyworld.mbanking.pesaapi.APIConstants.APPLICATION_TYPE applicationType){
        LinkedList<LinkedHashMap<String, String>> rVal = new LinkedList<>();
        NodeList nlStatementPeriods;
        Node ndChannel;
        try {
            nlStatementPeriods = MBankingAPI.getNodeListFromLocalParams(applicationType, "/OTHER_DETAILS/CUSTOM_PARAMETERS/SERVICE_CONFIGS/CONFIGURATION/ACCOUNT_STATEMENT/STATEMENT_PERIODS/PERIOD");
            for(int i = 0; i < nlStatementPeriods.getLength(); i++){
                ndChannel = nlStatementPeriods.item(i);

                if (ndChannel != null && ndChannel.getNodeType() == Node.ELEMENT_NODE) {
                    String strStatus = ndChannel.getAttributes().getNamedItem("STATUS").getTextContent();

                    if(strStatus.equalsIgnoreCase("ACTIVE")){
                        LinkedHashMap<String, String> hmStatementPeriods = new LinkedHashMap<String, String>();
                        for(int j = 0; j < ndChannel.getAttributes().getLength(); j++){
                            String strName = ndChannel.getAttributes().item(j).getNodeName();
                            String strValue = ndChannel.getAttributes().item(j).getTextContent();
                            hmStatementPeriods.put(strName, strValue);
                        }
                        rVal.add(hmStatementPeriods);
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        } finally {
            nlStatementPeriods = null;
            ndChannel = null;
        }
        return rVal;
    }

    public static MemberRegisterResponse fnCheckMemberRegister(String theMobileNumber, RegisterConstants.MemberRegisterType theRegisterType){
        MemberRegisterResponse registerResponse = null;
        try{
            registerResponse = RegisterProcessor.getMemberRegister(RegisterConstants.MemberRegisterIdentifierType.MSISDN, theMobileNumber, RegisterConstants.MemberRegisterType.WHITELIST);
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
            e.printStackTrace();
        }
        return registerResponse;
    }

    public static String generateTransactionHashMap(APIConstants.TransactionType theTransactionType, String theMobileNumber, double theAmount, String theOTPValue, String strPassKeyTIme){
        try{
            LinkedHashMap<String, String> hmTransaction = new LinkedHashMap<>();
            hmTransaction.put("TYPE", theTransactionType.getValue());
            hmTransaction.put("BENEFICIARY_IDENTIFIER", theMobileNumber);
            hmTransaction.put("AMOUNT", String.valueOf(theAmount));
            hmTransaction.put("PASS_KEY_VALUE", theOTPValue);
            hmTransaction.put("PASS_KEY_TIME", strPassKeyTIme);

            return new ObjectMapper().writeValueAsString(hmTransaction);
        } catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }

    public static String generateRandomIntegerString(int theLength, boolean withLeadingZero) {
        StringBuilder strRandom = new StringBuilder();

        try {
            char[] chars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
            SecureRandom random = new SecureRandom();

            for(int i = 0; i < theLength; ++i) {
                char c = chars[random.nextInt(chars.length)];
                if( i == 0 && c =='0' ) {
                    if(withLeadingZero){
                        c = chars[random.nextInt(chars.length)];
                        while (c != '0'){
                            c = chars[random.nextInt(chars.length)];
                        }
                    }
                }
                strRandom.append(c);
            }

            return strRandom.toString();
        } catch (Exception var5) {
            System.out.println(var5.getMessage());
            return strRandom.toString();
        }
    }

    public static String generateTransactionOTPJson(ke.skyworld.mbanking.ussdapi.APIConstants.TransactionType theTransactionType, String theMobileNumber, String theAccount, double theAmount, String theTransactionReference, String theOTPValue){
        try{
            return new JSONObject()
                    .put("transaction", new JSONObject()
                            .put("type", theTransactionType.getValue())
                            .put("mobile_number", theMobileNumber)
                            .put("account", theAccount)
                            .put("amount", theAmount)
                            .put("transaction_reference", theTransactionReference)
                            .put("otp", theOTPValue))
                    .toString();
        } catch (Exception e){
            e.printStackTrace();
        }
        return  null;
    }

    public static XMLGregorianCalendar fnGetCurrentDateInGregorianFormat(){
        XMLGregorianCalendar rVal = null;
        try {
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            rVal = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        } catch (Exception e){
            e.printStackTrace();
        }
        return rVal;
    }

    public static String nodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (Exception te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    public static void setLoanTypeInMemory(String theCode, String theLoanTypeXML, USSDRequest theUSSDRequest){
        Crypto crypto = new Crypto();
        try {
            String strKey = "LOAN:"+theCode+":"+theUSSDRequest.getUSSDMobileNo();
            strKey = crypto.encrypt(APIUtils.ENCRYPTION_KEY, strKey);
            theLoanTypeXML = crypto.encrypt(APIUtils.ENCRYPTION_KEY, theLoanTypeXML);
            InMemoryCache.store(strKey, theLoanTypeXML, 300);
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            crypto = null;
        }
    }

    public static String getLoanTypeFromMemory(String theCode, USSDRequest theUSSDRequest){
        Crypto crypto = new Crypto();
        USSDAPI theUSSDAPI = new USSDAPI();
        try {
            String strKey = "LOAN:"+theCode+":"+theUSSDRequest.getUSSDMobileNo();
            strKey = crypto.encrypt(APIUtils.ENCRYPTION_KEY, strKey);

            if(InMemoryCache.exists(strKey)){
                String strLoanType = (String) InMemoryCache.retrieve(strKey);
                strLoanType = crypto.decrypt(APIUtils.ENCRYPTION_KEY, strLoanType);
                return strLoanType;
            } else {
                String strLoanAccount = theUSSDRequest.getUSSDData().get(AppConstants.USSDDataType.LOAN_APPLICATION_ACCOUNT.name());
                LinkedList<String> loanTypes = theUSSDAPI.getLoanTypes(theUSSDRequest, "ALL", strLoanAccount);

                if(loanTypes != null) {
                    for (String loanType : loanTypes) {
                        String strCode = MBankingXMLFactory.getXPathValueFromXMLString("/Product/Code", loanType);

                        if(strCode.equals(theCode)){
                            APIUtils.setLoanTypeInMemory(theCode, loanType, theUSSDRequest);
                            return loanType;
                        }
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            crypto = null;
            theUSSDAPI = null;
        }
        return "";
    }

    public static void removeLoanTypeFromMemory(String theCode, USSDRequest theUSSDRequest){
        Crypto crypto = new Crypto();
        try {
            String strKey = "LOAN:"+theCode+":"+theUSSDRequest.getUSSDMobileNo();
            strKey = crypto.encrypt(APIUtils.ENCRYPTION_KEY, strKey);
            MAPPAPIDB.fnDeleteOTPData(strKey);
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        } finally {
            crypto = null;
        }
    }

    public static NodeList getXMLNodeListFromPath(String thePath, String theXML){
        try{
            InputSource source = new InputSource(new StringReader(theXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            return ((NodeList) configXPath.evaluate(thePath, xmlDocument, XPathConstants.NODESET));
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException e) {
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return null;
    }

    public static Node getXMLNodeFromPath(String thePath, String theXML){
        try{
            InputSource source = new InputSource(new StringReader(theXML));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(source);
            XPath configXPath = XPathFactory.newInstance().newXPath();

            return (Node) configXPath.evaluate(thePath, xmlDocument, XPathConstants.NODE);
        } catch (ParserConfigurationException | IOException | XPathExpressionException | SAXException e) {
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return null;
    }

    public static boolean checkIfInstallmentIsWithinLimits(String strUserInput, String strXML){
        return true;
    }

    public static String  getLoanInstallmentLabel(String theInstallmentId, String strXML){
        try {
            NodeList nlInstallments = APIUtils.getXMLNodeListFromPath("/Product/PresetInstallments/Installment", strXML);

            if (nlInstallments != null) {
                for (int i = 1; i <= nlInstallments.getLength(); i++) {
                    String strInstallmentId = MBankingXMLFactory.getXPathValueFromXMLString("/Product/PresetInstallments/Installment["+i+"]/@Id", strXML);
                    if(theInstallmentId.equalsIgnoreCase(strInstallmentId)){
                        return MBankingXMLFactory.getXPathValueFromXMLString("/Product/PresetInstallments/Installment["+i+"]/@Label", strXML);
                    }
                }
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return "";
    }

    public static LoanAmountLimits  getLoanInstallmentAmounts(String theInstallmentId, String strXML){
        String strMinimum = "0", strMaximum = "0";
        try {
            if(theInstallmentId != null){
                NodeList nlInstallments = APIUtils.getXMLNodeListFromPath("/Product/PresetInstallments/Installment", strXML);

                if (nlInstallments != null) {
                    for (int i = 1; i <= nlInstallments.getLength(); i++) {
                        String strInstallmentId = MBankingXMLFactory.getXPathValueFromXMLString("/Product/PresetInstallments/Installment["+i+"]/@Id", strXML);
                        if(theInstallmentId.equalsIgnoreCase(strInstallmentId)){
                            strMinimum = MBankingXMLFactory.getXPathValueFromXMLString("/Product/PresetInstallments/Installment["+i+"]/Qualification/Minimum", strXML);
                            strMaximum = MBankingXMLFactory.getXPathValueFromXMLString("/Product/PresetInstallments/Installment["+i+"]/Qualification/Maximum", strXML);
                        }
                    }
                } else {
                    strMinimum = MBankingXMLFactory.getXPathValueFromXMLString("/Product/DefaultQualification/Minimum", strXML);
                    strMaximum = MBankingXMLFactory.getXPathValueFromXMLString("/Product/DefaultQualification/Maximum", strXML);

                    System.out.println("Minimum: "+strMinimum);
                    System.out.println("Maximum: "+strMaximum);
                }
            } else {
                strMinimum = MBankingXMLFactory.getXPathValueFromXMLString("/Product/DefaultQualification/Minimum", strXML);
                strMaximum = MBankingXMLFactory.getXPathValueFromXMLString("/Product/DefaultQualification/Maximum", strXML);

                System.out.println("Minimum: "+strMinimum);
                System.out.println("Maximum: "+strMaximum);
            }
        } catch (Exception e){
            System.err.println(APIUtils.class.getSimpleName()+"."+new Object() {}.getClass().getEnclosingMethod().getName()+"() ERROR : " + e.getMessage());
        }
        return new LoanAmountLimits(strMinimum, strMaximum);
    }

    public static class LoanAmountLimits {
        private String minimum;
        private String maximum;

        public LoanAmountLimits(String minimum, String maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public String getMinimum() {
            return minimum;
        }

        public void setMinimum(String minimum) {
            this.minimum = minimum;
        }

        public String getMaximum() {
            return maximum;
        }

        public void setMaximum(String maximum) {
            this.maximum = maximum;
        }
    }

    public static String convertToTitleCaseIteratingChars(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder converted = new StringBuilder();

        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }

        return converted.toString();
    }

    public static String convertNodeToString(Node node) {
        StringWriter sw = new StringWriter();
        try {
            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            t.setOutputProperty(OutputKeys.INDENT, "yes");
            t.transform(new DOMSource(node), new StreamResult(sw));
        } catch (TransformerException te) {
            System.out.println("nodeToString Transformer Exception");
        }
        return sw.toString();
    }

    public static HashMap<String, String> getUserIdentifierDetails(String strUserPhoneNumber) {
        HashMap<String, String> userIdentifierDetails = new HashMap<>();

        TransactionWrapper<FlexicoreHashMap> signatoryDetailsWrapper = Repository.selectWhere(StringRefs.SENTINEL,
                TBL_CUSTOMER_REGISTER_SIGNATORIES, "identifier_type, identifier, full_name, primary_email_address",
                new FilterPredicate("primary_mobile_number = :primary_mobile_number"),
                new FlexicoreHashMap().addQueryArgument(":primary_mobile_number", strUserPhoneNumber));

        if (signatoryDetailsWrapper.hasErrors()) {
            return userIdentifierDetails;
        }

        FlexicoreHashMap signatoryDetailsMap = signatoryDetailsWrapper.getSingleRecord();

        if (signatoryDetailsMap != null && !signatoryDetailsMap.isEmpty()) {
            userIdentifierDetails.put("identifier_type", signatoryDetailsMap.getStringValue("identifier_type"));
            userIdentifierDetails.put("identifier", signatoryDetailsMap.getStringValue("identifier"));
            userIdentifierDetails.put("full_name", signatoryDetailsMap.getStringValue("full_name"));
            userIdentifierDetails.put("primary_email_address", signatoryDetailsMap.getStringValue("primary_email_address"));
            return userIdentifierDetails;
        }

        return userIdentifierDetails;
    }

}
