package com.writesmith.core;

import com.oaigptconnector.model.CompletionRole;
import com.writesmith.database.model.Sender;

public class RoleMapper {

    public static CompletionRole getRole(Sender sender) {
        switch (sender) {
            case USER:
                return CompletionRole.USER;
            case AI:
                return CompletionRole.ASSISTANT;
        }

        return null;
    }

}
