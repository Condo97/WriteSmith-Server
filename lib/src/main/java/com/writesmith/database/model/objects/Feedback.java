package com.writesmith.database.model.objects;

import sqlcomponentizer.dbserializer.DBColumn;
import sqlcomponentizer.dbserializer.DBSerializable;

import java.time.LocalDateTime;

@DBSerializable(tableName = "Feedback")
public class Feedback {

    @DBColumn(name = "id", primaryKey = true)
    private Integer feedback_id;

    @DBColumn(name = "feedback")
    private String feedback;

    @DBColumn(name = "date")
    private LocalDateTime date;

    public Feedback() {

    }

    public Feedback(Integer feedback_id, String feedback, LocalDateTime date) {
        this.feedback_id = feedback_id;
        this.feedback = feedback;
        this.date = date;
    }

    public Integer getFeedback_id() {
        return feedback_id;
    }

    public String getFeedback() {
        return feedback;
    }

    public LocalDateTime getDate() {
        return date;
    }

}
