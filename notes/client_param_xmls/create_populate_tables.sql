create table if not exists PESA_IN_pesa_log
(
    transaction_id                 bigint unsigned auto_increment
    primary key,
    originator_id                  varchar(100)                                                                               not null,
    pesa_id                        bigint unsigned                                                                            null,
    server_id                      bigint unsigned                                                                            null,
    pesa_product_id                bigint unsigned                                                                            not null,
    pesa_charge                    varchar(20)                                                                                not null,
    pesa_charge_applied            varchar(20)                                                                                null,
    pesa_status_code               int unsigned                                                                               not null,
    pesa_status_description        varchar(1000)                                                                              null,
    pesa_status_date               datetime                                                                                   not null,
    source_type                    varchar(50)                                                                                not null,
    source_identifier              varchar(100)                                                                               not null,
    source_account                 varchar(100)                                                                               not null,
    source_name                    varchar(200)                                                                               not null,
    source_other_details           varchar(1000)                                                                              not null,
    sender_type                    varchar(50)                                                                                not null,
    sender_identifier              varchar(100)                                                                               not null,
    sender_account                 varchar(100)                                                                               not null,
    sender_name                    varchar(200)                                                                               not null,
    sender_other_details           varchar(1000)                                                                              not null,
    receiver_type                  varchar(50)                                                                                not null,
    receiver_identifier            varchar(100)                                                                               not null,
    receiver_account               varchar(100)                                                                               not null,
    receiver_name                  varchar(200)                                                                               not null,
    receiver_other_details         varchar(1000)                                                                              not null,
    beneficiary_type               varchar(50)                                                                                not null,
    beneficiary_identifier         varchar(100)                                                                               not null,
    beneficiary_account            varchar(100)                                                                               not null,
    beneficiary_name               varchar(200)                                                                               not null,
    beneficiary_other_details      varchar(1000)                                                                              not null,
    remark                         varchar(200)                                                                               not null,
    currency                       varchar(10)                                                                                not null,
    amount                         double(20, 5)                                                                              not null,
    pesa_type                      enum ('PESA_IN', 'PESA_OUT')                                                               not null,
    pesa_action                    enum ('C2B', 'B2C', 'B2B', 'C2C')                                                          not null,
    pesa_command                   varchar(100)                                                                               not null,
    pesa_sensitivity               enum ('NORMAL', 'PERSONAL', 'PRIVATE', 'CONFIDENTIAL')                                     not null,
    pesa_category                  varchar(30)                                                                                not null,
    pesa_priority                  int unsigned                                                                               not null,
    pesa_send_count                int unsigned                                                                               not null,
    pesa_request_application       varchar(30)                                                                                null,
    pesa_request_correlation_id    varchar(50)                                                                                null,
    pesa_source_application        varchar(30)                                                                                null,
    pesa_source_reference          varchar(50)                                                                                null,
    pesa_destination_reference     varchar(50)                                                                                null,
    pesa_xml_data                  varchar(2000)                                                                              null,
    schedule_pesa                  enum ('YES', 'NO')                                                                         not null,
    date_scheduled                 datetime                                                                                   null,
    pesa_send_integrity_hash       varchar(100)                                                                               null,
    pesa_response                  varchar(30)                                                                                null,
    pesa_response_code             int unsigned                                                                               null,
    pesa_response_description      varchar(1000)                                                                              null,
    pesa_response_xml_data         varchar(2000)                                                                              null,
    pesa_response_date             datetime                                                                                   null,
    pesa_response_integrity_hash   varchar(100)                                                                               null,
    pesa_result                    varchar(30)                                                                                null,
    pesa_result_code               int unsigned                                                                               null,
    pesa_result_description        varchar(1000)                                                                              null,
    pesa_result_xml_data           varchar(2000)                                                                              null,
    pesa_result_date               datetime                                                                                   null,
    pesa_result_integrity_hash     varchar(100)                                                                               null,
    pesa_result_submit_count       int unsigned                                                                               null,
    pesa_result_submit_status      enum ('PENDING', 'PROGRESS', 'ERROR', 'FAILED', 'COMPLETED', 'REVERSED') default 'PENDING' null,
    pesa_result_submit_description varchar(1000)                                                                              null,
    pesa_result_submit_date        datetime                                                                                   null,
    pesa_general_flag              varchar(20)                                                                                null,
    transaction_date               datetime                                                                                   not null,
    date_created                   datetime                                                                                   not null,
    constraint originator_id
    unique (originator_id)
    )
    charset = latin1;

create index index_pesa_log_amount
    on PESA_IN_pesa_log (amount);

create index index_pesa_log_beneficiary_account
    on PESA_IN_pesa_log (beneficiary_account);

create index index_pesa_log_beneficiary_identifier
    on PESA_IN_pesa_log (beneficiary_identifier);

create index index_pesa_log_beneficiary_name
    on PESA_IN_pesa_log (beneficiary_name);

create index index_pesa_log_beneficiary_type
    on PESA_IN_pesa_log (beneficiary_type);

create index index_pesa_log_currency
    on PESA_IN_pesa_log (currency);

create index index_pesa_log_date_created
    on PESA_IN_pesa_log (date_created);

create index index_pesa_log_date_scheduled
    on PESA_IN_pesa_log (date_scheduled);

create index index_pesa_log_destination_reference
    on PESA_IN_pesa_log (pesa_destination_reference);

create index index_pesa_log_originator_id
    on PESA_IN_pesa_log (originator_id);

create index index_pesa_log_pesa_action
    on PESA_IN_pesa_log (pesa_action);

create index index_pesa_log_pesa_category
    on PESA_IN_pesa_log (pesa_category);

create index index_pesa_log_pesa_charge
    on PESA_IN_pesa_log (pesa_charge);

create index index_pesa_log_pesa_charge_applied
    on PESA_IN_pesa_log (pesa_charge_applied);

create index index_pesa_log_pesa_command
    on PESA_IN_pesa_log (pesa_command);

create index index_pesa_log_pesa_general_flag
    on PESA_IN_pesa_log (pesa_general_flag);

create index index_pesa_log_pesa_id
    on PESA_IN_pesa_log (pesa_id);

create index index_pesa_log_pesa_priority
    on PESA_IN_pesa_log (pesa_priority);

create index index_pesa_log_pesa_product_id
    on PESA_IN_pesa_log (pesa_product_id);

create index index_pesa_log_pesa_response
    on PESA_IN_pesa_log (pesa_response);

create index index_pesa_log_pesa_response_code
    on PESA_IN_pesa_log (pesa_response_code);

create index index_pesa_log_pesa_response_date
    on PESA_IN_pesa_log (pesa_response_date);

create index index_pesa_log_pesa_result
    on PESA_IN_pesa_log (pesa_result);

create index index_pesa_log_pesa_result_code
    on PESA_IN_pesa_log (pesa_result_code);

create index index_pesa_log_pesa_result_date
    on PESA_IN_pesa_log (pesa_result_date);

create index index_pesa_log_pesa_result_submit_count
    on PESA_IN_pesa_log (pesa_result_submit_count);

create index index_pesa_log_pesa_result_submit_date
    on PESA_IN_pesa_log (pesa_result_submit_date);

create index index_pesa_log_pesa_result_submit_status
    on PESA_IN_pesa_log (pesa_result_submit_status);

create index index_pesa_log_pesa_send_count
    on PESA_IN_pesa_log (pesa_send_count);

create index index_pesa_log_pesa_sensitivity
    on PESA_IN_pesa_log (pesa_sensitivity);

create index index_pesa_log_pesa_status_code
    on PESA_IN_pesa_log (pesa_status_code);

create index index_pesa_log_pesa_status_date
    on PESA_IN_pesa_log (pesa_status_date);

create index index_pesa_log_pesa_type
    on PESA_IN_pesa_log (pesa_type);

create index index_pesa_log_receiver_account
    on PESA_IN_pesa_log (receiver_account);

create index index_pesa_log_receiver_identifier
    on PESA_IN_pesa_log (receiver_identifier);

create index index_pesa_log_receiver_name
    on PESA_IN_pesa_log (receiver_name);

create index index_pesa_log_receiver_type
    on PESA_IN_pesa_log (receiver_type);

create index index_pesa_log_request_application
    on PESA_IN_pesa_log (pesa_request_application);

create index index_pesa_log_request_correlation_id
    on PESA_IN_pesa_log (pesa_request_correlation_id);

create index index_pesa_log_schedule_pesa
    on PESA_IN_pesa_log (schedule_pesa);

create index index_pesa_log_sender_account
    on PESA_IN_pesa_log (sender_account);

create index index_pesa_log_sender_identifier
    on PESA_IN_pesa_log (sender_identifier);

create index index_pesa_log_sender_name
    on PESA_IN_pesa_log (sender_name);

create index index_pesa_log_sender_type
    on PESA_IN_pesa_log (sender_type);

create index index_pesa_log_server_id
    on PESA_IN_pesa_log (server_id);

create index index_pesa_log_source_account
    on PESA_IN_pesa_log (source_account);

create index index_pesa_log_source_application
    on PESA_IN_pesa_log (pesa_source_application);

create index index_pesa_log_source_identifier
    on PESA_IN_pesa_log (source_identifier);

create index index_pesa_log_source_name
    on PESA_IN_pesa_log (source_name);

create index index_pesa_log_source_reference
    on PESA_IN_pesa_log (pesa_source_reference);

create index index_pesa_log_source_type
    on PESA_IN_pesa_log (source_type);

create index index_pesa_log_transaction_date
    on PESA_IN_pesa_log (transaction_date);

create index index_pesa_log_transaction_id
    on PESA_IN_pesa_log (transaction_id);

create table if not exists PESA_OUT_pesa_log
(
    transaction_id                 bigint unsigned auto_increment
    primary key,
    originator_id                  varchar(100)                                                                               not null,
    pesa_id                        bigint unsigned                                                                            null,
    server_id                      bigint unsigned                                                                            null,
    pesa_product_id                bigint unsigned                                                                            not null,
    pesa_charge                    varchar(20)                                                                                not null,
    pesa_charge_applied            varchar(20)                                                                                null,
    pesa_status_code               int unsigned                                                                               not null,
    pesa_status_description        varchar(1000)                                                                              null,
    pesa_status_date               datetime                                                                                   not null,
    source_type                    varchar(50)                                                                                not null,
    source_identifier              varchar(100)                                                                               not null,
    source_account                 varchar(100)                                                                               not null,
    source_name                    varchar(200)                                                                               not null,
    source_other_details           varchar(1000)                                                                              not null,
    sender_type                    varchar(50)                                                                                not null,
    sender_identifier              varchar(100)                                                                               not null,
    sender_account                 varchar(100)                                                                               not null,
    sender_name                    varchar(200)                                                                               not null,
    sender_other_details           varchar(1000)                                                                              not null,
    receiver_type                  varchar(50)                                                                                not null,
    receiver_identifier            varchar(100)                                                                               not null,
    receiver_account               varchar(100)                                                                               not null,
    receiver_name                  varchar(200)                                                                               not null,
    receiver_other_details         varchar(1000)                                                                              not null,
    beneficiary_type               varchar(50)                                                                                not null,
    beneficiary_identifier         varchar(100)                                                                               not null,
    beneficiary_account            varchar(100)                                                                               not null,
    beneficiary_name               varchar(200)                                                                               not null,
    beneficiary_other_details      varchar(1000)                                                                              not null,
    remark                         varchar(200)                                                                               not null,
    currency                       varchar(10)                                                                                not null,
    amount                         double(20, 5)                                                                              not null,
    pesa_type                      enum ('PESA_IN', 'PESA_OUT')                                                               not null,
    pesa_action                    enum ('C2B', 'B2C', 'B2B', 'C2C')                                                          not null,
    pesa_command                   varchar(100)                                                                               not null,
    pesa_sensitivity               enum ('NORMAL', 'PERSONAL', 'PRIVATE', 'CONFIDENTIAL')                                     not null,
    pesa_category                  varchar(30)                                                                                not null,
    pesa_priority                  int unsigned                                                                               not null,
    pesa_send_count                int unsigned                                                                               not null,
    pesa_request_application       varchar(30)                                                                                null,
    pesa_request_correlation_id    varchar(50)                                                                                null,
    pesa_source_application        varchar(30)                                                                                null,
    pesa_source_reference          varchar(50)                                                                                null,
    pesa_destination_reference     varchar(50)                                                                                null,
    pesa_xml_data                  varchar(2000)                                                                              null,
    schedule_pesa                  enum ('YES', 'NO')                                                                         not null,
    date_scheduled                 datetime                                                                                   null,
    pesa_send_integrity_hash       varchar(100)                                                                               null,
    pesa_response                  varchar(30)                                                                                null,
    pesa_response_code             int unsigned                                                                               null,
    pesa_response_description      varchar(1000)                                                                              null,
    pesa_response_xml_data         varchar(2000)                                                                              null,
    pesa_response_date             datetime                                                                                   null,
    pesa_response_integrity_hash   varchar(100)                                                                               null,
    pesa_result                    varchar(30)                                                                                null,
    pesa_result_code               int unsigned                                                                               null,
    pesa_result_description        varchar(1000)                                                                              null,
    pesa_result_xml_data           varchar(2000)                                                                              null,
    pesa_result_date               datetime                                                                                   null,
    pesa_result_integrity_hash     varchar(100)                                                                               null,
    pesa_result_submit_count       int unsigned                                                                               null,
    pesa_result_submit_status      enum ('PENDING', 'PROGRESS', 'ERROR', 'FAILED', 'COMPLETED', 'REVERSED') default 'PENDING' null,
    pesa_result_submit_description varchar(1000)                                                                              null,
    pesa_result_submit_date        datetime                                                                                   null,
    pesa_general_flag              varchar(20)                                                                                null,
    transaction_date               datetime                                                                                   not null,
    date_created                   datetime                                                                                   not null,
    constraint originator_id
    unique (originator_id)
    )
    charset = latin1;

create index index_pesa_log_amount
    on PESA_OUT_pesa_log (amount);

create index index_pesa_log_beneficiary_account
    on PESA_OUT_pesa_log (beneficiary_account);

create index index_pesa_log_beneficiary_identifier
    on PESA_OUT_pesa_log (beneficiary_identifier);

create index index_pesa_log_beneficiary_name
    on PESA_OUT_pesa_log (beneficiary_name);

create index index_pesa_log_beneficiary_type
    on PESA_OUT_pesa_log (beneficiary_type);

create index index_pesa_log_currency
    on PESA_OUT_pesa_log (currency);

create index index_pesa_log_date_created
    on PESA_OUT_pesa_log (date_created);

create index index_pesa_log_date_scheduled
    on PESA_OUT_pesa_log (date_scheduled);

create index index_pesa_log_destination_reference
    on PESA_OUT_pesa_log (pesa_destination_reference);

create index index_pesa_log_originator_id
    on PESA_OUT_pesa_log (originator_id);

create index index_pesa_log_pesa_action
    on PESA_OUT_pesa_log (pesa_action);

create index index_pesa_log_pesa_category
    on PESA_OUT_pesa_log (pesa_category);

create index index_pesa_log_pesa_charge
    on PESA_OUT_pesa_log (pesa_charge);

create index index_pesa_log_pesa_charge_applied
    on PESA_OUT_pesa_log (pesa_charge_applied);

create index index_pesa_log_pesa_command
    on PESA_OUT_pesa_log (pesa_command);

create index index_pesa_log_pesa_general_flag
    on PESA_OUT_pesa_log (pesa_general_flag);

create index index_pesa_log_pesa_id
    on PESA_OUT_pesa_log (pesa_id);

create index index_pesa_log_pesa_priority
    on PESA_OUT_pesa_log (pesa_priority);

create index index_pesa_log_pesa_product_id
    on PESA_OUT_pesa_log (pesa_product_id);

create index index_pesa_log_pesa_response
    on PESA_OUT_pesa_log (pesa_response);

create index index_pesa_log_pesa_response_code
    on PESA_OUT_pesa_log (pesa_response_code);

create index index_pesa_log_pesa_response_date
    on PESA_OUT_pesa_log (pesa_response_date);

create index index_pesa_log_pesa_result
    on PESA_OUT_pesa_log (pesa_result);

create index index_pesa_log_pesa_result_code
    on PESA_OUT_pesa_log (pesa_result_code);

create index index_pesa_log_pesa_result_date
    on PESA_OUT_pesa_log (pesa_result_date);

create index index_pesa_log_pesa_result_submit_count
    on PESA_OUT_pesa_log (pesa_result_submit_count);

create index index_pesa_log_pesa_result_submit_date
    on PESA_OUT_pesa_log (pesa_result_submit_date);

create index index_pesa_log_pesa_result_submit_status
    on PESA_OUT_pesa_log (pesa_result_submit_status);

create index index_pesa_log_pesa_send_count
    on PESA_OUT_pesa_log (pesa_send_count);

create index index_pesa_log_pesa_sensitivity
    on PESA_OUT_pesa_log (pesa_sensitivity);

create index index_pesa_log_pesa_status_code
    on PESA_OUT_pesa_log (pesa_status_code);

create index index_pesa_log_pesa_status_date
    on PESA_OUT_pesa_log (pesa_status_date);

create index index_pesa_log_pesa_type
    on PESA_OUT_pesa_log (pesa_type);

create index index_pesa_log_receiver_account
    on PESA_OUT_pesa_log (receiver_account);

create index index_pesa_log_receiver_identifier
    on PESA_OUT_pesa_log (receiver_identifier);

create index index_pesa_log_receiver_name
    on PESA_OUT_pesa_log (receiver_name);

create index index_pesa_log_receiver_type
    on PESA_OUT_pesa_log (receiver_type);

create index index_pesa_log_request_application
    on PESA_OUT_pesa_log (pesa_request_application);

create index index_pesa_log_request_correlation_id
    on PESA_OUT_pesa_log (pesa_request_correlation_id);

create index index_pesa_log_schedule_pesa
    on PESA_OUT_pesa_log (schedule_pesa);

create index index_pesa_log_sender_account
    on PESA_OUT_pesa_log (sender_account);

create index index_pesa_log_sender_identifier
    on PESA_OUT_pesa_log (sender_identifier);

create index index_pesa_log_sender_name
    on PESA_OUT_pesa_log (sender_name);

create index index_pesa_log_sender_type
    on PESA_OUT_pesa_log (sender_type);

create index index_pesa_log_server_id
    on PESA_OUT_pesa_log (server_id);

create index index_pesa_log_source_account
    on PESA_OUT_pesa_log (source_account);

create index index_pesa_log_source_application
    on PESA_OUT_pesa_log (pesa_source_application);

create index index_pesa_log_source_identifier
    on PESA_OUT_pesa_log (source_identifier);

create index index_pesa_log_source_name
    on PESA_OUT_pesa_log (source_name);

create index index_pesa_log_source_reference
    on PESA_OUT_pesa_log (pesa_source_reference);

create index index_pesa_log_source_type
    on PESA_OUT_pesa_log (source_type);

create index index_pesa_log_transaction_date
    on PESA_OUT_pesa_log (transaction_date);

create index index_pesa_log_transaction_id
    on PESA_OUT_pesa_log (transaction_id);

create table if not exists agency_banking_receipts
(
    receipt_id       int auto_increment
    primary key,
    username         varchar(50)                        not null,
    session_id       varchar(50)                        not null,
    transaction_type varchar(50)                        not null,
    print            text                               not null,
    date_created     datetime default CURRENT_TIMESTAMP null,
    constraint agency_banking_receipts_receipt_id_uindex
    unique (receipt_id)
    );

create index agency_banking_receipts_receipt_id_index
    on agency_banking_receipts (receipt_id);

create index agency_banking_receipts_session_id_index
    on agency_banking_receipts (session_id);

create index agency_banking_receipts_username_index
    on agency_banking_receipts (username);

create table if not exists agency_banking_user_data
(
    username         varchar(50) not null,
    fingerprint_data text        null,
    constraint agency_banking_user_data_username_uindex
    unique (username),
    constraint agency_banking_user_data_username_uindex_2
    unique (username)
    );

create table if not exists cbs_analytics
(
    id                 int auto_increment
    primary key,
    function_name      varchar(255)                          not null,
    latency            bigint                                not null,
    date_created       datetime    default CURRENT_TIMESTAMP not null,
    cbs_address        varchar(255)                          null,
    middleware_address varchar(255)                          null,
    endpoint           varchar(50) default 'CBS'             not null,
    constraint cbs_analytics_id_uindex
    unique (id)
    );

create index cbs_analytics_cbs_address_index
    on cbs_analytics (cbs_address);

create index cbs_analytics_date_created_index
    on cbs_analytics (date_created);

create index cbs_analytics_function_name_index
    on cbs_analytics (function_name);

create index cbs_analytics_id_index
    on cbs_analytics (id);

create index cbs_analytics_latency_index
    on cbs_analytics (latency);

create index cbs_analytics_middleware_address_index
    on cbs_analytics (middleware_address);

create table if not exists cbs_analytics_backup
(
    id                 int         default 0                 not null,
    function_name      varchar(255)                          not null,
    latency            bigint                                not null,
    date_created       datetime    default CURRENT_TIMESTAMP not null,
    cbs_address        varchar(255)                          null,
    middleware_address varchar(255)                          null,
    endpoint           varchar(50) default 'CBS'             not null
    );

create table if not exists client_web_servers_parameters
(
    web_server_id                        bigint unsigned auto_increment
    primary key,
    web_server_name                      varchar(100)                         null,
    web_server_enabled                   enum ('YES', 'NO')                   not null,
    web_server_port                      int unsigned                         not null,
    web_server_thread_max_pool_size      int unsigned                         not null,
    web_server_thread_keep_alive_time    int unsigned                         not null,
    cryptography_enabled                 enum ('YES', 'NO')                   not null,
    cryptographic_protocol               enum ('SSLv3', 'TLSv1.1', 'TLSv1.2') not null,
    client_auth_enabled                  enum ('YES', 'NO')                   not null,
    strict_client_auth_check             enum ('YES', 'NO')                   not null,
    home_file_path                       varchar(200)                         null,
    ca_certificate_file_path             varchar(200)                         null,
    certificate_file_path                varchar(200)                         null,
    certificate_password                 varchar(500)                         null,
    certificate_password_type            enum ('CLEARTEXT', 'ENCRYPTED')      not null,
    allowed_access_sources_enabled       enum ('YES', 'NO')                   not null,
    allowed_access_sources_match_type    enum ('STRING', 'REGEX')             not null,
    max_allowed_access_sources           int default 5                        not null,
    allowed_access_sources               varchar(1000)                        null,
    restricted_access_sources_enabled    enum ('YES', 'NO')                   not null,
    restricted_access_sources_match_type enum ('STRING', 'REGEX')             not null,
    max_restricted_access_sources        int default 5                        not null,
    restricted_access_sources            varchar(1000)                        null,
    date_modified                        datetime                             not null,
    date_created                         datetime                             not null
    )
    charset = latin1;

create table if not exists client_parameters
(
    parameters_id                       bigint unsigned auto_increment
    primary key,
    parameters_urls                     varchar(5000)                                                                                 not null,
    parameters_status                   enum ('ACTIVE', 'INACTIVE')                                                                   not null,
    client_id                           bigint unsigned                                                                               not null,
    client_name                         varchar(100)                                                                                  null,
    system_user_id                      bigint unsigned                                                                               not null,
    system_username                     varchar(100)                                                                                  not null,
    system_password                     varchar(500)                                                                                  not null,
    system_password_type                enum ('CLEARTEXT', 'ENCRYPTED')                                                               not null,
    integrity_hash_enabled              enum ('YES', 'NO')                                                                            not null,
    integrity_hash_inbound_check        enum ('YES', 'NO')                                                                            not null,
    integrity_hash_outbound_check       enum ('YES', 'NO')                                                                            not null,
    integrity_secret                    varchar(500)                                                                                  not null,
    integrity_secret_type               enum ('CLEARTEXT', 'ENCRYPTED')                                                               not null,
    client_xml_parameters               text                                                                                          null,
    client_connection_timeout_seconds   int unsigned                                                                                  not null,
    client_encryption_type              enum ('NONE', 'AES_128', 'AES_256')                                                           not null,
    client_certificate_configuration    enum ('PRIVATE_CA', 'PRIVATE_CA_WITH_CLIENT_AUTH', 'PUBLIC_CA', 'PUBLIC_CA_WITH_CLIENT_AUTH') not null,
    client_cryptographic_protocol       enum ('SSLv3', 'TLSv1.1', 'TLSv1.2')                                                          not null,
    client_certificate_chain_validation enum ('YES', 'NO')                                                                            not null,
    client_hostname_verification        enum ('YES', 'NO')                                                                            not null,
    client_ca_certificate_file_path     varchar(200)                                                                                  null,
    client_certificate_file_path        varchar(200)                                                                                  null,
    client_certificate_password         varchar(500)                                                                                  null,
    client_certificate_password_type    enum ('CLEARTEXT', 'ENCRYPTED')                                                               not null,
    messaging_web_server_id             bigint unsigned                                                                               null,
    management_web_server_id            bigint unsigned                                                                               null,
    date_modified                       datetime                                                                                      not null,
    date_created                        datetime                                                                                      not null,
    constraint fkey_client_parameters_messaging_web_server_id
    foreign key (messaging_web_server_id) references client_web_servers_parameters (web_server_id)
    on update cascade,
    constraint key_client_parameters_management_web_server_id
    foreign key (management_web_server_id) references client_web_servers_parameters (web_server_id)
    on update cascade
    )
    charset = latin1;

create index index_client_parameters_client_id
    on client_parameters (client_id);

create index index_client_parameters_management_web_server_id
    on client_parameters (management_web_server_id);

create index index_client_parameters_messaging_web_server_id
    on client_parameters (messaging_web_server_id);

create index index_client_parameters_parameters_status
    on client_parameters (parameters_status);

create index index_client_parameters_system_user_id
    on client_parameters (system_user_id);

create index index_client_web_servers_parameters_enabled
    on client_web_servers_parameters (web_server_enabled);

create index index_client_web_servers_parameters_name
    on client_web_servers_parameters (web_server_name);

create table if not exists email_client_parameters
(
    parameters_id                 bigint unsigned auto_increment
    primary key,
    parameters_name               varchar(100)                             null,
    parameters_status             enum ('ACTIVE', 'INACTIVE', 'SUSPENDED') not null,
    parameters_status_date        datetime                                 not null,
    parameters_status_description varchar(100)                             null,
    connection_timeout_seconds    int unsigned                             not null,
    max_email_sender_threads      int unsigned                             not null,
    max_email_per_thread          int unsigned                             not null,
    max_email_per_batch           int unsigned                             not null,
    max_email_send_count          int unsigned                             not null,
    email_send_retry_wait_seconds int unsigned                             not null,
    email_expiry_minutes          int unsigned                             not null,
    pause_per_email_seconds       int unsigned                             not null,
    pause_new_email_seconds       int unsigned                             not null,
    refresh_parameters_minutes    int unsigned                             not null,
    restart_system_seconds        int unsigned                             not null,
    client_xml_parameters         varchar(500)                             null,
    date_modified                 datetime                                 not null,
    date_created                  datetime                                 not null
    );

create index index_email_client_parameters_connection_timeout_seconds
    on email_client_parameters (connection_timeout_seconds);

create index index_email_client_parameters_date_created
    on email_client_parameters (date_created);

create index index_email_client_parameters_date_modified
    on email_client_parameters (date_modified);

create index index_email_client_parameters_max_email_per_batch
    on email_client_parameters (max_email_per_batch);

create index index_email_client_parameters_max_email_send_count
    on email_client_parameters (max_email_send_count);

create index index_email_client_parameters_parameters_name
    on email_client_parameters (parameters_name);

create index index_email_client_parameters_parameters_status
    on email_client_parameters (parameters_status);

create index index_email_client_parameters_parameters_status_date
    on email_client_parameters (parameters_status_date);

create index index_email_client_parameters_refresh_parameters_minutes
    on email_client_parameters (refresh_parameters_minutes);

create table if not exists msg_log
(
    transaction_id                bigint unsigned auto_increment
    primary key,
    originator_id                 varchar(100)                                                                   not null,
    msg_id                        bigint unsigned                                                                null,
    server_id                     bigint unsigned                                                                null,
    msg_product_id                bigint unsigned                                                                not null,
    msg_provider_code             varchar(10)                                                                    null,
    msg_charge                    varchar(20)                                                                    not null,
    msg_charge_applied            varchar(20)                                                                    null,
    msg_mode                      enum ('SAF', 'EXPRESS')                                      default 'SAF'     not null,
    msg_status_code               int unsigned                                                                   not null,
    msg_status_description        varchar(500)                                                                   null,
    msg_status_date               datetime                                                                       not null,
    sender_type                   enum ('SENDER_ID', 'MSISDN', 'SHORT_CODE', 'APP_ID')                           not null,
    sender                        varchar(500)                                                                   not null,
    receiver_type                 enum ('MSISDN', 'SHORT_CODE', 'APP_ID')                                        not null,
    receiver                      varchar(500)                                                                   not null,
    msg                           varchar(1000)                                                                  null,
    msg_format                    enum ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO', 'FILE', 'BLOB')                       not null,
    msg_type                      enum ('MO', 'MT')                                                              not null,
    msg_command                   varchar(100)                                                                   not null,
    msg_sensitivity               enum ('NORMAL', 'PERSONAL', 'PRIVATE', 'CONFIDENTIAL')                         not null,
    msg_category                  varchar(30)                                                                    null,
    msg_priority                  int unsigned                                                                   not null,
    msg_request_application       varchar(50)                                                                    null,
    msg_request_correlation_id    varchar(50)                                                                    null,
    msg_source_application        varchar(50)                                                                    null,
    msg_source_reference          varchar(50)                                                                    null,
    msg_destination_reference     varchar(50)                                                                    null,
    msg_xml_data                  varchar(1000)                                                                  null,
    msg_send_count                int unsigned                                                                   not null,
    schedule_msg                  enum ('YES', 'NO')                                                             not null,
    date_scheduled                datetime                                                                       null,
    msg_send_integrity_hash       varchar(100)                                                                   null,
    msg_response                  varchar(50)                                                                    null,
    msg_response_code             int unsigned                                                                   null,
    msg_response_description      varchar(500)                                                                   null,
    msg_response_xml_data         varchar(1000)                                                                  null,
    msg_response_date             datetime                                                                       null,
    msg_response_integrity_hash   varchar(100)                                                                   null,
    msg_result                    varchar(50)                                                                    null,
    msg_result_code               int unsigned                                                                   null,
    msg_result_description        varchar(500)                                                                   null,
    msg_result_xml_data           varchar(1000)                                                                  null,
    msg_result_date               datetime                                                                       null,
    msg_result_integrity_hash     varchar(100)                                                                   null,
    msg_result_submit_count       int unsigned                                                                   null,
    msg_result_submit_status      enum ('PENDING', 'PROGRESS', 'ERROR', 'FAILED', 'COMPLETED') default 'PENDING' null,
    msg_result_submit_description varchar(1000)                                                                  null,
    msg_result_submit_date        datetime                                                                       null,
    msg_general_flag              varchar(20)                                                                    null,
    transaction_date              datetime                                                                       not null,
    date_created                  datetime                                                                       not null,
    constraint originator_id
    unique (originator_id)
    );

create index index_msg_log_date_created
    on msg_log (date_created);

create index index_msg_log_date_scheduled
    on msg_log (date_scheduled);

create index index_msg_log_destination_reference
    on msg_log (msg_destination_reference);

create index index_msg_log_msg_category
    on msg_log (msg_category);

create index index_msg_log_msg_charge
    on msg_log (msg_charge);

create index index_msg_log_msg_charge_applied
    on msg_log (msg_charge_applied);

create index index_msg_log_msg_command
    on msg_log (msg_command);

create index index_msg_log_msg_format
    on msg_log (msg_format);

create index index_msg_log_msg_general_flag
    on msg_log (msg_general_flag);

create index index_msg_log_msg_id
    on msg_log (msg_id);

create index index_msg_log_msg_mode
    on msg_log (msg_mode);

create index index_msg_log_msg_priority
    on msg_log (msg_priority);

create index index_msg_log_msg_product_id
    on msg_log (msg_product_id);

create index index_msg_log_msg_provider_code
    on msg_log (msg_provider_code);

create index index_msg_log_msg_response
    on msg_log (msg_response);

create index index_msg_log_msg_response_code
    on msg_log (msg_response_code);

create index index_msg_log_msg_response_date
    on msg_log (msg_response_date);

create index index_msg_log_msg_result_date
    on msg_log (msg_result_date);

create index index_msg_log_msg_result_submit_count
    on msg_log (msg_result_submit_count);

create index index_msg_log_msg_result_submit_date
    on msg_log (msg_result_submit_date);

create index index_msg_log_msg_result_submit_status
    on msg_log (msg_result_submit_status);

create index index_msg_log_msg_send_count
    on msg_log (msg_send_count);

create index index_msg_log_msg_sensitivity
    on msg_log (msg_sensitivity);

create index index_msg_log_msg_status_code
    on msg_log (msg_status_code);

create index index_msg_log_msg_status_date
    on msg_log (msg_status_date);

create index index_msg_log_msg_type
    on msg_log (msg_type);

create index index_msg_log_originator_id
    on msg_log (originator_id);

create index index_msg_log_receiver
    on msg_log (receiver);

create index index_msg_log_receiver_type
    on msg_log (receiver_type);

create index index_msg_log_schedule_msg
    on msg_log (schedule_msg);

create index index_msg_log_sender
    on msg_log (sender);

create index index_msg_log_sender_type
    on msg_log (sender_type);

create index index_msg_log_server_id
    on msg_log (server_id);

create index index_msg_log_source_reference
    on msg_log (msg_source_reference);

create index index_msg_log_transaction_date
    on msg_log (transaction_date);

create table if not exists sender_email_parameters
(
    sender_email_id            bigint unsigned auto_increment
    primary key,
    sender_email_name          varchar(200)                    not null,
    sender_email_address       varchar(200)                    not null,
    sender_email_username      varchar(200)                    not null,
    sender_email_password      varchar(500)                    not null,
    sender_email_password_type enum ('CLEARTEXT', 'ENCRYPTED') not null,
    parameter_status           enum ('ACTIVE', 'INACTIVE')     not null,
    receiver_email_addresses   text                            null,
    email_signature            text                            null,
    delete_attachment          enum ('YES', 'NO')              not null,
    email_client_configuration text                            not null,
    date_created               datetime                        not null,
    date_modified              datetime                        not null
    );

create table if not exists email_log
(
    email_id                 bigint unsigned auto_increment
    primary key,
    originator_id            varchar(100)                                           not null,
    status_code              int                                                    not null,
    status_name              varchar(50)                                            not null,
    status_description       varchar(500)                                           null,
    status_date              datetime                                               not null,
    sender_email_id          bigint unsigned                                        not null,
    sender_email_name        varchar(200)                                           not null,
    sender_email_address     varchar(200)                                           not null,
    receiver_email_addresses text                                                   not null,
    email_type               enum ('INBOUND_EMAIL', 'OUTBOUND_EMAIL')               not null,
    email_subject            varchar(200)                                           not null,
    email_content_type       enum ('TEXT', 'HTML')                                  not null,
    email_message_body       mediumtext                                             not null,
    attachment               enum ('YES', 'NO')                                     not null,
    delete_attachment        enum ('YES', 'NO')                                     not null,
    attachment_links         varchar(500)                                           null,
    sensitivity              enum ('NORMAL', 'PERSONAL', 'PRIVATE', 'CONFIDENTIAL') not null,
    category                 varchar(50)                                            null,
    priority                 int unsigned                                           not null,
    send_count               int unsigned                                           not null,
    request_application      varchar(50)                                            null,
    request_correlation_id   varchar(50)                                            null,
    source_application       varchar(50)                                            null,
    source_reference         varchar(50)                                            null,
    schedule_email           enum ('YES', 'NO')                                     not null,
    date_scheduled           datetime                                               null,
    send_integrity_hash      varchar(100)                                           null,
    general_flag             varchar(20)                                            null,
    date_created             datetime                                               not null,
    constraint originator_id
    unique (originator_id),
    constraint email_log_ibfk_1
    foreign key (sender_email_id) references sender_email_parameters (sender_email_id)
    on update cascade
    );

create index index_email_log_attachment
    on email_log (attachment);

create index index_email_log_category
    on email_log (category);

create index index_email_log_date_created
    on email_log (date_created);

create index index_email_log_date_scheduled
    on email_log (date_scheduled);

create index index_email_log_delete_attachment
    on email_log (delete_attachment);

create index index_email_log_email_type
    on email_log (email_type);

create index index_email_log_general_flag
    on email_log (general_flag);

create index index_email_log_originator_id
    on email_log (originator_id);

create index index_email_log_priority
    on email_log (priority);

create index index_email_log_request_correlation_id
    on email_log (request_correlation_id);

create index index_email_log_schedule_msg
    on email_log (schedule_email);

create index index_email_log_send_count
    on email_log (send_count);

create index index_email_log_sender_email_address
    on email_log (sender_email_address);

create index index_email_log_sender_email_id
    on email_log (sender_email_id);

create index index_email_log_sender_email_name
    on email_log (sender_email_name);

create index index_email_log_sensitivity
    on email_log (sensitivity);

create index index_email_log_source_reference
    on email_log (source_reference);

create index index_email_log_status_code
    on email_log (status_code);

create index index_email_log_status_date
    on email_log (status_date);

create index index_email_log_status_name
    on email_log (status_name);

create index index_email_client_parameters_date_created
    on sender_email_parameters (date_created);

create index index_email_client_parameters_date_modified
    on sender_email_parameters (date_modified);

create index index_email_client_parameters_delete_attachment
    on sender_email_parameters (delete_attachment);

create index index_email_client_parameters_parameter_status
    on sender_email_parameters (parameter_status);

create index index_email_client_parameters_sender_email_address
    on sender_email_parameters (sender_email_address);

create index index_email_client_parameters_sender_email_password_type
    on sender_email_parameters (sender_email_password_type);

create index index_email_client_parameters_sender_email_username
    on sender_email_parameters (sender_email_username);

create table if not exists service_providers
(
    provider_code               varchar(10)                              not null
    primary key,
    provider_name               varchar(200)                             null,
    provider_type               enum ('BANK', 'MNO', 'SERVICE_PROVIDER') not null,
    provider_status             enum ('ACTIVE', 'INACTIVE')              not null,
    provider_status_description varchar(200)                             null,
    country_code                varchar(10)                              not null,
    physical_address            varchar(500)                             not null,
    postal_address              varchar(500)                             not null,
    email_addresses             varchar(1000)                            not null,
    phone_numbers               varchar(1000)                            not null,
    general_flag                varchar(20)                              null,
    other_details               varchar(1000)                            not null,
    date_created                datetime                                 not null,
    date_modified               datetime                                 not null,
    integrity_hash              varchar(100)                             null,
    provider_sequence           int unsigned default '0'                 not null
    )
    charset = latin1;

create table if not exists service_provider_accounts
(
    provider_account_code      varchar(10)                                                                                                  not null
    primary key,
    provider_code              varchar(10)                                                                                                  not null,
    account_type               enum ('ACCOUNT_NO', 'BANK_CODE', 'BANK_SHORT_CODE', 'SHORT_CODE', 'UTILITY_CODE', 'TILL_NUMBER', 'SKY_CODE') not null,
    account_type_tag           varchar(50)                                                                                                  not null,
    account_identifier         varchar(200)                                                                                                 not null,
    account_name               varchar(200)                                                                                                 not null,
    account_short_tag          varchar(30)                                                                                                  not null,
    account_long_tag           varchar(50)                                                                                                  not null,
    account_status             enum ('ACTIVE', 'INACTIVE')                                                                                  not null,
    account_status_description varchar(200)                                                                                                 null,
    currency_code              varchar(10)                                                                                                  not null,
    min_transaction_amount     double(20, 5)                                                                                                not null,
    max_transaction_amount     double(20, 5)                                                                                                not null,
    c2b_capability             enum ('YES', 'NO')                                                                                           not null,
    c2c_capability             enum ('YES', 'NO')                                                                                           not null,
    b2c_capability             enum ('YES', 'NO')                                                                                           not null,
    b2b_capability             enum ('YES', 'NO')                                                                                           not null,
    general_flag               varchar(20)                                                                                                  null,
    other_details              varchar(1000)                                                                                                not null,
    date_created               datetime                                                                                                     not null,
    date_modified              datetime                                                                                                     not null,
    integrity_hash             varchar(100)                                                                                                 null,
    provider_account_sequence  int unsigned default '0'                                                                                     not null,
    constraint service_provider_accounts_ibfk_1
    foreign key (provider_code) references service_providers (provider_code)
    on update cascade
    )
    charset = latin1;

create index index_service_provider_accounts_account_identifier
    on service_provider_accounts (account_identifier);

create index index_service_provider_accounts_account_identifier_type
    on service_provider_accounts (account_type);

create index index_service_provider_accounts_account_long_tag
    on service_provider_accounts (account_long_tag);

create index index_service_provider_accounts_account_name
    on service_provider_accounts (account_name);

create index index_service_provider_accounts_account_short_tag
    on service_provider_accounts (account_short_tag);

create index index_service_provider_accounts_account_status
    on service_provider_accounts (account_status);

create index index_service_provider_accounts_account_type_tag
    on service_provider_accounts (account_type_tag);

create index index_service_provider_accounts_b2c_capability
    on service_provider_accounts (b2c_capability);

create index index_service_provider_accounts_c2b_capability
    on service_provider_accounts (c2b_capability);

create index index_service_provider_accounts_c2c_capability
    on service_provider_accounts (c2c_capability);

create index index_service_provider_accounts_currency_code
    on service_provider_accounts (currency_code);

create index index_service_provider_accounts_date_modified
    on service_provider_accounts (date_modified);

create index index_service_provider_accounts_general_flag
    on service_provider_accounts (general_flag);

create index index_service_provider_accounts_payment_capability
    on service_provider_accounts (b2b_capability);

create index index_service_provider_accounts_provider_account_sequence
    on service_provider_accounts (provider_account_sequence);

create index index_service_provider_accounts_provider_code
    on service_provider_accounts (provider_code);

create index index_service_providers_country_code
    on service_providers (country_code);

create index index_service_providers_date_created
    on service_providers (date_created);

create index index_service_providers_date_modified
    on service_providers (date_modified);

create index index_service_providers_general_flag
    on service_providers (general_flag);

create index index_service_providers_provider_name
    on service_providers (provider_name);

create index index_service_providers_provider_sequence
    on service_providers (provider_sequence);

create index index_service_providers_provider_status
    on service_providers (provider_status);

create table if not exists temporary_otps
(
    otp_key      varchar(255) not null,
    otp_value    varchar(255) not null,
    date_created datetime     not null,
    otp_ttl      datetime     not null
    );

create index temporary_otps_date_created_index
    on temporary_otps (date_created);

create index temporary_otps_otp_key_index
    on temporary_otps (otp_key);

create index temporary_otps_otp_ttl_index
    on temporary_otps (otp_ttl);

create index temporary_otps_otp_value_index
    on temporary_otps (otp_value);

create table if not exists temporary_user_data
(
    id           int auto_increment,
    datum_key    varchar(255)                       null,
    datum_value  text                               null,
    datum_hash   varchar(255)                       null,
    date_created datetime default CURRENT_TIMESTAMP not null,
    constraint temporary_user_data_id_uindex
    unique (id)
    );

create index temporary_user_data_date_created_index
    on temporary_user_data (date_created desc);

create index temporary_user_data_datum_hash_index
    on temporary_user_data (datum_hash);

create index temporary_user_data_datum_key_index
    on temporary_user_data (datum_key);

create table if not exists user_accounts
(
    user_account_id            bigint unsigned auto_increment
    primary key,
    user_identifier_type       enum ('MSISDN', 'ACCOUNT_NO') not null,
    user_identifier            varchar(200)                  null,
    provider_account_code      varchar(10)                   not null,
    account_identifier_type    enum ('MSISDN', 'ACCOUNT_NO') not null,
    account_identifier         varchar(200)                  null,
    account_name               varchar(200)                  null,
    account_status             enum ('ACTIVE', 'INACTIVE')   not null,
    account_status_description varchar(200)                  null,
    other_details              varchar(1000)                 not null,
    date_created               datetime                      not null,
    date_modified              datetime                      not null,
    integrity_hash             varchar(100)                  null,
    constraint user_identifier
    unique (user_identifier, account_identifier),
    constraint fk_user_accounts_service_provider_accounts
    foreign key (provider_account_code) references service_provider_accounts (provider_account_code)
    on update cascade
    )
    charset = latin1;

create index index_user_accounts_account_identifier
    on user_accounts (account_identifier);

create index index_user_accounts_account_identifier_type
    on user_accounts (account_identifier_type);

create index index_user_accounts_account_name
    on user_accounts (account_name);

create index index_user_accounts_account_status
    on user_accounts (account_status);

create index index_user_accounts_date_created
    on user_accounts (date_created);

create index index_user_accounts_date_modified
    on user_accounts (date_modified);

create index index_user_accounts_provider_account_code
    on user_accounts (provider_account_code);

create index index_user_accounts_user_identifier
    on user_accounts (user_identifier);

create index index_user_accounts_user_identifier_type
    on user_accounts (user_identifier_type);

create table if not exists user_accounts_audit_trail
(
    audit_trail_id             bigint unsigned auto_increment
    primary key,
    audit_trail_action         enum ('ADD', 'EDIT', 'REMOVE') not null,
    user_account_id            bigint unsigned                not null,
    user_identifier_type       enum ('MSISDN', 'ACCOUNT_NO')  not null,
    user_identifier            varchar(200)                   null,
    provider_account_code      bigint unsigned                not null,
    account_identifier_type    enum ('MSISDN', 'ACCOUNT_NO')  not null,
    account_identifier         varchar(200)                   null,
    account_name               varchar(200)                   null,
    account_status             enum ('ACTIVE', 'INACTIVE')    not null,
    account_status_description varchar(200)                   null,
    other_details              varchar(1000)                  not null,
    date_created               datetime                       not null,
    date_modified              datetime                       not null,
    audit_trail_date           datetime                       not null,
    integrity_hash             varchar(100)                   null
    )
    charset = latin1;

create index index_user_accounts_audit_trail_account_identifier
    on user_accounts_audit_trail (account_identifier);

create index index_user_accounts_audit_trail_account_identifier_type
    on user_accounts_audit_trail (account_identifier_type);

create index index_user_accounts_audit_trail_account_name
    on user_accounts_audit_trail (account_name);

create index index_user_accounts_audit_trail_account_status
    on user_accounts_audit_trail (account_status);

create index index_user_accounts_audit_trail_action
    on user_accounts_audit_trail (audit_trail_action);

create index index_user_accounts_audit_trail_date_created
    on user_accounts_audit_trail (date_created);

create index index_user_accounts_audit_trail_date_modified
    on user_accounts_audit_trail (date_modified);

create index index_user_accounts_audit_trail_provider_account_code
    on user_accounts_audit_trail (provider_account_code);

create index index_user_accounts_audit_trail_user_account_id
    on user_accounts_audit_trail (user_account_id);

create index index_user_accounts_audit_trail_user_identifier
    on user_accounts_audit_trail (user_identifier);

create index index_user_accounts_audit_trail_user_identifier_type
    on user_accounts_audit_trail (user_identifier_type);

/*--------------------------------------------------------------------------------------------------------------------*/
INSERT INTO client_web_servers_parameters (web_server_name, web_server_enabled, web_server_port, web_server_thread_max_pool_size, web_server_thread_keep_alive_time, cryptography_enabled, cryptographic_protocol, client_auth_enabled, strict_client_auth_check, home_file_path, ca_certificate_file_path, certificate_file_path, certificate_password, certificate_password_type, allowed_access_sources_enabled, allowed_access_sources_match_type, max_allowed_access_sources, allowed_access_sources, restricted_access_sources_enabled, restricted_access_sources_match_type, max_restricted_access_sources, restricted_access_sources, date_modified, date_created) VALUES ('MSG Messaging Web Server', 'YES', 8091, 100, 20, 'NO', 'TLSv1.2', 'NO', 'NO', '/msg/messaging.aspx', '', '', '6SA+aMakGXsPYKWt2octzA==', 'ENCRYPTED', 'YES', 'REGEX', 20, '^((102.220.116)|(102.220.117)).((20[8-9])|(21[0-5]))$', 'NO', 'STRING', 5, '127.0.0.1', '2018-06-21 03:56:01', '2018-06-21 01:50:50');
INSERT INTO client_web_servers_parameters (web_server_name, web_server_enabled, web_server_port, web_server_thread_max_pool_size, web_server_thread_keep_alive_time, cryptography_enabled, cryptographic_protocol, client_auth_enabled, strict_client_auth_check, home_file_path, ca_certificate_file_path, certificate_file_path, certificate_password, certificate_password_type, allowed_access_sources_enabled, allowed_access_sources_match_type, max_allowed_access_sources, allowed_access_sources, restricted_access_sources_enabled, restricted_access_sources_match_type, max_restricted_access_sources, restricted_access_sources, date_modified, date_created) VALUES ('MSG Management Web Server', 'YES', 8092, 100, 20, 'NO', 'TLSv1.1', 'NO', 'NO', '/msg/mgt.aspx', '', '', '6SA+aMakGXsPYKWt2octzA==', 'ENCRYPTED', 'YES', 'REGEX', 5, '^((102.220.116)|(102.220.117)).((20[8-9])|(21[0-5]))$', 'NO', 'STRING', 5, '127.0.0.1', '2018-06-21 03:56:01', '2018-06-21 01:51:06');
INSERT INTO client_web_servers_parameters (web_server_name, web_server_enabled, web_server_port, web_server_thread_max_pool_size, web_server_thread_keep_alive_time, cryptography_enabled, cryptographic_protocol, client_auth_enabled, strict_client_auth_check, home_file_path, ca_certificate_file_path, certificate_file_path, certificate_password, certificate_password_type, allowed_access_sources_enabled, allowed_access_sources_match_type, max_allowed_access_sources, allowed_access_sources, restricted_access_sources_enabled, restricted_access_sources_match_type, max_restricted_access_sources, restricted_access_sources, date_modified, date_created) VALUES ('PESA Messaging Web Server', 'YES', 9091, 100, 20, 'NO', 'TLSv1.2', 'NO', 'NO', '/pesa/response.aspx', 'D:\\certificates\\CA\\skycacert.crt', 'D:\\certificates\\CA\\portal.skyworld.co.ke.pfx', '6SA+aMakGXsPYKWt2octzA==', 'ENCRYPTED', 'YES', 'REGEX', 20, '^((102.220.116)|(102.220.117)).((20[8-9])|(21[0-5]))$', 'NO', 'STRING', 5, '127.0.0.1', '2018-06-21 04:50:05', '2018-06-21 02:05:28');
INSERT INTO client_web_servers_parameters (web_server_name, web_server_enabled, web_server_port, web_server_thread_max_pool_size, web_server_thread_keep_alive_time, cryptography_enabled, cryptographic_protocol, client_auth_enabled, strict_client_auth_check, home_file_path, ca_certificate_file_path, certificate_file_path, certificate_password, certificate_password_type, allowed_access_sources_enabled, allowed_access_sources_match_type, max_allowed_access_sources, allowed_access_sources, restricted_access_sources_enabled, restricted_access_sources_match_type, max_restricted_access_sources, restricted_access_sources, date_modified, date_created) VALUES ('PESA Management Web Server', 'YES', 9092, 100, 20, 'NO', 'TLSv1.1', 'NO', 'NO', '/pesa/mgt.aspx', 'D:\\certificates\\CA\\yourcacert.crt', 'D:\\certificates\\CA\\yourdomain.co.ke.pfx', '6SA+aMakGXsPYKWt2octzA==', 'ENCRYPTED', 'YES', 'REGEX', 5, '^((102.220.116)|(102.220.117)).((20[8-9])|(21[0-5]))$', 'NO', 'STRING', 5, '127.0.0.1', '2018-06-21 04:50:05', '2018-06-21 02:05:38');
INSERT INTO client_web_servers_parameters (web_server_name, web_server_enabled, web_server_port, web_server_thread_max_pool_size, web_server_thread_keep_alive_time, cryptography_enabled, cryptographic_protocol, client_auth_enabled, strict_client_auth_check, home_file_path, ca_certificate_file_path, certificate_file_path, certificate_password, certificate_password_type, allowed_access_sources_enabled, allowed_access_sources_match_type, max_allowed_access_sources, allowed_access_sources, restricted_access_sources_enabled, restricted_access_sources_match_type, max_restricted_access_sources, restricted_access_sources, date_modified, date_created) VALUES ('USSD Messaging Web Server', 'YES', 9093, 100, 20, 'NO', 'TLSv1.2', 'NO', 'NO', '/mbanking/request.aspx', 'D:\\certificates\\CA\\skycacert.crt', 'D:\\certificates\\CA\\portal.skyworld.co.ke.pfx', 'n/KIpls1LPZQlbDDv8HPOw==', 'ENCRYPTED', 'NO', 'REGEX', 20, '^((102.220.116)|(102.220.117)).((20[8-9])|(21[0-5]))$', 'NO', 'STRING', 5, '127.0.0.1', '2018-06-21 23:58:15', '2018-06-21 02:05:28');
INSERT INTO client_web_servers_parameters (web_server_name, web_server_enabled, web_server_port, web_server_thread_max_pool_size, web_server_thread_keep_alive_time, cryptography_enabled, cryptographic_protocol, client_auth_enabled, strict_client_auth_check, home_file_path, ca_certificate_file_path, certificate_file_path, certificate_password, certificate_password_type, allowed_access_sources_enabled, allowed_access_sources_match_type, max_allowed_access_sources, allowed_access_sources, restricted_access_sources_enabled, restricted_access_sources_match_type, max_restricted_access_sources, restricted_access_sources, date_modified, date_created) VALUES ('USSD Management Web Server', 'YES', 9094, 100, 20, 'NO', 'TLSv1.1', 'NO', 'NO', '/mbanking/mgt.aspx', 'D:\\certificates\\CA\\yourcacert.crt', 'D:\\certificates\\CA\\yourdomain.co.ke.pfx', 'n/KIpls1LPZQlbDDv8HPOw==', 'ENCRYPTED', 'YES', 'REGEX', 5, '^((102.220.116)|(102.220.117)).((20[8-9])|(21[0-5]))$', 'NO', 'STRING', 5, '127.0.0.1', '2018-06-21 23:58:15', '2018-06-21 02:05:38');

INSERT INTO client_parameters (parameters_urls, parameters_status, client_id, client_name, system_user_id, system_username, system_password, system_password_type, integrity_hash_enabled, integrity_hash_inbound_check, integrity_hash_outbound_check, integrity_secret, integrity_secret_type, client_xml_parameters, client_connection_timeout_seconds, client_encryption_type, client_certificate_configuration, client_cryptographic_protocol, client_certificate_chain_validation, client_hostname_verification, client_ca_certificate_file_path, client_certificate_file_path, client_certificate_password, client_certificate_password_type, messaging_web_server_id, management_web_server_id, date_modified, date_created) VALUES ('http://102.220.117.210:8088/msg_gateway_interface/global/sendmsg_client_parameters', 'ACTIVE', 2, 'TEST MSG Client', 34, 'mbanking_test@skyworld.co.ke', '4pmNUKmvqDLnAFM/2mT1M+ktBjjtXqv9bQGxY0pAm8w=', 'ENCRYPTED', 'NO', 'YES', 'YES', 'es3sAT+qcLLa5z8FgKtxFg==', 'ENCRYPTED', '<OTHER_DETAILS>
    <CUSTOM_PARAMETERS>
        <SMS>
            <MT>
                <PRODUCT_ID>122</PRODUCT_ID>
                <SENDER>SkyWorld</SENDER>
            </MT>
        </SMS>
    </CUSTOM_PARAMETERS>
    <MSG_PARAMETERS>
        <MT_MSG STATUS="ACTIVE">
            <TABLE_NAME>[DESACCO LTD].dbo.[DEFENCE SACCO LTD$Sky SMS Messages]</TABLE_NAME>
            <SEND_MSG>
                <SERVICE_URLS>
                    <SERVICE_URL>http://102.220.117.210:8088/msg_gateway_interface/global/sendmsg</SERVICE_URL>
                    <SERVICE_URL>http://102.220.117.210:8088/msg_gateway_interface/global/sendmsg</SERVICE_URL>
                    <SERVICE_URL>http://102.220.117.210:8088/msg_gateway_interface/global/sendmsg</SERVICE_URL>
                </SERVICE_URLS>
            </SEND_MSG>
        </MT_MSG>
        <MO_MSG MODE="ONLINE" STATUS="ACTIVE"> <!-- MODE=ONLINE/OFFLINE/-->
            <TABLE_NAME>[DESACCO LTD].dbo.[DEFENCE SACCO LTD$Sky SMS Messages]</TABLE_NAME>
            <ONLINE_MSG>Dear Customer, Your SMS has been received. We shall respond back shortly.</ONLINE_MSG>
        </MO_MSG>
    </MSG_PARAMETERS>
    <LOGS_DATABASE_PARAMETERS STATUS="ACTIVE">
        <TYPE>MicrosoftSQL</TYPE>
        <HOSTNAME>192.168.50.4\\MSSQLSERVERVERII</HOSTNAME>
        <PORT>1434</PORT>
        <NAME>DESACCO LTD</NAME>
        <USERNAME>SMSUser</USERNAME>
        <PASSWORD TYPE="ENCRYPTED">ZMDma3afOIlzp5TPlspYUJguyNSbePeGalxzneX1ViX6NvKR7LkXjgEuiJJaI+XrEFTgeiK8ogiqI9R2XH/ugst1BJTBsi9WzhIVqRxw4A8IHpcLq9WI3BJ41htg2DXoaMiKANCCResy3VU8nFU0t96DYF4ESng5TY2WvINcXOMxMnUwjhCWANHrI1bzxniZ/kLUywYS+Yf+HalmJ8EtucYfzjbA65Wc4ZOg89apCYqfyIo82RFrotAakVgcZ3Wm+i0kddl6df7/4Zm3C2coZjJqmLCcpCp2tVebO8W0DnG7+nbNPpoG4Gr1yqqnkbEkq5hGv1TQTlH4hbnfFmel2A==</PASSWORD>
    </LOGS_DATABASE_PARAMETERS>
</OTHER_DETAILS>', 30, 'AES_128', 'PRIVATE_CA_WITH_CLIENT_AUTH', 'TLSv1.2', 'YES', 'YES', '/home/ps-gw.skyworld.co.ke.crt', '/home/mintel.pfx', '4RoD2JFvJ7I2WI62/e8l0EnBNgzI0OZ4z1lybtqTYic=', 'ENCRYPTED', 1, 2, '2022-02-16 12:46:42', '2018-06-21 03:43:54');
INSERT INTO client_parameters (parameters_urls, parameters_status, client_id, client_name, system_user_id, system_username, system_password, system_password_type, integrity_hash_enabled, integrity_hash_inbound_check, integrity_hash_outbound_check, integrity_secret, integrity_secret_type, client_xml_parameters, client_connection_timeout_seconds, client_encryption_type, client_certificate_configuration, client_cryptographic_protocol, client_certificate_chain_validation, client_hostname_verification, client_ca_certificate_file_path, client_certificate_file_path, client_certificate_password, client_certificate_password_type, messaging_web_server_id, management_web_server_id, date_modified, date_created) VALUES ('http://102.220.117.209:8088/pesa_gateway_interface/global/sendpesa_client_parameters', 'ACTIVE', 2, 'PESA OUT Client', 2, 'mbanking_test@skyworld.co.ke', '4pmNUKmvqDLnAFM/2mT1M+ktBjjtXqv9bQGxY0pAm8w=', 'ENCRYPTED', 'YES', 'YES', 'YES', 'es3sAT+qcLLa5z8FgKtxFg==', 'ENCRYPTED', '<OTHER_DETAILS>
    <CUSTOM_PARAMETERS>
        <SAFARICOM>
            <MPESA_B2C>
                <PRODUCT_ID>11</PRODUCT_ID>
                <SENDER_IDENTIFIER>531800</SENDER_IDENTIFIER>
                <SENDER_ACCOUNT>531800</SENDER_ACCOUNT>
                <SENDER_NAME>SKY WORLD LTD - B2C</SENDER_NAME>
            </MPESA_B2C>
            <MPESA_C2B>
                <PRODUCT_ID>9</PRODUCT_ID>
                <SENDER_IDENTIFIER>829800</SENDER_IDENTIFIER>
                <SENDER_ACCOUNT>829800</SENDER_ACCOUNT>
                <SENDER_NAME>SKY WORLD LTD - C2B</SENDER_NAME>
            </MPESA_C2B>
            <MPESA_B2B>
                <PRODUCT_ID>9</PRODUCT_ID>
                <SENDER_IDENTIFIER>829800</SENDER_IDENTIFIER>
                <SENDER_ACCOUNT>829800</SENDER_ACCOUNT>
                <SENDER_NAME>SKY WORLD LTD - B2B</SENDER_NAME>
            </MPESA_B2B>
        </SAFARICOM>
        <GLOBAL>
            <AIRTIME>
                <PRODUCT_ID>89</PRODUCT_ID>
                <SENDER_IDENTIFIER>1010029</SENDER_IDENTIFIER>
                <SENDER_ACCOUNT>1010029</SENDER_ACCOUNT>
                <SENDER_NAME>SKY WORLD LTD - AIRTIME</SENDER_NAME>
            </AIRTIME>
        </GLOBAL>
    </CUSTOM_PARAMETERS>
    <PESA_PARAMETERS>
        <PESA_OUT STATUS="ACTIVE">
            <TABLE_NAME>PESA_OUT_pesa_log</TABLE_NAME>
            <SEND_PESA>
                <SERVICE_URLS>
                    <SERVICE_URL>http://102.220.117.209:8088/pesa_gateway_interface/global/sendpesa</SERVICE_URL>
                    <SERVICE_URL>http://102.220.117.209:8088/pesa_gateway_interface/global/sendpesa</SERVICE_URL>
                    <SERVICE_URL>http://102.220.117.209:8088/pesa_gateway_interface/global/sendpesa</SERVICE_URL>
                </SERVICE_URLS>
            </SEND_PESA>
        </PESA_OUT>
        <PESA_IN>
            <TABLE_NAME>PESA_IN_pesa_log</TABLE_NAME>
            <PAYMENT_REQUEST>
                <SERVICE_URLS>
                    <SERVICE_URL>http://102.220.117.209:8088/pesa_gateway_interface/global/requestpesa</SERVICE_URL>
                    <SERVICE_URL>http://102.220.117.209:8088/pesa_gateway_interface/global/requestpesa</SERVICE_URL>
                    <SERVICE_URL>http://102.220.117.209:8088/pesa_gateway_interface/global/requestpesa</SERVICE_URL>
                </SERVICE_URLS>
            </PAYMENT_REQUEST>
        </PESA_IN>
    </PESA_PARAMETERS>
</OTHER_DETAILS>', 30, 'AES_128', 'PRIVATE_CA_WITH_CLIENT_AUTH', 'TLSv1.2', 'YES', 'YES', 'D:\\certificates\\CA\\skycacert.crt', 'D:\\certificates\\CA\\portal.skyworld.co.ke.pfx', 'es3sAT+qcLLa5z8FgKtxFg==', 'ENCRYPTED', 3, 4, '2022-02-16 12:46:10', '2018-06-21 04:28:34');
INSERT INTO client_parameters (parameters_urls, parameters_status, client_id, client_name, system_user_id, system_username, system_password, system_password_type, integrity_hash_enabled, integrity_hash_inbound_check, integrity_hash_outbound_check, integrity_secret, integrity_secret_type, client_xml_parameters, client_connection_timeout_seconds, client_encryption_type, client_certificate_configuration, client_cryptographic_protocol, client_certificate_chain_validation, client_hostname_verification, client_ca_certificate_file_path, client_certificate_file_path, client_certificate_password, client_certificate_password_type, messaging_web_server_id, management_web_server_id, date_modified, date_created) VALUES ('http://102.220.117.211:8088', 'ACTIVE', 0, 'USSD Client', 2, 'mbanking_test@skyworld.co.ke', '7111udf3zyeBq39WnIEZ5+Y33cNBG4DTEifAOPOExS4=', 'ENCRYPTED', 'NO', 'NO', 'NO', 'pnhB1kYi5tIQ2Y8zIjDvSg==', 'ENCRYPTED', '<OTHER_DETAILS>
    <CUSTOM_PARAMETERS>
        <MAPP_ACTIVATION_CODE LENGTH=''6'' TTL=''1200''/> <!-- TTL Time in Seconds -->
        <POST_TRANSACTIONS_INTERVAL>15</POST_TRANSACTIONS_INTERVAL> <!-- Time in Seconds -->
        <SERVICE_CONFIGS>
            <AMOUNT_LIMITS>
                <CASH_WITHDRAWAL>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>20</MAX_AMOUNT>
                </CASH_WITHDRAWAL>
                <AIRTIME_PURCHASE>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </AIRTIME_PURCHASE>
                <PAY_BILL>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </PAY_BILL>
                <EXTERNAL_FUNDS_TRANSFER>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </EXTERNAL_FUNDS_TRANSFER>
                <INTERNAL_FUNDS_TRANSFER>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>10000</MAX_AMOUNT>
                </INTERNAL_FUNDS_TRANSFER>
                <DEPOSIT>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>20</MAX_AMOUNT>
                </DEPOSIT>
                <APPLY_LOAN>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </APPLY_LOAN>
                <PAY_LOAN>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </PAY_LOAN>
            </AMOUNT_LIMITS>
            <CONFIGURATION>
                <CASH_WITHDRAWAL>
                    <CHANNELS>
                        <CHANNEL NAME="M-PESA" LABEL="M-PESA" STATUS="ACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
                        <CHANNEL NAME="ATM" LABEL="ATM" STATUS="ACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
                        <CHANNEL NAME="AGENT" LABEL="AGENT" STATUS="INACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
                    </CHANNELS>
                </CASH_WITHDRAWAL>
            </CONFIGURATION>
        </SERVICE_CONFIGS>
    </CUSTOM_PARAMETERS>
    <AUTHENTICATION_PARAMETERS>
        <PASSWORD_ATTEMPT_PARAMETERS>
            <ATTEMPTS NAME=''DEFAULT_SUSPEND'' ACTION=''SUSPEND'' STEP=''2'' DURATION=''30'' UNIT=''DAY'' NOTIFY=''YES'' >
                <!-- <ATTEMPTS NAME=''DEFAULT_SUSPEND/DEFAULT_WARN/DEFAULT_LOCK'' ACTION=''SUSPEND/WARN/LOCK'' STEP=''3'' DURATION=''24'' UNIT=''HOUR'' NOTIFY=''YES'' > -->
                <!--<ATTEMPT ACTION=''NONE/WARN/SUSPEND/LOCK'' NOTIFY=''YES/NO''>3</ATTEMPT>-->
                <ATTEMPT NAME=''FIRST_WARNING'' ACTION=''WARN'' NOTIFY=''NO''>2</ATTEMPT>
                <ATTEMPT NAME=''FIRST_SUSPENSION'' ACTION=''SUSPEND'' DURATION=''60'' UNIT=''MINUTE'' NOTIFY=''YES''>4</ATTEMPT> <!-- UNIT=''SECOND/MINUTE/HOUR/DAY'' -->
                <ATTEMPT NAME=''SECOND_WARNING'' ACTION=''WARN'' NOTIFY=''NO''>5</ATTEMPT>
                <ATTEMPT NAME=''SECOND_SUSPENSION'' ACTION=''SUSPEND'' DURATION=''24'' UNIT=''HOUR'' NOTIFY=''NO''>6</ATTEMPT>
                <ATTEMPT NAME=''THIRD_WARNING'' ACTION=''WARN'' NOTIFY=''NO''>7</ATTEMPT>
            </ATTEMPTS>
            <NOTIFY STATUS=''ACTIVE''>
                <!-- STATUS=''ACTIVE/INACTIVE''-->
                <GROUP NAME=''SUPPORT_TEAM_MSISDN'' TYPE=''MSISDN'' ATTEMPTS = ''THIRD_WARNING,DEFAULT_SUSPEND'' STATUS=''ACTIVE''>
                    <!-- TYPE=''MSISDN/APP_ID/EMAIL_ADDRESS'' STATUS=''ACTIVE/INACTIVE'' ATTEMPTS=''ATTEMPT NAMEs separated with Comma.''-->
                    <CONTACTS>
                        <CONTACT IDENTIFIER=''254721913958'' STATUS=''ACTIVE''>Moses Magero</CONTACT>
                        <!-- STATUS=''ACTIVE/INACTIVE''-->
                        <CONTACT IDENTIFIER=''254706405989'' STATUS=''ACTIVE''>Isaac Kiptoo</CONTACT>
                    </CONTACTS> <!-- todo: Structure message & Include Mobile Number -->
                    <MSG SUBJECT=''''>Dear [FIRST_NAME], Mobile Banking Account for Mobile Number: [MOBILE_NUMBER] has made [LOGIN_ATTEMPTS] login attempts. The Mobile Banking Account is in [ATTEMPT_NAME] stage.</MSG> <!-- ATTEMPT_NAME=WARNING/SUSPENSION/LOCKED -->
                    <!-- Inbuild MSG Place holders [LOGIN_ATTEMPTS], [ATTEMPT_NAME], [ATTEMPT_ACTION], [NAME], [FIRST_NAME], [LAST_NAME] -->
                </GROUP>
                <GROUP NAME=''SUPPORT_TEAM_EMAIL_ADDRESS'' TYPE=''EMAIL_ADDRESS'' ATTEMPTS = ''THIRD_WARNING,DEFAULT_SUSPEND'' STATUS=''ACTIVE''>
                    <CONTACTS>
                        <!--<CONTACT IDENTIFIER=''moses@skyworld.co.ke'' ACTION=''TO'' STATUS=''ACTIVE''>Moses Magero</CONTACT>--> <!-- ACTION=''TO/CC/BCC'' is optional. Default = ''TO'' -->
                        <CONTACT IDENTIFIER=''isaac.kiptoo@skyworld.co.ke'' STATUS=''ACTIVE''>Isaac Kiptoo</CONTACT>
                    </CONTACTS>
                    <MSG SUBJECT=''Mobile Banking Login Attempts''>Dear [FIRST_NAME], Mobile Banking Account for Mobile Number: [MOBILE_NUMBER] has made [LOGIN_ATTEMPTS] login attempts. The Mobile Banking Account is in [ATTEMPT_NAME] stage.</MSG> <!-- ATTEMPT_NAME=WARNING/SUSPENSION/LOCKED -->
                </GROUP>
            </NOTIFY>
        </PASSWORD_ATTEMPT_PARAMETERS>
    </AUTHENTICATION_PARAMETERS>
</OTHER_DETAILS>', 30, 'AES_128', 'PRIVATE_CA_WITH_CLIENT_AUTH', 'TLSv1.2', 'YES', 'YES', 'D:\\certificates\\CA\\skycacert.crt', 'D:\\certificates\\CA\\portal.skyworld.co.ke.pfx', 'pnhB1kYi5tIQ2Y8zIjDvSg==', 'ENCRYPTED', 5, 6, '2022-02-16 12:50:51', '2018-06-21 04:28:34');
INSERT INTO client_parameters (parameters_urls, parameters_status, client_id, client_name, system_user_id, system_username, system_password, system_password_type, integrity_hash_enabled, integrity_hash_inbound_check, integrity_hash_outbound_check, integrity_secret, integrity_secret_type, client_xml_parameters, client_connection_timeout_seconds, client_encryption_type, client_certificate_configuration, client_cryptographic_protocol, client_certificate_chain_validation, client_hostname_verification, client_ca_certificate_file_path, client_certificate_file_path, client_certificate_password, client_certificate_password_type, messaging_web_server_id, management_web_server_id, date_modified, date_created) VALUES ('http://102.220.117.212:8088', 'ACTIVE', 0, 'MAPP Client', 3, 'mbanking_test@skyworld.co.ke', '4POGbtGpVwOj/0fTtUqBuCjp+EyclJfCwR5UOO/Cz7w=', 'ENCRYPTED', 'NO', 'NO', 'NO', 'ARFb6ygf+ZgvI+3nL93Afw==', 'ENCRYPTED', '<OTHER_DETAILS>
    <CUSTOM_PARAMETERS>
        <SERVICE_CONFIGS>
            <AMOUNT_LIMITS>
                <CASH_WITHDRAWAL>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>20</MAX_AMOUNT>
                </CASH_WITHDRAWAL>
                <AIRTIME_PURCHASE>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </AIRTIME_PURCHASE>
                <PAY_BILL>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </PAY_BILL>
                <EXTERNAL_FUNDS_TRANSFER>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </EXTERNAL_FUNDS_TRANSFER>
                <INTERNAL_FUNDS_TRANSFER>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </INTERNAL_FUNDS_TRANSFER>
                <DEPOSIT>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>20</MAX_AMOUNT>
                </DEPOSIT>
                <APPLY_LOAN>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </APPLY_LOAN>
                <PAY_LOAN>
                    <MIN_AMOUNT>10</MIN_AMOUNT>
                    <MAX_AMOUNT>100</MAX_AMOUNT>
                </PAY_LOAN>
            </AMOUNT_LIMITS>
            <CONFIGURATION>
                <CASH_WITHDRAWAL>
                    <CHANNELS>
                        <CHANNEL NAME="MPESA" LABEL="Safaricom M-PESA" STATUS="ACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
                        <CHANNEL NAME="ATM" LABEL="Withdraw Via ATM" STATUS="INACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
                        <CHANNEL NAME="AGENT" LABEL="Withdraw Via AGENT" STATUS="INACTIVE" WITHDRAW_TO_OTHER_NUMBER="INACTIVE"/>
                    </CHANNELS>
                </CASH_WITHDRAWAL>
                <AGENCY_BANKING>
                    <CUSTOMER_SEARCH_OPTION>
                        <OPTION NAME="PHONE_NUMBER" LABEL="Phone Number" STATUS="ACTIVE"/>
                        <OPTION NAME="ID_NUMBER" LABEL="ID Number" STATUS="ACTIVE"/>
                        <OPTION NAME="MEMBER_NUMBER" LABEL="Account Number" STATUS="ACTIVE"/>
                    </CUSTOMER_SEARCH_OPTION>
                </AGENCY_BANKING>
                <ACCOUNT_STATEMENT>
                    <STATEMENT_PERIODS>
                        <PERIOD NAME="CUSTOM" LABEL="Custom Period" STATUS="ACTIVE" START_DATE="MONTH_START" END_DATE="MONTH_END" MAXIMUM_TRANSACTIONS="100"/>
                        <PERIOD NAME="1WEEK" LABEL="Past 1 Week" STATUS="ACTIVE" START_DATE="TODAY-7D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="50"/>
                        <PERIOD NAME="2WEEKS" LABEL="Past 2 Weeks" STATUS="ACTIVE" START_DATE="TODAY-14D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="75"/>
                        <PERIOD NAME="1MONTHS" LABEL="Past 1 Month" STATUS="ACTIVE" START_DATE="TODAY-30D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="100"/>
                        <PERIOD NAME="3MONTHS" LABEL="Past 3 Months" STATUS="ACTIVE" START_DATE="TODAY-90D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="250"/>
                        <PERIOD NAME="6MONTHS" LABEL="Past 6 Months" STATUS="ACTIVE" START_DATE="TODAY-183D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="500"/>
                        <PERIOD NAME="YTD" LABEL="This Year To Date" STATUS="ACTIVE" START_DATE="TODAY-YTD" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="750"/>
                        <PERIOD NAME="1YEAR" LABEL="Past 1 Year" STATUS="ACTIVE" START_DATE="TODAY-365D" END_DATE="TODAY" MAXIMUM_TRANSACTIONS="1000"/>
                    </STATEMENT_PERIODS>
                </ACCOUNT_STATEMENT>
            </CONFIGURATION>
        </SERVICE_CONFIGS>
    </CUSTOM_PARAMETERS>
    <AUTHENTICATION_PARAMETERS>
        <PASSWORD_ATTEMPT_PARAMETERS>
            <ATTEMPTS NAME=''DEFAULT_SUSPEND'' ACTION=''SUSPEND'' STEP=''2'' DURATION=''30'' UNIT=''DAY'' NOTIFY=''YES'' >
                <!-- <ATTEMPTS NAME=''DEFAULT_SUSPEND/DEFAULT_WARN/DEFAULT_LOCK'' ACTION=''SUSPEND/WARN/LOCK'' STEP=''3'' DURATION=''24'' UNIT=''HOUR'' NOTIFY=''YES'' > -->
                <!--<ATTEMPT ACTION=''NONE/WARN/SUSPEND/LOCK'' NOTIFY=''YES/NO''>3</ATTEMPT>-->
                <ATTEMPT NAME=''FIRST_WARNING'' ACTION=''WARN'' NOTIFY=''NO''>2</ATTEMPT>
                <ATTEMPT NAME=''FIRST_SUSPENSION'' ACTION=''SUSPEND'' DURATION=''60'' UNIT=''MINUTE'' NOTIFY=''NO''>4</ATTEMPT> <!-- UNIT=''SECOND/MINUTE/HOUR/DAY'' -->
                <ATTEMPT NAME=''SECOND_WARNING'' ACTION=''WARN'' NOTIFY=''NO''>5</ATTEMPT>
                <ATTEMPT NAME=''SECOND_SUSPENSION'' ACTION=''SUSPEND'' DURATION=''24'' UNIT=''HOUR'' NOTIFY=''NO''>6</ATTEMPT>
                <ATTEMPT NAME=''THIRD_WARNING'' ACTION=''WARN'' NOTIFY=''NO''>7</ATTEMPT>
            </ATTEMPTS>
            <NOTIFY STATUS=''ACTIVE''>
                <!-- STATUS=''ACTIVE/INACTIVE''-->
                <GROUP NAME=''SUPPORT_TEAM_MSISDN'' TYPE=''MSISDN'' ATTEMPTS = ''THIRD_WARNING,DEFAULT_SUSPEND'' STATUS=''ACTIVE''>
                    <!-- TYPE=''MSISDN/APP_ID/EMAIL_ADDRESS'' STATUS=''ACTIVE/INACTIVE'' ATTEMPTS=''ATTEMPT NAMEs separated with Comma.''-->
                    <CONTACTS>
                        <CONTACT IDENTIFIER=''254721913958'' STATUS=''ACTIVE''>Moses Magero</CONTACT>
                        <!-- STATUS=''ACTIVE/INACTIVE''-->
                        <CONTACT IDENTIFIER=''254706405989'' STATUS=''ACTIVE''>Isaac Kiptoo</CONTACT>
                    </CONTACTS>
                    <!-- todo: Structure message & Include Mobile Number -->
                    <MSG SUBJECT=''''>Dear [FIRST_NAME], Mobile Banking Account for Mobile Number: [MOBILE_NUMBER] has made [LOGIN_ATTEMPTS] login attempts. The Mobile Banking Account is in [ATTEMPT_NAME] stage.</MSG>
                    <!-- ATTEMPT_NAME=WARNING/SUSPENSION/LOCKED -->
                    <!-- Inbuild MSG Place holders [LOGIN_ATTEMPTS], [ATTEMPT_NAME], [ATTEMPT_ACTION], [NAME], [FIRST_NAME], [LAST_NAME] -->
                </GROUP>
                <GROUP NAME=''SUPPORT_TEAM_EMAIL_ADDRESS'' TYPE=''EMAIL_ADDRESS'' ATTEMPTS = ''THIRD_WARNING,DEFAULT_SUSPEND'' STATUS=''ACTIVE''>
                    <CONTACTS>
                        <CONTACT IDENTIFIER=''moses@skyworld.co.ke'' ACTION=''TO'' STATUS=''ACTIVE''>Moses Magero</CONTACT>
                        <!-- ACTION=''TO/CC/BCC'' is optional. Default = ''TO'' -->
                        <CONTACT IDENTIFIER=''isaac.kiptoo@skyworld.co.ke'' STATUS=''ACTIVE''>Isaac Kiptoo</CONTACT>
                    </CONTACTS>
                    <MSG SUBJECT=''Mobile Banking Login Attempts''>Dear [FIRST_NAME], Mobile Banking Account for Mobile Number: [MOBILE_NUMBER] has made [LOGIN_ATTEMPTS] login attempts. The Mobile Banking Account is in [ATTEMPT_NAME] stage.</MSG>
                    <!-- ATTEMPT_NAME=WARNING/SUSPENSION/LOCKED -->
                </GROUP>
            </NOTIFY>
        </PASSWORD_ATTEMPT_PARAMETERS>
        <OTP_ATTEMPT_PARAMETERS>
            <ATTEMPTS NAME=''DEFAULT_SUSPEND'' ACTION=''SUSPEND'' STEP=''2'' RESET_OTP=''YES'' DURATION=''30'' UNIT=''DAY'' NOTIFY=''NO'' >
                <!--<ATTEMPT NAME=''FIRST_WARNING'' ACTION=''NONE/WARN/SUSPEND/LOCK'' RESET_OTP=''YES/NO'' NOTIFY=''YES/NO''>3</ATTEMPT>-->
                <ATTEMPT NAME=''FIRST_WARNING'' ACTION=''WARN'' RESET_OTP=''YES'' NOTIFY=''NO''>3</ATTEMPT>
                <ATTEMPT NAME=''FIRST_SUSPENSION'' ACTION=''SUSPEND'' RESET_OTP=''YES'' DURATION=''60'' UNIT=''MINUTE'' NOTIFY=''NO''>6</ATTEMPT>
                <!-- UNIT=''SECOND/MINUTE/HOUR/DAY'' -->
                <ATTEMPT NAME=''SECOND_WARNING'' ACTION=''WARN'' RESET_OTP=''YES'' NOTIFY=''NO''>9</ATTEMPT>
                <ATTEMPT NAME=''SECOND_SUSPENSION'' ACTION=''SUSPEND'' RESET_OTP=''YES'' DURATION=''24'' UNIT=''HOUR'' NOTIFY=''NO''>12</ATTEMPT>
                <ATTEMPT NAME=''THIRD_WARNING'' ACTION=''WARN'' RESET_OTP=''YES'' NOTIFY=''NO''>15</ATTEMPT>
                <!-- RESET_OTP=''YES'' Means Remove OTP from table -->
                <ATTEMPT NAME=''LOCK_ACCOUNT'' ACTION=''LOCK'' RESET_OTP=''YES'' NOTIFY=''YES''>18</ATTEMPT>
                <!-- if LOCK is set, do not use DEFAULT_SUSPEND -->
            </ATTEMPTS>
            <NOTIFY STATUS=''ACTIVE''>
                <!-- STATUS=''ACTIVE/INACTIVE''-->
                <GROUP NAME=''SUPPORT_TEAM_MSISDN'' TYPE=''MSISDN'' ATTEMPTS = ''THIRD_WARNING,DEFAULT_SUSPEND'' STATUS=''ACTIVE''>
                    <!-- TYPE=''MSISDN/APP_ID/EMAIL_ADDRESS'' STATUS=''ACTIVE/INACTIVE'' ATTEMPTS=''ATTEMPT NAMEs separated with Comma.''-->
                    <CONTACTS>
                        <CONTACT IDENTIFIER=''254721913958'' STATUS=''ACTIVE''>Moses Magero</CONTACT>
                        <!-- STATUS=''ACTIVE/INACTIVE''-->
                        <CONTACT IDENTIFIER=''254706405989'' STATUS=''ACTIVE''>Isaac Kiptoo</CONTACT>
                    </CONTACTS>
                    <!-- Include Mobile Number -->
                    <MSG SUBJECT=''''>Dear [FIRST_NAME], Mobile Banking User with Mobile Number: [MOBILE_NUMBER] has made [OTP_ATTEMPTS] OTP attempts. The Mobile Banking Account is in [ATTEMPT_NAME] stage.</MSG>
                    <!-- ATTEMPT_NAME=WARNING/SUSPENSION/LOCKED -->
                </GROUP>
                <GROUP NAME=''SUPPORT_TEAM_EMAIL_ADDRESS'' TYPE=''EMAIL_ADDRESS'' ATTEMPTS = ''THIRD_WARNING,DEFAULT_SUSPEND''  STATUS=''ACTIVE''>
                    <CONTACTS>
                        <CONTACT IDENTIFIER=''moses@skyworld.co.ke'' ACTION=''TO'' STATUS=''ACTIVE''>Moses Magero</CONTACT>
                        <CONTACT IDENTIFIER=''isaac.kiptoo@skyworld.co.ke'' STATUS=''ACTIVE''>Isaac Kiptoo</CONTACT>
                    </CONTACTS>
                    <MSG SUBJECT=''Mobile Banking OTP Attempts''>Dear [FIRST_NAME], Mobile Banking User with Mobile Number: [MOBILE_NUMBER] has made [OTP_ATTEMPTS] OTP attempts. The Mobile Banking Account is in [ATTEMPT_NAME] stage.</MSG>
                    <!-- ATTEMPT_NAME=WARNING/SUSPENSION/LOCKED -->
                </GROUP>
            </NOTIFY>
        </OTP_ATTEMPT_PARAMETERS>
    </AUTHENTICATION_PARAMETERS>
</OTHER_DETAILS>', 30, 'AES_128', 'PRIVATE_CA_WITH_CLIENT_AUTH', 'TLSv1.2', 'YES', 'YES', 'D:\\certificates\\CA\\skycacert.crt', 'D:\\certificates\\CA\\portal.skyworld.co.ke.pfx', 'Secret1234', 'CLEARTEXT', 5, 6, '2020-08-18 01:13:35', '2018-06-21 04:28:34');
INSERT INTO client_parameters (parameters_urls, parameters_status, client_id, client_name, system_user_id, system_username, system_password, system_password_type, integrity_hash_enabled, integrity_hash_inbound_check, integrity_hash_outbound_check, integrity_secret, integrity_secret_type, client_xml_parameters, client_connection_timeout_seconds, client_encryption_type, client_certificate_configuration, client_cryptographic_protocol, client_certificate_chain_validation, client_hostname_verification, client_ca_certificate_file_path, client_certificate_file_path, client_certificate_password, client_certificate_password_type, messaging_web_server_id, management_web_server_id, date_modified, date_created) VALUES ('http://102.220.117.213:8088', 'ACTIVE', 0, 'Register Client', 6, 'mbanking_test@skyworld.co.ke', 'RkcBqOkWINEZ6zd066IFcWgenEELjElVEITcXpva6MU=', 'ENCRYPTED', 'NO', 'NO', 'NO', 'mXvtfgcia5NmCeS53GHk6A==', 'ENCRYPTED', '<OTHER_DETAILS>
    <CUSTOM_PARAMETERS />
    <REGISTER_PARAMETERS>
        <SERVICE_URLS>
            <SERVICE_URL>http://102.220.117.213:8088/register_interface/global/register</SERVICE_URL>
            <SERVICE_URL>http://102.220.117.213:8088/register_interface/global/register</SERVICE_URL>
            <SERVICE_URL>http://102.220.117.213:8088/register_interface/global/register</SERVICE_URL>
        </SERVICE_URLS>
    </REGISTER_PARAMETERS>
</OTHER_DETAILS>', 30, 'AES_128', 'PRIVATE_CA_WITH_CLIENT_AUTH', 'TLSv1.2', 'YES', 'YES', 'D:\\certificates\\CA\\skycacert.crt', 'D:\\certificates\\CA\\portal.skyworld.co.ke.pfx', 'Secret1234', 'CLEARTEXT', 5, 6, '2021-01-21 16:38:12', '2018-06-21 04:28:34');


INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('101', 'Safaricom', 'MNO', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@safaricom.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '589ebf27b109c176fe96bbaf18d1bcc4175dd10fce10ebdf4a9f947da307820e', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('102', 'Airtel', 'MNO', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@airtel.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '258dfbd94ff87ee50c43a509610fb576685592b6cc97fea2d4aa042e9aff9ed9', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('201', 'Co-operative Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@coop.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '4810ea724e9bfaf1c7eb005949b5b8e92309383d435a136b400519cb649f8bf2', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('202', 'Kenya Commercial Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@kcb.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '9af08670cc5f45a39fd64cee7400b43f5dc5810191e312bd34085fea400ed3cb', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('203', 'Equity Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@equity.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '18a95b8159bc0e885d19dd50ea3a69eba83a2089a121e59d6bbb7d0f4a86408', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('204', 'Standard Chartered', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@stanchat.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '8fe4bd47436a89a3df4d4ea8fe7e60368e86d82489900f2038e89b4a09eac79e', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('205', 'Barclays Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@barclays.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '92f0fcefb2a54a0b93e9777802f08c4b2b1ab7f34197cc55a5a02afc980addaa', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('206', 'Post Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@postbank.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-03-24 14:37:21', '2020-03-24 14:37:21', '13f8f9b8fedc7044311f20f5316543a0a1e65e9ea959ca2458297a609bbf77d1', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('207', 'Family Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@familybank.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-08-13 12:24:47', '2020-08-13 12:24:47', '2b2aa5fc20f1becede3de407ef529678553cfada5829607bc4a1da59319f17d1', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('208', 'National Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@nationalbank.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-08-13 12:24:47', '2020-08-13 12:24:47', '778eacf912d78a2949542ab23fa05dc6d7fa45ba072fd379707506efa0d50169', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('209', 'Consolidated Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@consolidatedbank.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-08-13 12:24:47', '2020-08-13 12:24:47', 'c428b03ff91295647989c4986cb804d7722071c2f83e65e63ed83f5c108cd372', 0);
INSERT INTO service_providers (provider_code, provider_name, provider_type, provider_status, provider_status_description, country_code, physical_address, postal_address, email_addresses, phone_numbers, general_flag, other_details, date_created, date_modified, integrity_hash, provider_sequence) VALUES ('210', 'Sidian Bank', 'BANK', 'ACTIVE', 'ACTIVE', 'KEN', 'Nairobi, Kenya', '100-00100', 'info@sidianbank.co.ke', '254710000000', null, '<OTHER_DETAILS />', '2020-08-13 12:24:47', '2020-08-13 12:24:47', 'afc12f78e91543c9c0cb6f22ab6d0c05c3fdcf84bca7534c680820fd890cea5d', 0);

INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10101', '101', 'SHORT_CODE', 'Account Number', '0000', 'SACCO B2C', 'MPESA', 'MPESA', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'YES', 'NO', 'NO', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', '283696bfc2acd85cd2dd345ada799129090792d1474146c341c84ef5e3025413', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10102', '101', 'SHORT_CODE', 'Account Number', '0000', 'SACCO C2B', 'MPESA', 'MPESA', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'YES', 'NO', 'NO', 'NO', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'fc1bc148b17cebf0e25e2ba2ce97f8e470f81df35505f990d0ba3fa17a30b914', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10103', '101', 'SHORT_CODE', 'Account Number', '0000', 'SACCO B2C', 'MPESA', 'MPESA', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'YES', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'c3b027f50487436f3511988e0a3a7881e449a12cc111759044d27a84d7272404', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10104', '101', 'UTILITY_CODE', 'Meter Number', '888880', 'KPLC TOKEN', 'KPLC TOKEN', 'KPLC TOKEN', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', '656b99efd51d883bf1a3d9e8430faf7f3d31819280aa92841ea200078c78a902', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10105', '101', 'UTILITY_CODE', 'Meter Number', '888888', 'KPLC Postpaid', 'KPLC Postpaid', 'KPLC Postpaid', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'b937bfc12592c6d79acdb41254c2fd17eef637b81ee3de1c18f441c032b9e8d5', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10106', '101', 'UTILITY_CODE', 'Account Number', '444400', 'Nairobi Water', 'Nairobi Water', 'Nairobi Water', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'e7c18042c7c23686c5b1996d14d965bd4fac0d2ba9942bbe605ced5ed128f74b', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10107', '101', 'UTILITY_CODE', 'Account Number', '444900', 'DStv', 'DStv', 'DStv', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'd6e041e5d6a5340e21cc1fcd7fab2d7f37118c286588a9aef6b2d8cc3202f654', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10108', '101', 'UTILITY_CODE', 'Account Number', '320320', 'ZUKU', 'ZUKU', 'ZUKU', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'bfae392f93c7ebbafefde5f71dd4677a9d95f6651caaa5d732ce4893d5378ae9', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('10109', '101', 'UTILITY_CODE', 'Account Number', '423655', 'GOtv', 'GOtv', 'GOtv', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-03-24 14:37:28', '2020-03-24 14:37:28', '2593d5eda8cca73c2b1ca959d30b096c5179cfc3d8be6b0c6d1a2db7b6dfc68f', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20101', '201', 'BANK_CODE', 'Account Number', '11', 'PESALINK - COOP', 'Cooperative Bank', 'Cooperative Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 250000, 'NO', 'YES', 'YES', 'YES', null, '<OTHER_DETAILS><DATA><PROVIDER_ACCOUNT_DETAILS><BRANCH_CODE>000</BRANCH_CODE></PROVIDER_ACCOUNT_DETAILS></DATA></OTHER_DETAILS>', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'b7c47e4c617efa1c1130051eddc2b459a6fa2e35db962df54f178379f4b2a6ab', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20102', '201', 'BANK_SHORT_CODE', 'Account Number', '400200', 'SKY WORLD LTD - SAFARICOM - COOP', 'Coop Bank', 'Cooperative Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-08-08 18:21:29', '2020-08-08 18:21:29', '9fbc1297c53a1cfe227211cb18d8a9cf7a2bdd749e56f36dcdba615bc41a4814', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20201', '202', 'BANK_CODE', 'Account Number', '01', 'PESALINK - KCB', 'KCB', 'KCB', 'ACTIVE', 'ACTIVE', 'KES', 100, 250000, 'NO', 'YES', 'YES', 'YES', null, '<OTHER_DETAILS><DATA><PROVIDER_ACCOUNT_DETAILS><BRANCH_CODE>094</BRANCH_CODE></PROVIDER_ACCOUNT_DETAILS></DATA></OTHER_DETAILS>', '2020-03-24 14:37:28', '2020-03-24 14:37:28', '6443e09d48e52834ec0322de4b52750277e27854d7e62633d4db83c7d85348cc', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20202', '202', 'BANK_SHORT_CODE', 'Account Number', '522522', 'SKY WORLD LTD - SAFARICOM - KCB', 'KCB', 'KCB', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-08-08 18:21:29', '2020-08-08 18:21:29', '5624b0ef6f989c427a90b5b0714f66bbba4cfa5dc06fd90c7559426458309105', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20301', '203', 'BANK_CODE', 'Account Number', '68', 'PESALINK - EQUITY', 'EQUITY', 'Equity Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 250000, 'NO', 'YES', 'YES', 'YES', null, '<OTHER_DETAILS><DATA><PROVIDER_ACCOUNT_DETAILS><BRANCH_CODE>000</BRANCH_CODE></PROVIDER_ACCOUNT_DETAILS></DATA></OTHER_DETAILS>', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'dbee7968260bd2f033336c5f57bf0cd8f1d79e9dfa05e2bc04e2358aa42950c9', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20302', '203', 'BANK_SHORT_CODE', 'Account Number', '247247', 'SKY WORLD LTD - SAFARICOM - EQUITY', 'EQUITY', 'Equity Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-08-08 18:21:29', '2020-08-08 18:21:29', '62ab92ec9c7f5ab779e849626a08d9df8298404378bd46ce097e582c6fb65814', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20401', '204', 'BANK_CODE', 'Account Number', '02', 'PESALINK - STANCHAT', 'STANCHAT', 'Standard Chartered Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 250000, 'NO', 'YES', 'YES', 'YES', null, '<OTHER_DETAILS><DATA><PROVIDER_ACCOUNT_DETAILS><BRANCH_CODE>015</BRANCH_CODE></PROVIDER_ACCOUNT_DETAILS></DATA></OTHER_DETAILS>', '2020-03-24 14:37:28', '2020-03-24 14:37:28', '29598aaf20bc2ed3fcc55e351303ac5baa1323562af9f25bb48c5558dec290a9', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20402', '204', 'BANK_SHORT_CODE', 'Account Number', '329329', 'SKY WORLD LTD - SAFARICOM - STANCHAT', 'STANCHAT', 'Standard Chartered Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-08-08 18:21:29', '2020-08-08 18:21:29', '22c212d3888129f22d11792a6ebfce6027fa823d437817018979983d5546de60', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20501', '205', 'BANK_CODE', 'Account Number', '03', 'PESALINK - BARCLAYS', 'BARCLAYS', 'ABSA Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 250000, 'NO', 'YES', 'YES', 'YES', null, '<OTHER_DETAILS><DATA><PROVIDER_ACCOUNT_DETAILS><BRANCH_CODE>001</BRANCH_CODE></PROVIDER_ACCOUNT_DETAILS></DATA></OTHER_DETAILS>', '2020-03-24 14:37:28', '2020-03-24 14:37:28', 'c9870eb1818b2712cadf0d4abeaf9b1928ef01538f454d41d737de6a874fb2b3', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20502', '205', 'BANK_SHORT_CODE', 'Account Number', '303030', 'SKY WORLD LTD - SAFARICOM - BARCLAYS', 'BARCLAYS', 'ABSA Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 70000, 'NO', 'NO', 'YES', 'YES', null, '<OTHER_DETAILS />', '2020-08-08 18:21:29', '2020-08-08 18:21:29', 'aad51915900fd70374fe00d89c6904fa94223e9c571945ed9f89b2b397275403', 0);
INSERT INTO service_provider_accounts (provider_account_code, provider_code, account_type, account_type_tag, account_identifier, account_name, account_short_tag, account_long_tag, account_status, account_status_description, currency_code, min_transaction_amount, max_transaction_amount, c2b_capability, c2c_capability, b2c_capability, b2b_capability, general_flag, other_details, date_created, date_modified, integrity_hash, provider_account_sequence) VALUES ('20701', '207', 'BANK_CODE', 'Account Number', '70', 'PESALINK - FAMILY', 'FAMILY', 'Family Bank', 'ACTIVE', 'ACTIVE', 'KES', 100, 250000, 'NO', 'YES', 'YES', 'YES', null, '<OTHER_DETAILS><DATA><PROVIDER_ACCOUNT_DETAILS><BRANCH_CODE>000</BRANCH_CODE></PROVIDER_ACCOUNT_DETAILS></DATA></OTHER_DETAILS>', '2020-08-13 12:33:16', '2020-08-13 12:33:16', 'c21cd551225ec3233829e6a94e3ef54d6b867537d41c3b5ab35b6d137811d575', 0);
