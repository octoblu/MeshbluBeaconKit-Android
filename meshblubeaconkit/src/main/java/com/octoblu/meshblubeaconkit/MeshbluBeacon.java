package com.octoblu.meshblubeaconkit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;

import com.octoblu.meshblukit.Meshblu;
import com.octoblu.sanejsonobject.SaneJSONObject;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class MeshbluBeacon implements BootstrapNotifier, BeaconConsumer {
    private static final String TAG = "MeshbluBeacon";
    private static final Double VERSION = 1.0;
    private Meshblu meshblu;
    private Emitter emitter = new Emitter();
    private Boolean monitoring = true;
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private List<String> beaconTypes;
    private Context context;
    private BeaconManager beaconManager;
    public ArrayList<BeaconInfo> beaconInfo = new ArrayList();
    public final class BEACON_TYPES {
        public static final String ESTIMOTE = "Estimote";
        public static final String IBEACON = "iBeacon";
        public static final String EASIBEACON = "easiBeacon";
        public static final String ALTBEACON = "AltBeacon";
    }
    public final class EVENTS {
        public static final String REGISTER = "register";
        public static final String GENERATED_TOKEN = "generate_token";
        public static final String WHOAMI = "whoami";
        public static final String LOCATION_UPDATE = "location_update";
        public static final String DID_ENTER_REGION = "did_enter_region";
        public static final String DID_EXIT_REGION = "did_exit_region";
        public static final String REGION_STATE_CHANGE = "region_state_change";
        public static final String DISCOVERED_BEACON = "discovered_beacon";
    }
    public MeshbluBeacon(SaneJSONObject meshbluConfig, Context context){
        this.meshblu = new Meshblu(meshbluConfig, context);
        this.context = context;
        beaconManager = BeaconManager.getInstanceForApplication(context);
    }

    public MeshbluBeacon(Meshblu meshblu, Context context){
        this.meshblu = meshblu;
        this.context = context;
        beaconManager = BeaconManager.getInstanceForApplication(context);
    }

    public void on(String event, Emitter.Listener fn) {
        emitter.on(event, fn);
    }

    public void off() {
        emitter.off();
    }

    public void start(List<String> beaconTypes){
        this.beaconTypes = beaconTypes;
        startMeshbluListeners();
        if(!meshblu.isRegistered()){
            Log.d(TAG, "Device is not registered, registering now");
            SaneJSONObject properties = new SaneJSONObject();
            properties.putOrIgnore("type", "device:beacon-blu");
            properties.putOrIgnore("online", "true");
            meshblu.register(properties);
        }else{
            Log.d(TAG, "Device is registered, starting...");
            startBeaconMonitoring();
            getDevice();
        }
    }

    private void startMeshbluListeners(){
        meshblu.on(Meshblu.REGISTER, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Regsitered");
                emitter.emit(EVENTS.REGISTER, args);
                JSONObject deviceJSON = (JSONObject) args[0];
                setCredentials(SaneJSONObject.fromJSONObject(deviceJSON));
                startBeaconMonitoring();
                getDevice();
            }
        });
        meshblu.on(Meshblu.WHOAMI, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.d(TAG, "Whoami");
                emitter.emit(EVENTS.WHOAMI, args);
            }
        });
        meshblu.on(Meshblu.GENERATED_TOKEN, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                emitter.emit(EVENTS.GENERATED_TOKEN, args);
            }
        });
    }

    public void getDevice(){
        meshblu.whoami();
    }

    public void generateToken(){
        meshblu.generateToken(meshblu.uuid);
    }

    public void startBeaconMonitoring(){
        verifyBluetooth();

        for(String type : beaconTypes){
            beaconManager.getBeaconParsers().add(getParser(type));
        }

        Log.d(TAG, "Starting Beacon monitoring...");

        Region region = new Region("backgroundRegion", null, null, null);
        regionBootstrap = new RegionBootstrap(this, region);

        backgroundPowerSaver = new BackgroundPowerSaver(context);
        beaconManager.bind(this);
    }


    private BeaconParser getParser(String parser){
        String layout;
        switch(parser){
            case BEACON_TYPES.ESTIMOTE:
                layout = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
                break;
            case BEACON_TYPES.IBEACON:
                layout = "m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24";
                break;
            case BEACON_TYPES.EASIBEACON:
                layout = "m:0-3=a7ae2eb7,i:4-19,i:20-21,i:22-23,p:24-24";
                break;
            case BEACON_TYPES.ALTBEACON:
                layout = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
                break;
            default:
                layout = "m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25";
                break;
        }
        return new BeaconParser().setBeaconLayout(layout);
    }

    public void resume(){
        this.monitoring = true;
        beaconManager.setBackgroundMode(false);

    }

    public void pause(){
        this.monitoring = false;
        beaconManager.setBackgroundMode(true);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return false;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {

    }

    @Override
    public Context getApplicationContext() {
        return context;
    }

    @Override
    public void didEnterRegion(Region arg0) {
        Log.d(TAG, "Did enter region.");
        if (monitoring) {
            Log.d(TAG, "I see a beacon again");
            emitter.emit(EVENTS.DID_ENTER_REGION);
        }
    }

    @Override
    public void didExitRegion(Region region) {
        if (monitoring) {
            Log.d(TAG, "I no longer see a beacon.");
            emitter.emit(EVENTS.DID_EXIT_REGION);
        }
    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        if (monitoring) {
            Log.d(TAG, "I have just switched from seeing/not seeing beacons: " + state);
            emitter.emit(EVENTS.REGION_STATE_CHANGE, state);

        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                beaconRangeChange(beacons, region);
            }

        });

        try {
            beaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }

    private void beaconRangeChange(Collection<Beacon> beacons, Region region){
        for(Beacon beacon : beacons){
            String uuid = beacon.getId1().toString();
            DecimalFormat df = new DecimalFormat("#.000");
            String distance = df.format(beacon.getDistance());
            Log.d(TAG, "Beacon (" + uuid.substring(0, 8) + ") is about " + distance + " meters away.");
            Boolean enabled = isBeaconEnabled(uuid);
            if(enabled != null){
                if(enabled) {
                    sendBeaconChangeMessage(beacon);
                }
            }else{
                emitter.emit(EVENTS.DISCOVERED_BEACON, beacon);
            }

        }
    }

    private void sendBeaconChangeMessage(Beacon beacon){
        String uuid = beacon.getId1().toString();
        // Payload
        SaneJSONObject payload = new SaneJSONObject();
        payload.putOrIgnore("platform", "android");
        payload.putDoubleOrIgnore("version", VERSION);

        // Beacon
        SaneJSONObject beaconJSON = new SaneJSONObject();
        beaconJSON.putOrIgnore("uuid", uuid);
        payload.putJSONOrIgnore("beacon", beaconJSON);

        // Proximity
        payload.putJSONOrIgnore("proximity", getProximity(beacon));

        // Message
        SaneJSONObject message = new SaneJSONObject();
        JSONArray devices = new JSONArray();
        devices.put("*");
        message.putArrayOrIgnore("devices", devices);
        message.putJSONOrIgnore("payload", payload);
        message.putOrIgnore("topic", "location_update");

        // Send
        BeaconInfo beaconInfo = getBeaconInfo(this.beaconInfo, uuid);
        Double distance = beacon.getDistance();
        if(beaconInfo.hasChangedDistance(distance)){
            meshblu.message(message);
            emitter.emit(EVENTS.LOCATION_UPDATE, payload, beaconInfo);
        }
        beaconInfo.setLastDistance(distance);
    }

    private SaneJSONObject getProximity(Beacon beacon){
        Double distance = beacon.getDistance();
        String proximity = "Unknown";
        Integer code = 0;
        if(distance < 2){
            code = 1;
            proximity = "Immediate";
        }else if(distance >= 2 && distance < 5){
            code = 2;
            proximity = "Near";
        }else if(distance >= 5){
            code = 3;
            proximity = "Far";
        }
        SaneJSONObject proximityJSON = new SaneJSONObject();
        proximityJSON.putOrIgnore("message", proximity);
        proximityJSON.putIntOrIgnore("code", code);
        proximityJSON.putDoubleOrIgnore("distance", distance);
        proximityJSON.putIntOrIgnore("rssi", beacon.getRssi());
        Long time = new Date().getTime();
        proximityJSON.putOrIgnore("timestamp", new Timestamp(time).toString());
        return proximityJSON;
    }

    private @Nullable Boolean isBeaconEnabled(String uuid){
        BeaconInfo beacon = getBeaconInfo(beaconInfo, uuid);
        if(beacon == null){
            return null;
        }
        return beacon.status;
    }

    public @Nullable BeaconInfo getBeaconInfo(ArrayList<BeaconInfo>beaconInfo, String uuid){
        Iterator<BeaconInfo> iterator = beaconInfo.iterator();
        while(iterator.hasNext()){
            BeaconInfo beacon = iterator.next();
            if(beacon.uuid != null && beacon.uuid.equals(uuid)){
                return beacon;
            }
        }
        return null;
    }

    public void updateInfo(BeaconInfo info){
        Iterator<BeaconInfo> iterator = beaconInfo.iterator();
        Integer index = 0;
        Integer foundIndex = -1;
        while(iterator.hasNext()){
            BeaconInfo beacon = iterator.next();
            if(beacon.uuid != null && beacon.uuid.equals(info.uuid)){
                foundIndex = index;
            }
            index++;
        }
        if(foundIndex >= 0){
            beaconInfo.set(foundIndex, info);
        }
    }

    public void setBeaconInfo(BeaconInfo info){
        BeaconInfo lastInfo = getBeaconInfo(beaconInfo, info.uuid);
        if(lastInfo == null) {
            beaconInfo.add(info);
        }else{
            updateInfo(info);
        }
    }


    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(getApplicationContext()).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {
                    System.exit(0);
                }

            });
            builder.show();

        }

    }

    private void setCredentials(SaneJSONObject deviceJSON){
        meshblu.setCredentials(deviceJSON);
    }
}
