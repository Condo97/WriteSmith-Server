package com.writesmith.database.model.objects;

import com.writesmith.database.model.DBRegistry;
import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = DBRegistry.Table.APNSRegistration.TABLE_NAME)
public class APNSRegistration {

    @DBColumn(name = DBRegistry.Table.APNSRegistration.id, primaryKey = true)
    private Integer id;

    @DBColumn(name = DBRegistry.Table.APNSRegistration.user_id)
    private Integer userID;

    @DBColumn(name = DBRegistry.Table.APNSRegistration.device_id)
    private String deviceID;

    @DBColumn(name = DBRegistry.Table.APNSRegistration.add_date)
    private LocalDateTime addDate;

    @DBColumn(name = DBRegistry.Table.APNSRegistration.update_date)
    private LocalDateTime updateDate;

    public APNSRegistration() {

    }

    public APNSRegistration(Integer id, Integer userID, String deviceID, LocalDateTime addDate, LocalDateTime updateDate) {
        this.id = id;
        this.userID = userID;
        this.deviceID = deviceID;
        this.addDate = addDate;
        this.updateDate = updateDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserID() {
        return userID;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public LocalDateTime getAddDate() {
        return addDate;
    }

    public void setAddDate(LocalDateTime addDate) {
        this.addDate = addDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

}
