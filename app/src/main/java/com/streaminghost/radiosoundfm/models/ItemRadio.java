package com.streaminghost.radiosoundfm.models;

public class ItemRadio {

    private String radio_name;
    private String radio_url;
    private String radio_image;

    public ItemRadio(String radio_name, String radio_url) {
        this.radio_name = radio_name;
        this.radio_url = radio_url;
    }

    public String getRadio_name() {
        return radio_name;
    }

    public void setRadio_name(String radio_name) {
        this.radio_name = radio_name;
    }

    public String getRadio_url() {
        return radio_url;
    }

    public void setRadio_url(String radio_url) {
        this.radio_url = radio_url;
    }

    public String getRadio_image() {
        return radio_image;
    }

    public void setRadio_image(String radio_image) {
        this.radio_image = radio_image;
    }

}
