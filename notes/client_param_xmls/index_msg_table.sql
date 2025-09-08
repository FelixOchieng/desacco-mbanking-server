create index [Chai$Sky SMS Messages_timestamp_index]
    on [Chai$Sky SMS Messages] (timestamp)
go

create index [Chai$Sky SMS Messages_originator_id_index]
    on [Chai$Sky SMS Messages] (originator_id)
go

create index [Chai$Sky SMS Messages_msg_id_index]
    on [Chai$Sky SMS Messages] (msg_id)
go

create index [Chai$Sky SMS Messages_msg_product_id_index]
    on [Chai$Sky SMS Messages] (msg_product_id)
go

create index [Chai$Sky SMS Messages_msg_provider_code_index]
    on [Chai$Sky SMS Messages] (msg_provider_code)
go

create index [Chai$Sky SMS Messages_msg_charge_index]
    on [Chai$Sky SMS Messages] (msg_charge)
go

create index [Chai$Sky SMS Messages_msg_status_code_index]
    on [Chai$Sky SMS Messages] (msg_status_code)
go

create index [Chai$Sky SMS Messages_msg_status_description_index]
    on [Chai$Sky SMS Messages] (msg_status_description)
go

create index [Chai$Sky SMS Messages_msg_status_date_index]
    on [Chai$Sky SMS Messages] (msg_status_date)
go

create index [Chai$Sky SMS Messages_sender_index]
    on [Chai$Sky SMS Messages] (sender)
go

create index [Chai$Sky SMS Messages_receiver_index]
    on [Chai$Sky SMS Messages] (receiver)
go

create index [Chai$Sky SMS Messages_msg_index]
    on [Chai$Sky SMS Messages] (msg)
go

create index [Chai$Sky SMS Messages_msg_type_index]
    on [Chai$Sky SMS Messages] (msg_type)
go

create index [Chai$Sky SMS Messages_msg_source_reference_index]
    on [Chai$Sky SMS Messages] (msg_source_reference)
go

create index [Chai$Sky SMS Messages_msg_destination_reference_index]
    on [Chai$Sky SMS Messages] (msg_destination_reference)
go

create index [Chai$Sky SMS Messages_msg_xml_data_index]
    on [Chai$Sky SMS Messages] (msg_xml_data)
go

create index [Chai$Sky SMS Messages_msg_category_index]
    on [Chai$Sky SMS Messages] (msg_category)
go

create index [Chai$Sky SMS Messages_msg_priority_index]
    on [Chai$Sky SMS Messages] (msg_priority)
go

create index [Chai$Sky SMS Messages_msg_send_count_index]
    on [Chai$Sky SMS Messages] (msg_send_count)
go

create index [Chai$Sky SMS Messages_schedule_msg_index]
    on [Chai$Sky SMS Messages] (schedule_msg)
go

create index [Chai$Sky SMS Messages_date_scheduled_index]
    on [Chai$Sky SMS Messages] (date_scheduled)
go

create index [Chai$Sky SMS Messages_msg_send_integrity_hash_index]
    on [Chai$Sky SMS Messages] (msg_send_integrity_hash)
go

create index [Chai$Sky SMS Messages_msg_response_date_index]
    on [Chai$Sky SMS Messages] (msg_response_date)
go

create index [Chai$Sky SMS Messages_msg_response_xml_data_index]
    on [Chai$Sky SMS Messages] (msg_response_xml_data)
go

create index [Chai$Sky SMS Messages_msg_response_integrity_hash_index]
    on [Chai$Sky SMS Messages] (msg_response_integrity_hash)
go

create index [Chai$Sky SMS Messages_transaction_date_index]
    on [Chai$Sky SMS Messages] (transaction_date)
go

create index [Chai$Sky SMS Messages_date_created_index]
    on [Chai$Sky SMS Messages] (date_created)
go

create index [Chai$Sky SMS Messages_SMS Date_index]
    on [Chai$Sky SMS Messages] ([SMS Date])
go

create index [Chai$Sky SMS Messages_Account To Charge_index]
    on [Chai$Sky SMS Messages] ([Account To Charge])
go

create index [Chai$Sky SMS Messages_Posted_index]
    on [Chai$Sky SMS Messages] (Posted)
go

create index [Chai$Sky SMS Messages_transaction_id_index]
    on [Chai$Sky SMS Messages] (transaction_id)
go

create index [Chai$Sky SMS Messages_server_id_index]
    on [Chai$Sky SMS Messages] (server_id)
go

create index [Chai$Sky SMS Messages_msg_charge_applied_index]
    on [Chai$Sky SMS Messages] (msg_charge_applied)
go

create index [Chai$Sky SMS Messages_msg_format_index]
    on [Chai$Sky SMS Messages] (msg_format)
go

create index [Chai$Sky SMS Messages_msg_command_index]
    on [Chai$Sky SMS Messages] (msg_command)
go

create index [Chai$Sky SMS Messages_msg_sensitivity_index]
    on [Chai$Sky SMS Messages] (msg_sensitivity)
go

create index [Chai$Sky SMS Messages_msg_response_description_index]
    on [Chai$Sky SMS Messages] (msg_response_description)
go

create index [Chai$Sky SMS Messages_msg_result_description_index]
    on [Chai$Sky SMS Messages] (msg_result_description)
go

create index [Chai$Sky SMS Messages_msg_result_xml_data_index]
    on [Chai$Sky SMS Messages] (msg_result_xml_data)
go

create index [Chai$Sky SMS Messages_msg_result_date_index]
    on [Chai$Sky SMS Messages] (msg_result_date)
go

create index [Chai$Sky SMS Messages_msg_result_integrity_hash_index]
    on [Chai$Sky SMS Messages] (msg_result_integrity_hash)
go

create index [Chai$Sky SMS Messages_msg_result_submit_count_index]
    on [Chai$Sky SMS Messages] (msg_result_submit_count)
go

create index [Chai$Sky SMS Messages_msg_result_submit_status_index]
    on [Chai$Sky SMS Messages] (msg_result_submit_status)
go

create index [Chai$Sky SMS Messages_msg_result_submit_description_index]
    on [Chai$Sky SMS Messages] (msg_result_submit_description)
go

create index [Chai$Sky SMS Messages_msg_result_submit_date_index]
    on [Chai$Sky SMS Messages] (msg_result_submit_date)
go

create index [Chai$Sky SMS Messages_msg_general_flag_index]
    on [Chai$Sky SMS Messages] (msg_general_flag)
go

create index [Chai$Sky SMS Messages_sender_type_index]
    on [Chai$Sky SMS Messages] (sender_type)
go

create index [Chai$Sky SMS Messages_receiver_type_index]
    on [Chai$Sky SMS Messages] (receiver_type)
go

create index [Chai$Sky SMS Messages_Charge Member_index]
    on [Chai$Sky SMS Messages] ([Charge Member])
go

create index [Chai$Sky SMS Messages_Finalized_index]
    on [Chai$Sky SMS Messages] (Finalized)
go

create index [Chai$Sky SMS Messages_Split Charge_index]
    on [Chai$Sky SMS Messages] ([Split Charge])
go

create index [Chai$Sky SMS Messages_msg_request_application_index]
    on [Chai$Sky SMS Messages] (msg_request_application)
go

create index [Chai$Sky SMS Messages_msg_request_correlation_id_index]
    on [Chai$Sky SMS Messages] (msg_request_correlation_id)
go

create index [Chai$Sky SMS Messages_msg_source_application_index]
    on [Chai$Sky SMS Messages] (msg_source_application)
go

create index [Chai$Sky SMS Messages_msg_response_index]
    on [Chai$Sky SMS Messages] (msg_response)
go

create index [Chai$Sky SMS Messages_msg_response_code_index]
    on [Chai$Sky SMS Messages] (msg_response_code)
go

create index [Chai$Sky SMS Messages_msg_result_index]
    on [Chai$Sky SMS Messages] (msg_result)
go

create index [Chai$Sky SMS Messages_msg_result_code_index]
    on [Chai$Sky SMS Messages] (msg_result_code)
go

create index [Chai$Sky SMS Messages_msg_mode_index]
    on [Chai$Sky SMS Messages] (msg_mode)
go

create index [Chai$Sky SMS Messages_Sent By_index]
    on [Chai$Sky SMS Messages] ([Sent By])
go

create index [Chai$Sky SMS Messages_Source_index]
    on [Chai$Sky SMS Messages] (Source)
go

create index [Chai$Sky SMS Messages_SMS Charge_index]
    on [Chai$Sky SMS Messages] ([SMS Charge])
go

create index [Chai$Sky SMS Messages_Document No_index]
    on [Chai$Sky SMS Messages] ([Document No])
go

create index [Chai$Sky SMS Messages_Member No_index]
    on [Chai$Sky SMS Messages] ([Member No])
go

create index [Chai$Sky SMS Messages_Entered By_index]
    on [Chai$Sky SMS Messages] ([Entered By])
go

