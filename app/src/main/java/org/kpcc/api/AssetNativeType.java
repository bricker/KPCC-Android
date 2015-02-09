package org.kpcc.api;


import org.json.JSONException;
import org.json.JSONObject;

public class AssetNativeType extends Entity
{

    private String mType;
    private String mId;


    public static AssetNativeType buildFromJson(JSONObject jsonNative)
    {
        AssetNativeType nativeType = new AssetNativeType();

        try
        {
            nativeType.setType(jsonNative.getString("class"));
            nativeType.setId(jsonNative.getString("id"));

        } catch (JSONException e) {
            // TODO: Handle Exception
            e.printStackTrace();
        }

        return nativeType;
    }


    public String getType()
    {
        return mType;
    }

    public void setType(String type)
    {
        mType = type;
    }


    public String getId()
    {
        return mId;
    }

    public void setId(String id)
    {
        mId = id;
    }

}
