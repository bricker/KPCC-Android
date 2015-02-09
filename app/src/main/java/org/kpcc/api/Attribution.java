package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Attribution extends Entity
{

    private String mName;
    private String mRoleText;
    private int mRoleId;


    public static Attribution buildFromJson(JSONObject jsonAttribution)
    {
        Attribution attribution = new Attribution();

        try
        {
            attribution.setName(jsonAttribution.getString("name"));
            attribution.setRoleText(jsonAttribution.getString("role_text"));
            attribution.setRoleId(jsonAttribution.getInt("role"));

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return attribution;
    }


    public String getName()
    {
        return mName;
    }

    public void setName(String name)
    {
        mName = name;
    }

    public String getRoleText()
    {
        return mRoleText;
    }

    public void setRoleText(String roleText)
    {
        mRoleText = roleText;
    }

    public int getRoleId()
    {
        return mRoleId;
    }

    public void setRoleId(int roleId)
    {
        mRoleId = roleId;
    }

}
