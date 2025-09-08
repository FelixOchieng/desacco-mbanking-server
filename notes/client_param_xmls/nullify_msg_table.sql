alter table [Chai$Sky SMS Messages]
    alter column msg_id bigint null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_product_id bigint null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_provider_code nvarchar(10) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_charge nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_status_code int null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_status_description nvarchar(200) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_status_date datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column sender nvarchar(15) null
go

alter table [Chai$Sky SMS Messages]
    alter column receiver nvarchar(15) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_type nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_source_reference nvarchar(50) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_destination_reference nvarchar(50) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_xml_data nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_category nvarchar(40) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_priority int null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_send_count int null
go

alter table [Chai$Sky SMS Messages]
    alter column schedule_msg nvarchar(5) null
go

alter table [Chai$Sky SMS Messages]
    alter column date_scheduled datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_send_integrity_hash nvarchar(200) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_response_date datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_response_xml_data nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_response_integrity_hash nvarchar(200) null
go

alter table [Chai$Sky SMS Messages]
    alter column transaction_date datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column date_created datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column [SMS Date] datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column [Account To Charge] nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column Posted tinyint null
go

alter table [Chai$Sky SMS Messages]
    alter column server_id bigint null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_charge_applied nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_format nvarchar(10) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_command nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_response_description nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_description nvarchar(200) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_xml_data nvarchar(200) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_date datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_integrity_hash nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_submit_count int null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_submit_status nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_submit_description nvarchar(150) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_submit_date datetime null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_general_flag nvarchar(50) null
go

alter table [Chai$Sky SMS Messages]
    alter column sender_type nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column receiver_type nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column [Charge Member] tinyint null
go

alter table [Chai$Sky SMS Messages]
    alter column Finalized tinyint null
go

alter table [Chai$Sky SMS Messages]
    alter column [Split Charge] tinyint null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_request_application nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_request_correlation_id nvarchar(50) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_source_application nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_response nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_response_code int null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result nvarchar(250) null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_result_code int null
go

alter table [Chai$Sky SMS Messages]
    alter column msg_mode nvarchar(30) null
go

alter table [Chai$Sky SMS Messages]
    alter column [Sent By] nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column Source int null
go

alter table [Chai$Sky SMS Messages]
    alter column [SMS Charge] decimal(38, 20) null
go

alter table [Chai$Sky SMS Messages]
    alter column [Document No] nvarchar(50) null
go

alter table [Chai$Sky SMS Messages]
    alter column [Member No] nvarchar(20) null
go

alter table [Chai$Sky SMS Messages]
    alter column [Entered By] nvarchar(50) null
go

