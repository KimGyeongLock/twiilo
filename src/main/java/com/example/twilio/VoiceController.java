package com.example.twilio;
// Install the Java helper library from twilio.com/docs/libraries/java
import java.net.URI;
import java.net.URISyntaxException;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.type.PhoneNumber;

public class MakePhoneCall {
    // Get your Account SID and Auth Token from https://twilio.com/console
    // To set up environment variables, see http://twil.io/secure
    public static final String ACCOUNT_SID = "ACf20e3d84432cd2707077712111b5694a";
    public static final String AUTH_TOKEN = "2389a70d9c6920b8ebeebe53a5c0f93c";

    public static void makePhoneCall() throws URISyntaxException {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

        String from = "+16202061103";
        String to = "+821080795427";

        Call call = Call.creator(new PhoneNumber(to), new PhoneNumber(from),
                new URI("http://demo.twilio.com/docs/voice.xml")).create();


        System.out.println(call.getSid());
    }
}