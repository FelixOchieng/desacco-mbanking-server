package ke.skyworld.mbanking.channelutils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class EmailTemplates {

    public static String mobileBankingAccountLockedTemplate() {
        return readFile(System.getProperty("user.dir") + "/system-folders/templates/mobile-banking-account-locked.html");
    }

    public static String helpAndSupportNewRequestTemplate() {
        return readFile(System.getProperty("user.dir") + "/system-folders/templates/help-and-support-new-request.html");
    }

    public static String helpAndSupportNewCommentsTemplate() {
        return readFile(System.getProperty("user.dir") + "/system-folders/templates/help-and-support-new-comments.html");
    }

    public static String helpAndSupportAssignedRequestTemplate() {
        return readFile(System.getProperty("user.dir") + "/system-folders/templates/help-and-support-assigned-request.html");
    }

    private static String readFile(String strFileName) {

        FileReader fileReader = null;
        BufferedReader reader = null;

        try {
            fileReader = new FileReader(strFileName);
            reader = new BufferedReader(fileReader);

            StringBuilder resultBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                resultBuilder.append(line);
            }

            fileReader.close();
            reader.close();

            return resultBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            if (fileReader != null) {
                try {
                    fileReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "";
    }
}
