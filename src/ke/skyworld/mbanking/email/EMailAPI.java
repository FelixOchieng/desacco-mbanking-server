package ke.skyworld.mbanking.email;

import ke.skyworld.lib.mbanking.core.MBankingDB;
import ke.skyworld.lib.mbanking.core.MBankingLocalParameters;
import ke.skyworld.lib.mbanking.email.EMail;
import ke.skyworld.lib.mbanking.utils.NamedParameterStatement;

public class EMailAPI {

    public static int insertEMailLog(EMail theEMail)
    {


        String strInsertSQL = null;
        int count = 0;
        NamedParameterStatement pInsertEMailLog = null;

        String strMSGTableName = "email_log";

        try
        {
            switch (MBankingLocalParameters.getDatabaseType()) {
                case MySQL:
                    strInsertSQL = "INSERT INTO " + strMSGTableName+ "(originator_id, status_code, status_name, status_description, status_date, sender_email_id, sender_email_name, sender_email_address," +
                            "receiver_email_addresses, email_type, email_subject, email_content_type, email_message_body, attachment, delete_attachment, attachment_link," +
                            "sensitivity, category, priority, send_count, request_application, request_correlation_id, source_application, source_reference," +
                            "schedule_email, date_scheduled, send_integrity_hash, general_flag, date_created)" +
                            "VALUES (:originator_id, :status_code, :status_name, :status_description, NOW(), :sender_email_id, :sender_email_name, :sender_email_address," +
                            ":receiver_email_addresses, :email_type, :email_subject, :email_content_type, :email_message_body, :attachment, :delete_attachment, :attachment_link," +
                            ":sensitivity, :category, :priority, :send_count, :request_application, :request_correlation_id, :source_application, :source_reference," +
                            ":schedule_email, :date_scheduled, :send_integrity_hash, :general_flag, NOW())";
                    break;
                case MicrosoftSQL:
                    strInsertSQL = "INSERT INTO " + strMSGTableName+ "(originator_id, status_code, status_name, status_description, status_date, sender_email_id, sender_email_name, sender_email_address," +
                            "receiver_email_addresses, email_type, email_subject, email_content_type, email_message_body, attachment, delete_attachment, attachment_link," +
                            "sensitivity, category, priority, send_count, request_application, request_correlation_id, source_application, source_reference," +
                            "schedule_email, date_scheduled, send_integrity_hash, general_flag, date_created)" +
                            "VALUES (:originator_id, :status_code, :status_name, :status_description, GETDATE(), :sender_email_id, :sender_email_name, :sender_email_address," +
                            ":receiver_email_addresses, :email_type, :email_subject, :email_content_type, :email_message_body, :attachment, :delete_attachment, :attachment_link," +
                            ":sensitivity, :category, :priority, :send_count, :request_application, :request_correlation_id, :source_application, :source_reference," +
                            ":schedule_email, :date_scheduled, :send_integrity_hash, :general_flag, GETDATE())";
                    break;
                case Oracle:
                    System.out.println("Oracle Database Type is not yet supported\n");
                    break;
                case PostgreSQL:
                    System.out.println("PostgreSQL Database Type is not yet supported\n");
                    break;
                case MicrosoftAccess:
                    System.out.println("Microsoft Access Database Type is not yet supported\n");
                    break;
                default:
                    System.out.println("Invalid Database Type selected\n");
                    break;
            }

            //System.out.println(strInsertSQL);

            pInsertEMailLog = new NamedParameterStatement(MBankingDB.getConnection(), strInsertSQL);

            pInsertEMailLog.setString("originator_id", theEMail.getOriginatorID());
            pInsertEMailLog.setInt("status_code", theEMail.getStatusCode());
            pInsertEMailLog.setString("status_name", theEMail.getStatusName());
            pInsertEMailLog.setString("status_description", theEMail.getStatusDescription());
            pInsertEMailLog.setLong("sender_email_id", theEMail.getSenderEMailID());
            pInsertEMailLog.setString("sender_email_name", theEMail.getSenderEMailName());
            pInsertEMailLog.setString("sender_email_address", theEMail.getSenderEMailAddress());
            pInsertEMailLog.setString("receiver_email_addresses", theEMail.getReceiverEMailAddresses());
            pInsertEMailLog.setString("email_type", theEMail.getEMailType().getValue());
            pInsertEMailLog.setString("email_subject", theEMail.getEMailSubject());
            pInsertEMailLog.setString("email_content_type", theEMail.getEMailContentType());
            pInsertEMailLog.setString("email_message_body", theEMail.getEMailMessageBody());
            pInsertEMailLog.setString("attachment", theEMail.getAttachment().getValue());
            pInsertEMailLog.setString("delete_attachment", theEMail.getDeleteAttachment().getValue());
            pInsertEMailLog.setString("attachment_link", theEMail.getAttachmentLinks());
            pInsertEMailLog.setString("sensitivity", theEMail.getSensitivity().getValue());
            pInsertEMailLog.setString("category", theEMail.getCategory());
            pInsertEMailLog.setInt("priority", theEMail.getPriority());
            pInsertEMailLog.setInt("send_count", theEMail.getSendCount());
            pInsertEMailLog.setString("request_application", theEMail.getRequestApplication());
            pInsertEMailLog.setString("request_correlation_id", theEMail.getRequestCorrelationID());
            pInsertEMailLog.setString("source_application", theEMail.getSourceApplication());
            pInsertEMailLog.setString("source_reference", theEMail.getSourceReference());
            pInsertEMailLog.setString("schedule_email", theEMail.getScheduleEMail().getValue());
            pInsertEMailLog.setString("date_scheduled", theEMail.getDateScheduled());
            pInsertEMailLog.setString("send_integrity_hash", theEMail.getIntegrityHash());
            pInsertEMailLog.setString("general_flag", theEMail.getGeneralFlag());

            count = pInsertEMailLog.executeUpdate();
            pInsertEMailLog.close();

        }
        catch (Exception e)
        {
            System.out.println ("EMailDB.insertEMailLog() Error message: " + e.getMessage ());
        }
        finally
        {
            strInsertSQL = null;
            if(pInsertEMailLog!=null){ try{pInsertEMailLog.close();}catch(Exception e){} }
            pInsertEMailLog = null;
        }

        return count;
    }
}
