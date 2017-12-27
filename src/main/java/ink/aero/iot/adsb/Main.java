package ink.aero.iot.adsb;

import ink.aero.iot.adsb.adsb.AdsbClient;

public class Main {

    public static void main(String[] args) {
        AdsbClient adsbClient = new AdsbClient();
        adsbClient.bootstrap(args);
    }
}
