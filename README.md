# MeshbluBeaconKit-Android

## Installation

- In library `build.gradle` add the dependency:

```gradle
dependencies {
    compile 'com.octoblu:meshblubeaconkit:1.0.0'
    compile 'com.octoblu:sanejsonobject:4.0.1'
}
```

- jCenter will need to be in the repository list in the root project `build.gradle`

```gradle
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        jcenter()
    }
}
```

## App Examples

* [BeaconBlu](https://github.com/octoblu/BeaconBlu-Android)

## Usage (Coming soon)

In your application context.

```java
import com.octoblu.meshblubeaconkit.MeshbluBeacon;
import com.octoblu.meshblubeaconkit.BeaconInfo;
import com.octoblu.sanejsonobject.SaneJSONObject;

```

## API

### Class `MeshbluBeacon`:

`import com.octoblu.meshblubeaconkit.MeshbluBeacon`

- **Constructor:** (Option 1)

```java
SaneJSONObject meshbluConfig = new SaneJSONObject();
meshbluConfig.putOrIgnore("uuid", uuid);
meshbluConfig.putOrIgnore("token", token);
new MeshbluBeacon(meshbluConfig, this); // 'this' is the application context
```

- **Constructor:** (Option 2)

```java
Meshblu meshblu = new Meshblu();
new MeshbluBeacon(meshblu, this); // 'this' is the application context
```

- **On Event:**

```java
meshbluBeacon.on(String event, Emitter.Listener fn)
```

- **Close Events:**

```java
meshbluBeacon.off()
```

- **Start Beacon Monitoring and Ranging:**

Calling this event will start the beacon ranging. You must specify the types of beacons you want to discover at application start.

The following events are emitted when beacons are discovered or are updated.

```java
com.octoblu.meshblubeaconkit.MeshbluBeacon.EVENTS.LOCATION_UPDATE
com.octoblu.meshblubeaconkit.MeshbluBeacon.EVENTS.DID_EXIT_REGION
com.octoblu.meshblubeaconkit.MeshbluBeacon.EVENTS.DID_ENTER_REGION
com.octoblu.meshblubeaconkit.MeshbluBeacon.EVENTS.REGION_STATE_CHANGE
com.octoblu.meshblubeaconkit.MeshbluBeacon.EVENTS.DISCOVERED_BEACON
```

```java
List<String> beaconTypes = new List();
// import static com.octoblu.meshblubeaconkit.MeshbluBeacon.BEACON_TYPES.ESTIMOTE
beaconTypes.add("Estimote");
// import static com.octoblu.meshblubeaconkit.MeshbluBeacon.BEACON_TYPES.IBEACON
beaconTypes.add("iBeacon");
// import static com.octoblu.meshblubeaconkit.MeshbluBeacon.BEACON_TYPES.EASIBEACON
beaconTypes.add("easiBeacon");
// import static com.octoblu.meshblubeaconkit.MeshbluBeacon.BEACON_TYPES.ALTBEACON
beaconTypes.add("AltBeacon");
meshbluBeacon.start(beaconTypes);
```

- **Get Device:**

Calling this event will trigger the event `com.octoblu.meshblubeaconkit.MeshbluBeacon.EVENTS.WHOAMI`

```java
meshbluBeacon.getDevice()
```

- **Generate Token:**

Calling this will generate the a token for the meshblu device then emit the event `com.octoblu.meshblubeaconkit.MeshbluBeacon.EVENTS.GENERATED_TOKEN`

```java
meshbluBeacon.generateToken()
```

- **Resume:**

Resume monitoring

```java
meshbluBeacon.resume()
```

- **Pause:**

Pause monitoring

```java
meshbluBeacon.pause()
```

- **Get Beacon Info:**

Retrieve a beacon by uuid.

```java
BeaconInfo beaconInfo = meshbluBeacon.getBeaconInfo(ArrayList<BeaconInfo>beaconInfo, String uuid)
```

- **Update Beacon Info:**

Update Beacon Info

```java
meshbluBeacon.updateInfo(BeaconInfo info)
```

- **Insert Beacon Info:**

Insert or update Beacon Info

```java
meshbluBeacon.setBeaconInfo(BeaconInfo info)
```

### Class `BeaconInfo`:

`import com.octoblu.meshblubeaconkit.BeaconInfo`

- **Constructor:**

```java
BeaconInfo beaconInfo = new BeaconInfo(new SaneJSONObject());
```

- **Load From JSON:**

Load information from SaneJSONObject(). See JSON format.
```javascript
{
  "name": "BeaconName", // Name of the bluetooth device
  "uuid": "some-uuid", // UUID of Beacon
  "status": true, // If true the beacon is transmitting location
  "sensitivity": 50 // value between 0-100, used for transmitting beacon based on distance changed
  "lastSeen": 1440517059205, // time in ms for the last time the beacon was seen
  "lastUpdated": 1440517059205 // time in ms for the last time the beacon transmitted location
}
```

```java
beaconInfo.loadFromJSON(new SaneJSONObject());
```

- **Set Last Distance:**

```java
beaconInfo.setLastDistance(Double lastDistance);
```

- **Has Changed Recently:**

This will return a boolean if the beacon has changed recently. (within 5 minutes)

```java
Boolean hasChanged = beaconInfo.hasChangedRecently();
```

- **Has Changed Distance:**

This will return a boolean if the beacon has changed significant distance based on sensitivity property.

```java
Boolean hasChangedDistance = beaconInfo.hasChangedDistance();
```

- **To JSON:**

```java
SaneJSONObject jsonObject = beaconInfo.toJSON();
```
