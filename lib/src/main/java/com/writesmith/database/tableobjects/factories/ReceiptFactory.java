package com.writesmith.database.tableobjects.factories;

import com.writesmith.database.tableobjects.Receipt;
import com.writesmith.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ReceiptFactory {

    public static Receipt getMostRecentReceiptFromDB(Integer userID) throws DBSerializerException, SQLException, IllegalAccessException, DBObjectNotFoundFromQueryException, InterruptedException {
        Receipt receipt = new Receipt(userID, null);

//        receipt.fillMostRecentByColumnNameAndObject("user_id", receipt.getUserID()); // TODO: - Fix the redundancy of supplying the object something it already contains.. user_id can be used to locate the value of the field

        receipt.fillWhereColumnObjectMapOrderByLimit(Map.of(
                "user_id", userID
        ), List.of(
                "record_date"
        ), OrderByComponent.Direction.DESC, 1);

        return receipt;
    }
}
