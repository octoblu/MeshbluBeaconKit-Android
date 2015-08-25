package com.octoblu.meshblubeaconkit;

import android.util.Log;

import java.util.Date;
import com.octoblu.sanejsonobject.SaneJSONObject;
/**
 * Created by octoblu on 8/3/15.
 */
public class BeaconInfo {
    private static final String TAG = "BeaconInfo";
    public String uuid;
    public Boolean status;
    public String name;
    public Integer sensitivity = 50;
    public Double lastDistance = 0.0;
    public Double sensitivityDistance = 2.0;
    public Date lastSeen = new Date();
    public Date lastUpdated = new Date();
    private final Integer FIVE_MINUTES_MS = 5 * 60 * 1000;
    private final Double MAX_SENSITIVITY = 10.0;

    public BeaconInfo(SaneJSONObject jsonObject){
        loadFromJSON(jsonObject);
        calculateSensitivity();
    }

    public void loadFromJSON(SaneJSONObject jsonObject){
        String newName = jsonObject.getStringOrNull("name");
        if(newName == null || newName.length() == 0){
            name = "Unknown Name";
        }else{
            name = newName;
        }
        String newUuid = jsonObject.getStringOrNull("uuid");
        if(newUuid == null || newUuid.length() == 0){
            uuid = "Unknown UUID";
        }else{
            uuid = newUuid;
        }
        status = jsonObject.getBoolean("status", false);
        Integer newSensitivity = jsonObject.getInteger("sensitivity", -1);
        if(newSensitivity >= 0){
            sensitivity = newSensitivity;
        }
        Date newLastSeen = jsonObject.getDateOrNull("lastSeen");
        if(newLastSeen != null){
            lastSeen = newLastSeen;
        }
        Date newLastUpdated = jsonObject.getDateOrNull("lastUpdated");
        if(newLastUpdated != null){
            lastUpdated = newLastUpdated;
        }
    }

    public void setLastDistance(Double lastDistance){
        this.lastDistance = lastDistance;
    }

    private void calculateSensitivity(){
        sensitivityDistance = ((sensitivity / 100.0) * MAX_SENSITIVITY);
    }

    public Boolean hasChangedRecently(){
        if(lastSeen == null){
            return false;
        }
        Date currentDate = new Date();

        return lastSeen.getTime() > (currentDate.getTime() - FIVE_MINUTES_MS);
    }

    public Boolean hasChangedDistance(Double distance){
        calculateSensitivity();
        lastSeen = new Date();

        if(distance > (lastDistance + sensitivityDistance)){
            significantChange(distance);
            return true;
        }else if(distance < (lastDistance - sensitivityDistance)){
            significantChange(distance);
            return true;
        }
        return false;
    }

    private void significantChange(Double distance){
        Log.d(TAG, String.format("Changed significant distance! %s %f %f %f", uuid.substring(0, 8), sensitivityDistance, distance, lastDistance));
        lastUpdated = new Date();
    }

    public SaneJSONObject toJSON(){
        SaneJSONObject jsonObject = new SaneJSONObject().fromString("{}");
        jsonObject.putBooleanOrIgnore("status", status);
        jsonObject.putOrIgnore("name", name);
        jsonObject.putOrIgnore("uuid", uuid);
        jsonObject.putIntOrIgnore("sensitivity", sensitivity);
        jsonObject.putDateOrIgnore("lastSeen", lastSeen);
        jsonObject.putDateOrIgnore("lastUpdated", lastUpdated);
        return jsonObject;
    }
}