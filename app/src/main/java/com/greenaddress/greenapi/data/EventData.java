package com.greenaddress.greenapi.data;

import java.io.Serializable;
import java.util.Objects;

public class EventData implements Serializable {
    private int title;
    private int description;
    private Object value;

    public EventData(int title, int description) {
        init(title, description, null);
    }

    public EventData(final int title, final int description, final Object value) {
        init(title, description, value);
    }

    private void init(int title, int descriptionFormat, Object value) {
        this.title = title;
        this.description = descriptionFormat;
        this.value = value;
    }

    public int getTitle() {
        return title;
    }

    public void setTitle(final int title) {
        this.title = title;
    }

    public int getDescription() {
        return description;
    }

    public void setDescription(final int description) {
        this.description = description;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventData eventData = (EventData) o;
        return title == eventData.title &&
               description == eventData.description &&
               Objects.equals(value, eventData.value);
    }

    @Override
    public int hashCode() {

        return Objects.hash(title, description, value);
    }
}
