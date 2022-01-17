# VisitCordovaIOS

VisitCordovaIOS is a plugin that can read the Steps, Distance and Sleep Data from Apple HealthKit and inject it into our PWA.

## Installation

Add the plugin to your [existing project](https://cordova.apache.org/docs/en/10.x/guide/cli/#add-plugins).

```bash
cordova plugin add https://github.com/VisitApp/VisitCordovaIOS
```

## Usage
It can be used by executing the following javascript function.
```javascript
cordova.exec(
      null,
      null,
      "CordovaFitnessPlugin", //plugin class name
      "loadVisitWebUrl", //plugin method
      [
        baseUrl, //base url (should change based on the PWA environment)
        firebase_default_client_id, //firebase default_client_id (can be null in case of ios)
        user_token, //token
        userId, //userId
      ] 
    );
```

