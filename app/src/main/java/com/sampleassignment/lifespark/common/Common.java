package com.sampleassignment.lifespark.common;

import java.security.PublicKey;
import java.util.UUID;

public class Common {
    public static final String TRANSMITTER_BLUETOOTH_ADAPTER_NAME = "TRANSMITTER";
    public static final String RECEIVER_BLUETOOTH_ADAPTER_NAME = "RECEIVER";

    public static final UUID MY_UUID = UUID.fromString("12a22272-9827-11eb-a8b3-0242ac130003");
    public static int  appMode;
    public static final int RECEIVER_MODE =0;
   public static final int TRANSMITTER_MODE = 1;
}
