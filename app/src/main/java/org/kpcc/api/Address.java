package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;


public class Address extends Entity
{

    private String mLine1;
    private String mLine2;
    private String mCity;
    private String mState;
    private String mZipCode;


    public static Address buildFromJson(JSONObject jsonAddress)
    {
        Address address = new Address();

        try {
            address.setLine1(jsonAddress.getString("line_1"));
            address.setLine2(jsonAddress.getString("line_2"));
            address.setCity(jsonAddress.getString("city"));
            address.setState(jsonAddress.getString("state"));
            address.setZipCode(jsonAddress.getString("zip_code"));

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return address;
    }


    public String getLine1()
    {
        return mLine1;
    }

    public void setLine1(String line1)
    {
        mLine1 = line1;
    }

    public String getLine2()
    {
        return mLine2;
    }

    public void setLine2(String line2)
    {
        mLine2 = line2;
    }

    public String getCity()
    {
        return mCity;
    }

    public void setCity(String city)
    {
        mCity = city;
    }

    public String getState()
    {
        return mState;
    }

    public void setState(String state)
    {
        mState = state;
    }

    public String getZipCode()
    {
        return mZipCode;
    }

    public void setZipCode(String zipCode)
    {
        mZipCode = zipCode;
    }

}
