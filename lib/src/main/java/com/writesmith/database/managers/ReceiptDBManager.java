package com.writesmith.database.managers;

import com.writesmith.database.DBManager;
import com.writesmith.model.database.objects.Receipt;
import com.writesmith.common.exceptions.DBObjectNotFoundFromQueryException;
import sqlcomponentizer.dbserializer.DBSerializerException;
import sqlcomponentizer.preparedstatement.component.OrderByComponent;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ReceiptDBManager extends DBManager {

    public static Receipt getMostRecentReceiptFromDB(Integer userID) throws DBSerializerException, SQLException, IllegalAccessException, DBObjectNotFoundFromQueryException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException {
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
