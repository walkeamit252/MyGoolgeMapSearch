package com.example.amitwalke.mygoolgemapsearch.model;

public class PickLocationModel {
    Double latitude;
    Double longitude;
    String locationName;

    public PickLocationModel(Double latitude,Double longitude){
        this.latitude=latitude;
        this.longitude=longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }
}
