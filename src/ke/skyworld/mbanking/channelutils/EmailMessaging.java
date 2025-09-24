package ke.skyworld.mbanking.channelutils;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.beans.TransactionWrapper;
import ke.co.skyworld.smp.query_manager.query.FilterPredicate;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.DateTime;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;
import ke.co.skyworld.smp.utility_items.data_formatting.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;

import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_EMAIL_LOG;
import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_SENDER_EMAIL_PARAMETERS;

public class EmailMessaging {

    public static void sendEmail(String recipientAddress, String subject, String emailBody, String category) {
        List<String> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(recipientAddress);
        sendEmail(recipientAddresses, null, null, subject, emailBody, category, null, "NO");
    }

    public static void sendEmail(List<String> recipientAddresses, String subject, String emailBody, String category) {
        sendEmail(recipientAddresses, null, null, subject, emailBody, category, null, "NO");
    }

    public static void sendEmail(String recipientAddress, String subject, String emailBody, String category, HashMap<String, String> attachmentsMap) {
        List<String> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(recipientAddress);
        sendEmail(recipientAddresses, null, null, subject, emailBody, category, attachmentsMap, "NO");
    }

    public static void sendEmail(String recipientAddress, String subject, String emailBody, String category, HashMap<String, String> attachmentsMap, String deleteAttachment) {
        List<String> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(recipientAddress);
        sendEmail(recipientAddresses, null, null, subject, emailBody, category, attachmentsMap, deleteAttachment);
    }

    public static void sendEmail(List<String> recipientAddresses, String ccAddresses, String bCCAddresses, String subject, String emailBody, String category, HashMap<String, String> attachmentsMap, String deleteAttachment) {

        if (recipientAddresses.isEmpty()) {
            return;
        }

        Document recipientsDoc = XmlUtils.createNewDocument();
        Element elRootContacts = recipientsDoc.createElement("CONTACTS");
        recipientsDoc.appendChild(elRootContacts);

        for (String recipientAddress : recipientAddresses) {
            Element elContact = recipientsDoc.createElement("CONTACT");
            elContact.setAttribute("NAME", recipientAddress);
            elContact.setAttribute("ACTION", "TO");
            elContact.setAttribute("STATUS", "ACTIVE");
            elContact.setAttribute("NOTE", "");
            elContact.setTextContent(recipientAddress);
            elRootContacts.appendChild(elContact);
        }

        if (ccAddresses != null && !ccAddresses.isBlank()) {
            String[] ccAddressesArr = ccAddresses.split(",");

            for (String ccAddress : ccAddressesArr) {

                if (ccAddress.equalsIgnoreCase("vinrithi96@gmail.com")) {
                    continue;
                }

                Element elContact = recipientsDoc.createElement("CONTACT");
                elContact.setAttribute("NAME", ccAddress.trim());
                elContact.setAttribute("ACTION", "CC");
                elContact.setAttribute("STATUS", "ACTIVE");
                elContact.setAttribute("NOTE", "");
                elContact.setTextContent(ccAddress.trim());
                elRootContacts.appendChild(elContact);
            }
        }

        if (bCCAddresses != null && !bCCAddresses.isBlank()) {
            String[] bccAddressesArr = bCCAddresses.split(",");

            for (String bccAddress : bccAddressesArr) {

                Element elContact = recipientsDoc.createElement("CONTACT");
                elContact.setAttribute("NAME", bccAddress.trim());
                elContact.setAttribute("ACTION", "BCC");
                elContact.setAttribute("STATUS", "ACTIVE");
                elContact.setAttribute("NOTE", "");
                elContact.setTextContent(bccAddress.trim());
                elRootContacts.appendChild(elContact);
            }
        }

        String hasAttachments = "NO";

        String strAttachments = "<LINKS/>";

        if (attachmentsMap != null && !attachmentsMap.isEmpty()) {
            hasAttachments = "YES";

            Document linksDoc = XmlUtils.createNewDocument();
            Element elRootLink = linksDoc.createElement("LINKS");
            linksDoc.appendChild(elRootLink);

            for (Map.Entry<String, String> entry : attachmentsMap.entrySet()) {
                String attachmentName = entry.getKey();
                String filePath = entry.getValue();

                Element elLink = linksDoc.createElement("LINK");
                elLink.setAttribute("NAME", attachmentName);
                elLink.setTextContent(filePath);
                elRootLink.appendChild(elLink);
            }

            strAttachments = XmlUtils.convertNodeToStr(linksDoc);
        }

        String strSenderParameterId = "1";

        FlexicoreHashMap senderMap = Repository.selectWhere(StringRefs.SENTINEL,
                TBL_SENDER_EMAIL_PARAMETERS,
                new FilterPredicate("sender_email_id = :sender_email_id"),
                new FlexicoreHashMap().addQueryArgument(":sender_email_id", strSenderParameterId)).getSingleRecord();

        String strGUID = UUID.randomUUID().toString();

        FlexicoreHashMap emailInsertMap = new FlexicoreHashMap();

        emailInsertMap.putValue("originator_id", strGUID);
        emailInsertMap.putValue("status_code", 10);
        emailInsertMap.putValue("status_name", "ACTIVE");
        emailInsertMap.putValue("status_description", "New Email");
        emailInsertMap.putValue("status_date", DateTime.getCurrentDateTime());
        emailInsertMap.putValue("sender_email_id", senderMap.getStringValue("sender_email_id"));
        emailInsertMap.putValue("sender_email_name", senderMap.getValueOrIfNull("sender_email_name", ""));
        emailInsertMap.putValue("sender_email_address", senderMap.getValueOrIfNull("sender_email_address", ""));
        emailInsertMap.putValue("receiver_email_addresses", XmlUtils.convertNodeToStr(recipientsDoc));
        emailInsertMap.putValue("email_type", "OUTBOUND_EMAIL");
        emailInsertMap.putValue("email_subject", subject);
        emailInsertMap.putValue("email_content_type", "HTML");
        emailInsertMap.putValue("email_message_body", emailBody);
        emailInsertMap.putValue("attachment", hasAttachments);
        emailInsertMap.putValue("delete_attachment", deleteAttachment);
        emailInsertMap.putValue("attachment_links", strAttachments);
        emailInsertMap.putValue("sensitivity", "NORMAL");
        emailInsertMap.putValue("category", category);
        emailInsertMap.putValue("priority", 200);
        emailInsertMap.putValue("send_count", 0);
        emailInsertMap.putValue("request_application", "SMP");
        emailInsertMap.putValue("request_correlation_id", strGUID);
        emailInsertMap.putValue("source_application", "SMP");
        emailInsertMap.putValue("source_reference", strGUID);
        emailInsertMap.putValue("schedule_email", "NO");
        emailInsertMap.putValue("date_scheduled", DateTime.getCurrentDateTime());
        emailInsertMap.putValue("send_integrity_hash", "");
        emailInsertMap.putValue("general_flag", "");
        emailInsertMap.putValue("date_created", DateTime.getCurrentDateTime());

        TransactionWrapper<FlexicoreHashMap> wrapper = Repository.insertAutoIncremented(StringRefs.SENTINEL, TBL_EMAIL_LOG, emailInsertMap);
        if (wrapper.hasErrors()) {
            System.err.println("Failed to Queue New Email");
        } else {
            System.out.println("Queued New Email Successfully");
        }
    }

    public static void sendEmailWithAttachment(String recipientAddress, String subject, String emailBody, String category, HashMap<String, String> attachmentsMap) {
        List<String> recipientAddresses = new ArrayList<>();
        recipientAddresses.add(recipientAddress);
        sendEmail(recipientAddresses, null, null, subject, emailBody, category, attachmentsMap, "NO");
    }
}
