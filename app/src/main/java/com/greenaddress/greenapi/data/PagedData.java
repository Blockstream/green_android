package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PagedData<T> extends JSONData {
    private List<T> list;
    private Integer nextPageId;
    private Integer pageId;

    public List<T> getList() {
        return list;
    }

    public void setList(final List<T> list) {
        this.list = list;
    }

    public Integer getNextPageId() {
        return nextPageId;
    }

    public void setNextPageId(final Integer nextPageId) {
        this.nextPageId = nextPageId;
    }

    public Integer getPageId() {
        return pageId;
    }

    public void setPageId(final Integer pageId) {
        this.pageId = pageId;
    }
}
