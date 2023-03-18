package com.writesmith.database.tableobjects.factories;

import com.writesmith.database.tableobjects.User_AuthToken;
import com.writesmith.database.tableobjects.helpers.AuthTokenGenerator;
import com.writesmith.exceptions.AutoIncrementingDBObjectExistsException;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.dbserializer.DBSerializerPrimaryKeyMissingException;

import java.sql.SQLException;

public class User_AuthTokenFactory extends DBObjectFactory {

    public static User_AuthToken getFromDB(String authToken) throws DBSerializerException, SQLException, IllegalAccessException, DBObjectNotFoundFromQueryException {
        User_AuthToken u_aT = new User_AuthToken(null, authToken);
        u_aT.fillWhereColumnNameAndObject("auth_token", u_aT.getAuthToken()); // TODO: - Is there a way to not use plain text here, maybe somehow just use the row or something? Or just use the plain text and look up the id by annotation? Maybe that is better, then it verifies the text

        return u_aT;
    }

    public static User_AuthToken createInDB() throws AutoIncrementingDBObjectExistsException, DBSerializerException, DBSerializerPrimaryKeyMissingException, IllegalAccessException, SQLException {
        // Create u_aT and ensure no userID
        User_AuthToken u_aT = create();

        insertWithAutoIncrementingPrimaryKey(u_aT);

        return u_aT;
    }

    /***
     * Creates a User_AuthToken object with an empty userID and generated authToken, just for use in the factory
     *
     * @return a User_AuthToken with only a generated authToken
     */
    private static User_AuthToken create() {
        return create(null);
    }

    /***
     * This creates a new User_AuthToken with the userID and a generated AuthToken
     *
     * @param userID
     * @return User_AuthToken with generated AuthToken
     */
    public static User_AuthToken create(Integer userID) {
        return create(userID, AuthTokenGenerator.generateAuthToken());
    }

    public static User_AuthToken create(Integer userID, String authToken) {
        return new User_AuthToken(userID, authToken);
    }
}
