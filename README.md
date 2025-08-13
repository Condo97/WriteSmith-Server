# StudyAI Server

The Java server supporting StudyAI. Interacts with OpenAI GPT-4o and DALL-E 3. Fully capable with image send, voice send, and more.

## Requirements

- Java 17
- MySQL

## Installation

1. Clone StudyAI-Server
2. Create and fill a Keys file (see below)
3. Create a MySQL Database (see below)
4. Update MYSQL_URL in Constants.java to point to your database
5. Run the project

## Keys File
This file is to be located in 
```
src > main > java > com > writesmith > keys > Keys.java
```
You must fill the values in this to work.
```java
public class Keys {

    /* APNS */
    public static final String apnsIssuerIDWhichIsTheTeamID
    public static final String appStoreConnectIssuerID
    public static final String apnsAuthKeyID
    public static final String appStoreConnectPrivateKeyID

    /* Google Search */
    public static final String googleSearchAPIKey

    /* Open AI */
    public static final String openAiAPI

    /* OpenRouter */
    public static final String openRouterAPI

    /* IAP */
    public static final String sharedAppSecret
    public static final String sslPassword

    /* MySQL */
    public static final String MYSQL_USER
    public static final String MYSQL_PASS

}
```

## MySQL Database
You must create a MySQL database with tables reflecting the Java objects located in lib/src/main/java/com/writesmith/database/model/objects

## Contribute

We would love you to contribute to **StudyAI Server**. Please feel free to submit a pull request or write my email: acoundou@gmail.com
