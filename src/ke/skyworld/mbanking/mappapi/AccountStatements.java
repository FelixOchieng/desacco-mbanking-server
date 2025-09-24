package ke.skyworld.mbanking.mappapi;

import com.openhtmltopdf.pdfboxout.PdfBoxRenderer;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.file_utils.FileOps;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class AccountStatements {

    /*public static void main(String[] args) {

        OutputStream outputStream = null;
        PDDocument doc = null;

        String strTheOutputFileName = System.getProperty("user.dir")+"/system-folders/outputs/account-statement-"+ DateTime.getCurrentDateTime("yyyy-MM-dd-HHmmssSSSSSS") +".pdf";

        try {

            doc = new PDDocument();
            PdfRendererBuilder pdfRendererBuilder = new PdfRendererBuilder();
            pdfRendererBuilder.useFastMode();
            pdfRendererBuilder.withHtmlContent(getTest(), "file://"+System.getProperty("user.dir")+"/system-folders/templates/");
            pdfRendererBuilder.usePDDocument(doc);
            pdfRendererBuilder.useSVGDrawer(new BatikSVGDrawer());

            PdfBoxRenderer renderer = pdfRendererBuilder.buildPdfRenderer();
            renderer.createPDFWithoutClosing();
            renderer.close();

            *//*AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(strNationalId, strNationalId, ap);
            doc.protect(spp);*//*

            outputStream = new FileOutputStream(strTheOutputFileName, false);

            doc.save(outputStream);
            doc.close();
            doc.close();
            outputStream.close();

        }
        catch (Exception e) {
            e.printStackTrace();

        }finally {
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(doc != null){
                try {
                    doc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }



    }*/

    public static String generateAccountStatementPDF(String strStatement, String strAccountNo/*, String strNationalId*/){
        OutputStream outputStream = null;
        PDDocument doc = null;

        String strTheOutputFileName = System.getProperty("user.dir")+"/system-folders/outputs/"+strAccountNo+"-account-statement-"+ DateTime.getCurrentDateTime("yyyy-MM-dd-HHmmssSSSSSS") +".pdf";

        try {

            doc = new PDDocument();
            PdfRendererBuilder pdfRendererBuilder = new PdfRendererBuilder();
            pdfRendererBuilder.useFastMode();
            pdfRendererBuilder.withHtmlContent(strStatement, "file://"+System.getProperty("user.dir")+"/system-folders/templates/");
            pdfRendererBuilder.usePDDocument(doc);
            pdfRendererBuilder.useSVGDrawer(new BatikSVGDrawer());

            PdfBoxRenderer renderer = pdfRendererBuilder.buildPdfRenderer();
            renderer.createPDFWithoutClosing();
            renderer.close();

            /*AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(strNationalId, strNationalId, ap);
            doc.protect(spp);*/

            outputStream = new FileOutputStream(strTheOutputFileName, false);

            doc.save(outputStream);
            doc.close();
            doc.close();
            outputStream.close();

           // return HashUtils.base64Encode(outputStream.toByteArray());
            //return strTheOutputFileName;
            return encodeFileToBase64Binary(strTheOutputFileName);
        }
        catch (Exception e) {
            e.printStackTrace();

        }finally {
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

             if(doc != null){
                try {
                    doc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            FileOps.deleteFile(strTheOutputFileName);
        }
        return "";
    }

    private static String encodeFileToBase64Binary(String fileName) throws IOException {
        File file = new File(fileName);
        byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(file));
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    public static String getAccountStatementHTML(){

        FileReader fileReader = null;
        BufferedReader reader = null;

        try {
            fileReader = new FileReader(System.getProperty("user.dir")+"/system-folders/templates/account-statement-template.html");
            reader = new BufferedReader(fileReader);

            StringBuilder resultBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }

            fileReader.close();
            reader.close();

            return resultBuilder.toString();
        }catch (Exception e){
            e.printStackTrace();
        }finally {

            if(fileReader != null){
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }

    public static String getLoanStatementHTML(){

        FileReader fileReader = null;
        BufferedReader reader = null;

        try {
            fileReader = new FileReader(System.getProperty("user.dir")+"/system-folders/templates/loan-statement-template.html");
            reader = new BufferedReader(fileReader);

            StringBuilder resultBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }

            fileReader.close();
            reader.close();

            return resultBuilder.toString();
        }catch (Exception e){
            e.printStackTrace();
        }finally {

            if(fileReader != null){
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

   /* public static String getTest(){

        FileReader fileReader = null;
        BufferedReader reader = null;

        try {
            fileReader = new FileReader(System.getProperty("user.dir")+"/system-folders/templates/playground-2.html");
            reader = new BufferedReader(fileReader);

            StringBuilder resultBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }

            fileReader.close();
            reader.close();

            return resultBuilder.toString();
        }catch (Exception e){
            e.printStackTrace();
        }finally {

            if(fileReader != null){
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }*/


}
