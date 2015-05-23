# KPCC-Android

### Package Name
`com.skyblue.pra.kpcc` is the legacy identifier for this application and can't change.
`org.kpcc.android` is the Java package name for the application.

### API Keys
API keys are stored in `assets/keys.properties` and loaded by `AppConfiguration`. This file isn't included in version control.

keys.properties needs the following keys:

```
mixpanel.token=your-mixpanel-token
parse.applicationId=your-parse-applicationId
parse.clientKey=your-parse-clientKey
desk.email=your-desk-email
desk.password=your-desk-password
```
