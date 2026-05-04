package com.idata.workingrfid.database;

public class RFIDItem {
    private int id;
    private String tag;
    private String name;
    private String location;

    public RFIDItem() {}

    public RFIDItem(String tag, String name, String location) {
        this.tag = tag;
        this.name = name;
        this.location = location;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
