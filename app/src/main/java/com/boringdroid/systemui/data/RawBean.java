package com.boringdroid.systemui.data;

public class RawBean {

    private  int id ;
    private int resourceId;
    private String resourceName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String toString() {
        return "RawBean{" +
                "id=" + id +
                ", resourceId=" + resourceId +
                ", resourceName='" + resourceName + '\'' +
                '}';
    }
}
