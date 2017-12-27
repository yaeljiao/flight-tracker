/**
 * aliyun.com Inc.
 * Copyright (c) 2004-2017 All Rights Reserved.
 */
package ink.aero.iot.adsb.alicloud;

import ink.aero.iot.adsb.util.SignUtil;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * IoT套件JAVA版设备接入
 */
public class SimpleClient4IOT {

    private final Logger logger = LoggerFactory.getLogger(SimpleClient4IOT.class);

    public static String deviceName = "";
    public static String productKey = "";
    public static String secret = "";

    public static MqttClient sampleClient;

    /**
     * 声明一个发布类的Topic
     */
    private static String pubTopic = "/" + productKey + "/" + deviceName + "/update";

    public void initialize() throws Exception {
        /*
         * Client's ID, suggest MAC address
         */
        String clientId = InetAddress.getLocalHost().getHostAddress();

        //设备认证
        Map<String, String> params = new HashMap<String, String>();
        params.put("productKey", productKey);
        params.put("deviceName", deviceName);
        params.put("clientId", clientId);
        String t = System.currentTimeMillis() + "";
        params.put("timestamp", t);

        //MQTT服务器地址，TLS连接使用ssl开头
        String targetServer = "ssl://" + productKey + ".iot-as-mqtt.cn-shanghai.aliyuncs.com:1883";

        //客户端ID格式，两个||之间的内容为设备端自定义的标记，字符范围[0-9][a-z][A-Z]
        String mqttclientId = clientId + "|securemode=2,signmethod=hmacsha1,timestamp=" + t + "|";
        //mqtt用户名格式
        String mqttUsername = deviceName + "&" + productKey;
        String mqttPassword = SignUtil.sign(params, secret, "hmacsha1");

        System.err.println("mqttclientId=" + mqttclientId);

        //
        connectMqtt(targetServer, mqttclientId, mqttUsername, mqttPassword, deviceName);
    }

    public void connectMqtt(String url, String clientId, String mqttUsername,
                                   String mqttPassword, final String deviceName) throws Exception {
        MemoryPersistence persistence = new MemoryPersistence();
        SSLSocketFactory socketFactory = createSSLSocket();

        //
        sampleClient = new MqttClient(url, clientId, persistence);

        MqttConnectOptions connOpts = new MqttConnectOptions();
        // MQTT 3.1.1
        connOpts.setMqttVersion(4);
        connOpts.setSocketFactory(socketFactory);

        //设置是否自动重连
        connOpts.setAutomaticReconnect(true);

        //如果是true，那么清理所有离线消息，即QoS1或者2的所有未接收内容
        connOpts.setCleanSession(false);

        connOpts.setUserName(mqttUsername);
        connOpts.setPassword(mqttPassword.toCharArray());
        connOpts.setKeepAliveInterval(65);

        logger.info(clientId + "is connecting, target to: " + url);
        sampleClient.connect(connOpts);

        sampleClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                logger.error("connection failed, reason: " + cause);
                cause.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                logger.info("received message, come from TOPIC [" + topic + "], content: ["
                    + new String(message.getPayload(), "UTF-8") + "],  ");
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                logger.info("Message send successful! " + ((token == null || token.getResponse() == null) ? "null"
                    : token.getResponse().getKey()));
            }
        });
        logger.info("connect successfully");
    }

    public void mqttMessage(String message) throws UnsupportedEncodingException, MqttException {

        // 发送一条消息
        MqttMessage mqttMessage = new MqttMessage(message.getBytes("utf-8"));
        mqttMessage.setQos(0);
        // 使用pubTopic进行发布
        sampleClient.publish(pubTopic, mqttMessage);
    }

    private static SSLSocketFactory createSSLSocket() throws Exception {
        SSLContext context = SSLContext.getInstance("TLSV1.2");
        context.init(null, new TrustManager[] {new ALiyunIotX509TrustManager()}, null);
        return context.getSocketFactory();
    }
}
