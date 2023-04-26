package com.writesmith.model.http.client.openaigpt;

import com.writesmith.model.database.Sender;

public class RoleMapper {

    public static Role getRole(Sender sender) {
        switch (sender) {
            case USER:
                return Role.USER;
            case AI:
                return Role.ASSISTANT;
        }

        return null;
    }

}
