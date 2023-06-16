package com.writesmith.model.database;

public class DBRegistry {

    public class Table {

        public class Chat {

            public static final String TABLE_NAME = "Chat";
            public static final String chat_id = "chat_id";
            public static final String conversation_id = "conversation_id";
            public static final String sender = "sender";
            public static final String text = "text";
            public static final String date = "date";

        }

        public class ChatLegacy {

            public static final String TABLE_NAME = "ChatLegacy";
            public static final String chat_id = "chat_id";
            public static final String user_id = "user_id";
            public static final String user_text = "user_text";
            public static final String ai_text = "ai_text";
            public static final String finish_reason = "finish_reason";
            public static final String date = "date";

        }

        public class Conversation {

            public static final String TABLE_NAME = "Conversation";
            public static final String conversation_id = "conversation_id";
            public static final String user_id = "user_id";
            public static final String behavior = "behavior";

        }

        public class GeneratedChat {

            public static final String TABLE_NAME = "GeneratedChat";
            public static final String chat_id = "chat_id";
            public static final String finish_reason = "finish_reason";
            public static final String model_name = "model_name";
            public static final String completion_tokens = "completion_tokens";
            public static final String prompt_tokens = "prompt_tokens";
            public static final String total_tokens = "total_tokens";

        }

        public class Transaction {

            public static final String TABLE_NAME = "Transaction";
            public static final String transaction_id = "transaction_id";
            public static final String user_id = "user_id";
            public static final String appstore_transaction_id = "appstore_transaction_id";
            public static final String transaction_date = "transaction_date";
            public static final String record_date = "record_date";
            public static final String check_date = "check_date";
            public static final String status = "status";

        }

        public class Receipt {

            public static final String TABLE_NAME = "Receipt";
            public static final String receipt_id = "receipt_id";
            public static final String user_id = "user_id";
            public static final String receipt_data = "receipt_data";
            public static final String record_date = "record_date";
            public static final String check_date = "check_date";
            public static final String expired = "expired";

        }

        public class User_AuthToken {
            public static final String TABLE_NAME = "User_AuthToken";
            public static final String user_id = "user_id";
            public static final String auth_token = "auth_token";

        }

    }

}
