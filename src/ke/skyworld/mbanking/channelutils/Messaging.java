package ke.skyworld.mbanking.channelutils;

import ke.co.skyworld.smp.query_manager.beans.FlexicoreHashMap;
import ke.co.skyworld.smp.query_manager.query.FilterPredicate;
import ke.co.skyworld.smp.query_repository.Repository;
import ke.co.skyworld.smp.utility_items.constants.StringRefs;

import static ke.co.skyworld.smp.query_manager.SystemTables.TBL_MESSAGE_TEMPLATES;

public class Messaging {

    public static String getMessagingTemplate(String templateType, String strTemplateName) {

        try {
            FlexicoreHashMap emailTemplateMap = Repository.selectWhere(StringRefs.SENTINEL, TBL_MESSAGE_TEMPLATES,
                    new FilterPredicate("template_type = :template_type AND template_name = :template_name"),
                    new FlexicoreHashMap()
                            .addQueryArgument(":template_type", templateType)
                            .addQueryArgument(":template_name", strTemplateName)).getSingleRecord();

            if (emailTemplateMap == null || emailTemplateMap.isEmpty()) {
                System.err.println("Messaging.getMessagingTemplate() -> No Template Found Named '" + strTemplateName + "', Type = '" + templateType + "'");
                return null;
            }

            return emailTemplateMap.getStringValue("template_value");

        } catch (Exception e) {
            System.err.println("Messaging.getMessagingTemplate() -> " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
