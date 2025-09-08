package ke.skyworld.mbanking.email;

public class EMailConstants {
	public static enum EMailType {
		INBOUND_EMAIL("INBOUND_EMAIL"),
		OUTBOUND_EMAIL("OUTBOUND_EMAIL");
		
		private String strValue;

        private EMailType(String theValue) {
                this.strValue = theValue;
        }
        
        public String getValue() {
            return strValue;
        }
	};
	
	public enum EMailStatusCode {
		QUEUED(10),
		RELAYED(101),
		SENT(102),
		SEND_ERROR(103),
		SEND_FAILED(104),
		
		CONFIRMED(105), //Accept Status From Remote
		COMPLETED(106),
		REQUEST_QUERY_TRANSACTION(107),
		QUERY_TRANSACTION_ERROR(108),
		QUERY_TRANSACTION_FAILED(109),
		QUERY_TRANSACTION_COMPLETED(110),
		
		REVERSE_REQUEST(201), //Accept Status From Remote
		REVERSE_ERROR(202),
		REVERSE_FAILED(203),
		REVERSE_COMPLETED(204),
		
		INSUFFICIENT_CREDIT(401),
		
		HTTP_POST_VARIABLES_ERROR(801),
		INVALID_XML_ERROR(802),
		INVALID_XML_MESSAGE_VERSION_ERROR(803),
		ENCRYPT_ERROR(804),
		DECRYPT_ERROR(805),
		ACCESS_DENIED_ERROR(806),
		INVALID_EMail_ID_ERROR(807),
		INVALID_EMail_TYPE_ERROR(808),
		INVALID_DATETIME_ERROR(809),
		DUPLICATE_TRANSACTION_ERROR(810),
		TRANSACTION_REJECTED_ERROR(811),
		
		SUSPENDED(901),
		STOPPED(902),
		TIMEDOUT(903),
		EXPIRED(904),
		UNKNOWN_RESPONSE(998),
		UNKNOWN_ERROR(999);

		private int intValue;
		
        private EMailStatusCode(int theValue) {
                this.intValue = theValue;
        }
        
        public int getValue() {
            return intValue;
        } 
	};

	public static enum EMailMode {
		SAF("SAF"),
		EXPRESS("EXPRESS");

		private String strValue;

		private EMailMode(String theValue) {
			this.strValue = theValue;
		}

		public String getValue() {
			return strValue;
		}
	};

	public static enum Sensitivity {
		NORMAL("NORMAL"),
		PERSONAL("PERSONAL"),
		PRIVATE("PRIVATE"),
		CONFIDENTIAL("CONFIDENTIAL");

		private String strValue;

		private Sensitivity(String theValue) {
			this.strValue = theValue;
		}

		public String getValue() {
			return strValue;
		}
	};

	public static enum EMailProtocol {
		SMTP("SMTP"),
		IMAP("IMAP");
		private String strValue;

		private EMailProtocol(String theValue) {
			this.strValue = theValue;
		}

		public String getValue() {
			return strValue;
		}
	};

	public static enum EMailEncryption {
		NONE("NONE"),
		SSL("SSL"),
		TLS("TLS");
		private String strValue;

		private EMailEncryption(String theValue) {
			this.strValue = theValue;
		}

		public String getValue() {
			return strValue;
		}
	};
}
