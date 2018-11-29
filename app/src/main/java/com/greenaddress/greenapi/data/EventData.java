package com.greenaddress.greenapi.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

public class EventData implements Serializable {
    private int title;
    private int descriptionFormat;
    private String[] descriptionArgs;
    private Date date;
    private Object value;

    public EventData(final int title, final int descriptionFormat, final String[] descriptionArgs, final Date date,
                     final Object value) {
        this.title = title;
        this.descriptionFormat = descriptionFormat;
        this.descriptionArgs = descriptionArgs;
        this.date = date;
        this.value = value;
    }

    public int getTitle() {
        return title;
    }

    public void setTitle(final int title) {
        this.title = title;
    }

    public int getDescriptionFormat() {
        return descriptionFormat;
    }

    public void setDescriptionFormat(final int descriptionFormat) {
        this.descriptionFormat = descriptionFormat;
    }

    public String[] getDescriptionArgs() {
        return descriptionArgs;
    }

    public void setDescriptionArgs(final String[] descriptionArgs) {
        this.descriptionArgs = descriptionArgs;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(final Date date) {
        this.date = date;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "EventData{" +
               "title=" + title +
               ", descriptionFormat=" + descriptionFormat +
               ", descriptionArgs=" + Arrays.toString(descriptionArgs) +
               ", date=" + date +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventData eventData = (EventData) o;
        return title == eventData.title &&
               descriptionFormat == eventData.descriptionFormat &&
               Arrays.equals(descriptionArgs, eventData.descriptionArgs);
    }

    @Override
    public int hashCode() {

        int result = Objects.hash(title, descriptionFormat);
        result = 31 * result + Arrays.hashCode(descriptionArgs);
        return result;
    }
}
