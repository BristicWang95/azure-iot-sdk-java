// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.device.transport.mqtt;

import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.auth.IotHubSasTokenAuthenticationProvider;
import com.microsoft.azure.sdk.iot.device.exceptions.IotHubServiceException;
import com.microsoft.azure.sdk.iot.device.exceptions.ProtocolException;
import com.microsoft.azure.sdk.iot.device.exceptions.TransportException;
import com.microsoft.azure.sdk.iot.device.transport.https.IotHubUri;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.device.transport.IotHubListener;
import com.microsoft.azure.sdk.iot.device.transport.IotHubTransportMessage;
import com.microsoft.azure.sdk.iot.device.transport.TransportUtils;
import mockit.*;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
 * Unit tests for MqttIotHubConnection
 * Code coverage: 100% methods, 95% lines
 */
public class MqttIotHubConnectionTest
{
    private static final String SSL_PREFIX = "ssl://";
    private static final String SSL_PORT_SUFFIX = ":8883";
    final String iotHubHostName = "test.host.name";
    final String hubName = "test.iothub";
    final String deviceId = "test-deviceId";
    final String modelId = "dtmi:my:company:namespace;1";
    final String apiVersionPrefix = "?api-version=";
    final String API_VERSION = apiVersionPrefix + TransportUtils.IOTHUB_API_VERSION;
    final char[] expectedToken = "someToken".toCharArray();
    final byte[] expectedMessageBody = { 0x61, 0x62, 0x63 };
    final String userAgentString = "some user agent string";

    @Mocked
    private ProductInfo mockedProductInfo;

    @Mocked
    private ClientConfiguration mockConfig;

    @Mocked
    private MqttTwin mockDeviceTwin;

    @Mocked
    private MqttMessaging mockDeviceMessaging;

    @Mocked
    private MqttDirectMethod mockDeviceMethod;

    @Mocked
    private IotHubUri mockIotHubUri;

    @Mocked
    private SSLContext mockSslContext;

    @Mocked
    private MqttAsyncClient mockedMqttConnection;

    @Mocked
    private Message mockedMessage;

    @Mocked
    private IotHubSasTokenAuthenticationProvider mockedSasTokenAuthenticationProvider;

    @Mocked
    private Queue<ClientConfiguration> mockedQueue;

    @Mocked
    private IotHubListener mockedIotHubListener;

    @Mocked
    private TransportException mockedTransportException;

    @Mocked
    private ProtocolException mockedProtocolConnectionStatusException;

    @Mocked
    private IotHubServiceException mockedIotHubConnectionStatusException;

    @Mocked
    private IotHubTransportMessage mockedTransportMessage;

    @Mocked
    private MessageCallback mockedMessageCallback;

    @Mocked
    private ScheduledExecutorService mockedScheduledExecutorService;

    @Mocked
    MqttConnectOptions mockedConnectOptions;

    @Mocked
    ProxySettings mockProxySettings;

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_001: [The constructor shall save the configuration.]
    @Test
    public void constructorSavesCorrectConfigAndListener() throws IOException, TransportException
    {
        baseExpectations();

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);

        ClientConfiguration actualClientConfig = Deencapsulation.getField(connection, "config");
        ClientConfiguration expectedClientConfig = mockConfig;
        assertEquals(expectedClientConfig, actualClientConfig);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_003: [The constructor shall throw a new IllegalArgumentException
    // if any of the parameters of the configuration is null or empty.]
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsIllegalArgumentExceptionIfHostNameIsEmpty() throws TransportException
    {
        new NonStrictExpectations() {
            {
                mockConfig.getIotHubHostname();
                result = "";
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.getDeviceId();
                result = deviceId;
            }
        };

        new MqttIotHubConnection(mockConfig);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_003: [The constructor shall throw a new IllegalArgumentException
    // if any of the parameters of the configuration is null or empty.]
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsIllegalArgumentExceptionIfConfigIsNull() throws TransportException
    {
        new NonStrictExpectations() {
            {
                mockConfig.getIotHubHostname();
                result = "";
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.getDeviceId();
                result = deviceId;
            }
        };

        new MqttIotHubConnection(null);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_003: [The constructor shall throw a new IllegalArgumentException
    // if any of the parameters of the configuration is null or empty.]
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsIllegalArgumentExceptionIfHostNameIsNull() throws TransportException
    {
        new NonStrictExpectations()
        {
            {
                mockConfig.getIotHubHostname();
                result = null;
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.getDeviceId();
                result = deviceId;
            }
        };

        new MqttIotHubConnection(mockConfig);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_003: [The constructor shall throw a new IllegalArgumentException
    // if any of the parameters of the configuration is null or empty.]
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsIllegalArgumentExceptionIfDeviceIDIsEmpty() throws TransportException
    {
        new NonStrictExpectations()
        {
            {
                mockConfig.getIotHubHostname();
                result = iotHubHostName;
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.getDeviceId();
                result = "";
            }
        };

        new MqttIotHubConnection(mockConfig);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_003: [The constructor shall throw a new IllegalArgumentException
    // if any of the parameters of the configuration is null or empty.]
    @Test(expected = IllegalArgumentException.class)
    public void constructorThrowsIllegalArgumentExceptionIfDeviceIDIsNull() throws TransportException
    {
        new NonStrictExpectations()
        {
            {
                mockConfig.getIotHubHostname();
                result = iotHubHostName;
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.getDeviceId();
                result = null;
            }
        };

        new MqttIotHubConnection(mockConfig);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_004: [The function shall establish an MQTT connection with an IoT Hub
    // using the provided host name, user name, device ID, and sas token.]
    // Tests_SRS_MQTTIOTHUBCONNECTION_25_019: [The function shall establish an MQTT connection with a server uri as ssl://<hostName>:8883 if websocket was not enabled.]
    @Test
    public void openEstablishesConnectionUsingCorrectConfig() throws IOException, TransportException, MqttException
    {
        final char[] expectedSasToken = "someToken".toCharArray();
        final String serverUri = SSL_PREFIX + iotHubHostName + SSL_PORT_SUFFIX;
        baseExpectations();

        new Expectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = ClientConfiguration.AuthType.SAS_TOKEN;
                mockConfig.getSasTokenAuthentication().getSasToken();
                result = expectedSasToken;
                mockConfig.isUsingWebsocket();
                result = false;
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();

        IotHubConnectionStatus expectedState = IotHubConnectionStatus.CONNECTED;
        IotHubConnectionStatus actualState =  Deencapsulation.getField(connection, "state");
        assertEquals(expectedState, actualState);

        new Verifications()
        {
            {
                new MqttAsyncClient(serverUri, deviceId, (MemoryPersistence) any);
                times = 1;
            }
        };
    }

    @Test
    public void openEstablishesConnectionUsingModelId() throws IOException, TransportException, MqttException
    {
        final char[] expectedSasToken = "someToken".toCharArray();
        final String serverUri = SSL_PREFIX + iotHubHostName + SSL_PORT_SUFFIX;
        baseExpectations();

        new Expectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = ClientConfiguration.AuthType.SAS_TOKEN;
                mockConfig.getSasTokenAuthentication().getSasToken();
                result = expectedSasToken;
                mockConfig.isUsingWebsocket();
                result = false;
                mockConfig.getModelId();
                result = modelId;
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();

        IotHubConnectionStatus expectedState = IotHubConnectionStatus.CONNECTED;
        IotHubConnectionStatus actualState =  Deencapsulation.getField(connection, "state");
        assertEquals(expectedState, actualState);

        new Verifications()
        {
            {
                new MqttAsyncClient(serverUri, deviceId, (MemoryPersistence) any);
                times = 1;
            }
        };
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_25_018: [The function shall establish an MQTT WS connection with a server uri as wss://<hostName>/$iothub/websocket?iothub-no-client-cert=true if websocket was enabled.]
    @Test
    public void openEstablishesWSConnectionUsingCorrectConfig(@Mocked final ProxySettings mockedProxySettings) throws IOException, TransportException, MqttException
    {
        final String WS_RAW_PATH = "/$iothub/websocket";
        final String WS_QUERY = "?iothub-no-client-cert=true";
        final String WS_SSLPrefix = "wss://";
        final String serverUri = WS_SSLPrefix + iotHubHostName + WS_RAW_PATH + WS_QUERY;

        baseExpectations();
        openExpectations(mockedProxySettings);

        new NonStrictExpectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = ClientConfiguration.AuthType.SAS_TOKEN;
                mockConfig.getSasTokenAuthentication().getSasToken();
                result = expectedToken;
                mockConfig.isUsingWebsocket();
                result = true;
                mockConfig.getProxySettings();
                result = null;
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();

        IotHubConnectionStatus expectedState = IotHubConnectionStatus.CONNECTED;
        IotHubConnectionStatus actualState =  Deencapsulation.getField(connection, "state");
        assertEquals(expectedState, actualState);

        new Verifications()
        {
            {
                new MqttAsyncClient(serverUri, deviceId, (MemoryPersistence) any);
               times = 1;
            }
        };
    }

    @Test
    public void websocketOpenHasNoQueryStringIfX509Auth(@Mocked final ProxySettings mockedProxySettings) throws IOException, TransportException, MqttException
    {
        final String WS_RAW_PATH = "/$iothub/websocket";
        final String WS_SSLPrefix = "wss://";
        final String serverUri = WS_SSLPrefix + iotHubHostName + WS_RAW_PATH;

        baseExpectations();

        new Expectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = ClientConfiguration.AuthType.X509_CERTIFICATE;
                mockConfig.isUsingWebsocket();
                result = true;
                mockConfig.getProxySettings();
                result = null;
                new MqttAsyncClient(serverUri, deviceId, (MemoryPersistence) any);
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();

        IotHubConnectionStatus expectedState = IotHubConnectionStatus.CONNECTED;
        IotHubConnectionStatus actualState =  Deencapsulation.getField(connection, "state");
        assertEquals(expectedState, actualState);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_007: [If the MQTT connection is already open, the function shall do nothing.]
    @Test
    public void openDoesNothingIfAlreadyOpened() throws IOException, TransportException, MqttException
    {
        baseExpectations();
        openExpectations(null);

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        connection.open();

        new Verifications()
        {
            {
                new MqttAsyncClient(anyString, deviceId, (MemoryPersistence) any);
                maxTimes = 1;
            }
        };
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_007: [If the MQTT connection is closed, the function shall do nothing.]
    @Test
    public void closeDoesNothingIfConnectionNotYetOpened() throws IOException, TransportException
    {
        baseExpectations();

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        connection.close();

        IotHubConnectionStatus expectedState = IotHubConnectionStatus.DISCONNECTED;
        IotHubConnectionStatus actualState =  Deencapsulation.getField(connection, "state");
        assertEquals(expectedState, actualState);

        new Verifications()
        {
            {
                mockDeviceMessaging.stop();
                times = 0;
                mockDeviceMethod.stop();
                times = 0;
                mockDeviceTwin.stop();
                times = 0;
            }
        };
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_007: [If the MQTT connection is closed, the function shall do nothing.]
    @Test
    public void closeDoesNothingIfConnectionAlreadyClosed() throws IOException, TransportException, MqttException
    {
        baseExpectations();
        openExpectations(null);

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        connection.close();
        connection.close();

        new Verifications()
        {
            {
                mockDeviceMessaging.stop();
                maxTimes = 1;
                mockDeviceMethod.stop();
                maxTimes = 1;
                mockDeviceTwin.stop();
                maxTimes = 1;
            }
        };
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_008: [The function shall send an event message to the IoT Hub
    // given in the configuration.]
    // Tests_SRS_MQTTIOTHUBCONNECTION_15_009: [The function shall send the message payload.]
    // Tests_SRS_MQTTIOTHUBCONNECTION_15_011: [If the message was successfully received by the service,
    // the function shall return status code OK.]
    @Test
    public void sendEventSendsMessageCorrectlyToIotHub() throws IOException, TransportException, MqttException
    {
        baseExpectations();
        openExpectations(null);

        final byte[] msgBody = { 0x61, 0x62, 0x63 };
        new NonStrictExpectations()
        {
            {
                mockedMessage.getBytes();
                result = msgBody;
                mockDeviceMessaging.send(mockedMessage);
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        IotHubStatusCode result = connection.sendMessage(mockedMessage);

        assertEquals(IotHubStatusCode.OK, result);

        new Verifications()
        {
            {
                mockDeviceMessaging.send(mockedMessage);
                times = 1;
            }
        };
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_010: [If the message is null or empty,
    // the function shall return status code BAD_FORMAT.]
    @Test
    public void sendEventReturnsBadFormatIfMessageIsNull() throws IOException, TransportException, MqttException
    {
        baseExpectations();
        openExpectations(null);

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        IotHubStatusCode result = connection.sendMessage(null);

        assertEquals(IotHubStatusCode.BAD_FORMAT, result);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_010: [If the message is null or empty,
    // the function shall return status code BAD_FORMAT.]
    @Test
    public void sendEventReturnsBadFormatIfMessageHasNullBody() throws IOException, TransportException, MqttException
    {
        baseExpectations();
        openExpectations(null);

        final byte[] msgBody = null;
        new NonStrictExpectations()
        {
            {
                mockedMessage.getBytes();
                result = msgBody;
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);

        connection.open();
        IotHubStatusCode result = connection.sendMessage(null);

        assertEquals(IotHubStatusCode.BAD_FORMAT, result);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_010: [If the message is null or empty,
    // the function shall return status code BAD_FORMAT.]
    @Test
    public void sendEventReturnsBadFormatIfMessageHasEmptyBody() throws IOException, TransportException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                mockedMessage.getBytes();
                result = new byte[0];
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        IotHubStatusCode result = connection.sendMessage(mockedMessage);

        assertEquals(IotHubStatusCode.BAD_FORMAT, result);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_013: [If the MQTT connection is closed, the function shall throw an IllegalStateException.]
    @Test(expected = IllegalStateException.class)
    public void sendEventFailsIfConnectionNotYetOpened() throws TransportException
    {
        baseExpectations();

        final byte[] msgBody = { 0x61, 0x62, 0x63 };
        new NonStrictExpectations()
        {
            {
                mockedMessage.getBytes();
                result = msgBody;
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        connection.sendMessage(mockedMessage);
    }

    // Tests_SRS_MQTTIOTHUBCONNECTION_15_013: [If the MQTT connection is closed, the function shall throw a IllegalStateException.]
    @Test(expected = IllegalStateException.class)
    public void sendEventFailsIfConnectionClosed() throws TransportException
    {
        baseExpectations();

        final byte[] msgBody = { 0x61, 0x62, 0x63 };
        new NonStrictExpectations()
        {
            {
                mockedMessage.getBytes();
                result = msgBody;
            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);

        connection.open();
        connection.close();
        connection.sendMessage(mockedMessage);
    }

    @Test
    public void sendEventSendsDeviceTwinMessage(@Mocked final IotHubTransportMessage mockDeviceTwinMsg) throws IOException, TransportException, MqttException
    {
        baseExpectations();
        openExpectations(null);

        final byte[] msgBody = { 0x61, 0x62, 0x63 };
        new NonStrictExpectations()
        {
            {
                mockDeviceTwinMsg.getBytes();
                result = msgBody;
                mockDeviceTwinMsg.getMessageType();
                result = MessageType.DEVICE_TWIN;

            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        IotHubStatusCode result = connection.sendMessage(mockDeviceTwinMsg);

        assertEquals(IotHubStatusCode.OK, result);

        new Verifications()
        {
            {
                mockDeviceMethod.send((IotHubTransportMessage)any);
                times = 0;
                mockDeviceMessaging.send(mockDeviceTwinMsg);
                times = 0;
                mockDeviceTwin.start();
                times = 1;
                mockDeviceTwin.send(mockDeviceTwinMsg);
                times = 1;
            }
        };
    }

    @Test
    public void sendEventSendsDeviceMethodMessage(@Mocked final IotHubTransportMessage mockDeviceMethodMsg) throws IOException, TransportException, MqttException
    {
        baseExpectations();
        openExpectations(null);

        final byte[] msgBody = { 0x61, 0x62, 0x63 };
        new NonStrictExpectations()
        {
            {
                mockDeviceMethodMsg.getBytes();
                result = msgBody;
                mockDeviceMethodMsg.getMessageType();
                result = MessageType.DEVICE_METHODS;

            }
        };

        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        IotHubStatusCode result = connection.sendMessage(mockDeviceMethodMsg);

        assertEquals(IotHubStatusCode.OK, result);

        new Verifications()
        {
            {
                mockDeviceMethod.start();
                times = 1;
                mockDeviceMethod.send(mockDeviceMethodMsg);
                times = 1;
                mockDeviceMessaging.send(mockDeviceMethodMsg);
                times = 0;
                mockDeviceTwin.send(mockDeviceMethodMsg);
                times = 0;
            }
        };
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_030: [This function shall instantiate this object's MqttMessaging object with this object as the listeners.]
    @Test
    public void openSavesListenerToMessagingClient() throws IOException, TransportException
    {
        //arrange
        final char[] expectedSasToken = "someToken".toCharArray();
        baseExpectations();

        new Expectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = ClientConfiguration.AuthType.SAS_TOKEN;
                mockConfig.getSasTokenAuthentication().getSasToken();
                result = expectedSasToken;
                mockConfig.isUsingWebsocket();
                result = false;
            }
        };

        final MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);

        //act
        connection.open();

        //assert
        new Verifications()
        {
            {
                new MqttMessaging(anyString, null, anyString, anyBoolean, (MqttConnectOptions) any, (Map) any, (Queue) any);
                times = 1;
            }
        };
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_065: [If the connection opens successfully, this function shall notify the listener that connection was established.]
    @Test
    public void openNotifiesListenerIfConnectionOpenedSuccessfully() throws IOException, TransportException
    {
        //arrange
        final char[] expectedSasToken = "someToken".toCharArray();
        baseExpectations();

        new Expectations()
        {
            {
                mockConfig.getAuthenticationType();
                result = ClientConfiguration.AuthType.SAS_TOKEN;
                mockConfig.getSasTokenAuthentication().getSasToken();
                result = expectedSasToken;
                mockConfig.isUsingWebsocket();
                result = false;
            }
        };

        final MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);

        //act
        connection.open();

        //assert
        new Verifications()
        {
            {
                mockedIotHubListener.onConnectionEstablished(anyString);
                times = 1;
            }
        };
    }


    //Tests_SRS_MQTTIOTHUBCONNECTION_34_049: [If the provided listener object is null, this function shall throw an IllegalArgumentException.]
    @Test (expected = IllegalArgumentException.class)
    public void setListenerThrowsForNullListener() throws TransportException
    {
        //arrange
        baseExpectations();
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);

        //act
        connection.setListener(null);
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_050: [This function shall save the provided listener object.]
    @Test
    public void setListenerSavesListener() throws TransportException
    {
        //arrange
        baseExpectations();
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);

        //act
        connection.setListener(mockedIotHubListener);

        //assert
        IotHubListener actualListener = Deencapsulation.getField(connection, "listener");
        assertEquals(mockedIotHubListener, actualListener);
    }


    //Tests_SRS_MQTTIOTHUBCONNECTION_34_051: [If this object has not received the provided message from the service, this function shall throw a TransportException.]
    @Test (expected = TransportException.class)
    public void sendMessageResultThrowsWhenMessageNotReceivedFirst() throws TransportException, IOException, MqttException
    {
        //arrange
        baseExpectations();
        openExpectations(null);
        final IotHubMessageResult expectedResult = IotHubMessageResult.COMPLETE;
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        Map<IotHubTransportMessage, Integer> receivedMessagesToAcknowledge = new ConcurrentHashMap<>();
        Deencapsulation.setField(connection, "receivedMessagesToAcknowledge", receivedMessagesToAcknowledge);

        //act
        connection.sendMessageResult(mockedTransportMessage, expectedResult);
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_057: [If the provided message or result is null, this function shall throw a TransportException.]
    @Test (expected = TransportException.class)
    public void sendMessageResultThrowsForNullMessage() throws TransportException, IOException, MqttException
    {
        //arrange
        baseExpectations();
        openExpectations(null);
        final IotHubMessageResult expectedResult = IotHubMessageResult.COMPLETE;
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();

        //act
        connection.sendMessageResult(null, expectedResult);
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_057: [If the provided message or result is null, this function shall throw a TransportException.]
    @Test (expected = TransportException.class)
    public void sendMessageResultThrowsForNullResult() throws TransportException, IOException, MqttException
    {
        //arrange
        baseExpectations();
        openExpectations(null);
        final IotHubMessageResult expectedResult = IotHubMessageResult.COMPLETE;
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();

        //act
        connection.sendMessageResult(mockedTransportMessage, null);
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_053: [If the provided message has message type DEVICE_METHODS, this function shall invoke the methods client to send the ack and return the result.]
    //Tests_SRS_MQTTIOTHUBCONNECTION_34_056: [If the ack was sent successfully, this function shall remove the provided message from the saved map of messages to acknowledge.]
    //Tests_SRS_MQTTIOTHUBCONNECTION_34_052: [If this object has received the provided message from the service, this function shall retrieve the Mqtt messageId for that message.]
    @Test
    public void sendMessageResultForMethods() throws TransportException, IOException, MqttException
    {
        //arrange
        baseExpectations();
        openExpectations(null);
        final IotHubMessageResult expectedResult = IotHubMessageResult.COMPLETE;
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        final int expectedMessageId = 12;
        Map<IotHubTransportMessage, Integer> receivedMessagesToAcknowledge = new ConcurrentHashMap<>();
        receivedMessagesToAcknowledge.put(mockedTransportMessage, expectedMessageId);
        Deencapsulation.setField(connection, "receivedMessagesToAcknowledge", receivedMessagesToAcknowledge);
        new NonStrictExpectations()
        {
            {
                mockedTransportMessage.getMessageType();
                result = MessageType.DEVICE_METHODS;

                Deencapsulation.invoke(mockDeviceMethod, "sendMessageAcknowledgement", expectedMessageId);
                Deencapsulation.invoke(mockDeviceTwin, "sendMessageAcknowledgement", expectedMessageId);
                Deencapsulation.invoke(mockDeviceMessaging, "sendMessageAcknowledgement", expectedMessageId);
            }
        };


        //act
        boolean sendMessageResult = connection.sendMessageResult(mockedTransportMessage, expectedResult);

        //assert
        receivedMessagesToAcknowledge = Deencapsulation.getField(connection, "receivedMessagesToAcknowledge");
        assertTrue(receivedMessagesToAcknowledge.isEmpty());
        assertTrue(sendMessageResult);
        new Verifications()
        {
            {
                Deencapsulation.invoke(mockDeviceMethod, "sendMessageAcknowledgement", expectedMessageId);
                times = 1;

                Deencapsulation.invoke(mockDeviceTwin, "sendMessageAcknowledgement", expectedMessageId);
                times = 0;

                Deencapsulation.invoke(mockDeviceMessaging, "sendMessageAcknowledgement", expectedMessageId);
                times = 0;
            }
        };
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_054: [If the provided message has message type DEVICE_TWIN, this function shall invoke the twin client to send the ack and return the result.]
    @Test
    public void sendMessageResultForTwin() throws TransportException, IOException, MqttException
    {
        //arrange
        baseExpectations();
        openExpectations(null);
        final IotHubMessageResult expectedResult = IotHubMessageResult.COMPLETE;
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        final int expectedMessageId = 12;
        Map<IotHubTransportMessage, Integer> receivedMessagesToAcknowledge = new ConcurrentHashMap<>();
        receivedMessagesToAcknowledge.put(mockedTransportMessage, expectedMessageId);
        Deencapsulation.setField(connection, "receivedMessagesToAcknowledge", receivedMessagesToAcknowledge);
        new NonStrictExpectations()
        {
            {
                mockedTransportMessage.getMessageType();
                result = MessageType.DEVICE_TWIN;

                Deencapsulation.invoke(mockDeviceMethod, "sendMessageAcknowledgement", expectedMessageId);
                Deencapsulation.invoke(mockDeviceTwin, "sendMessageAcknowledgement", expectedMessageId);
                Deencapsulation.invoke(mockDeviceMessaging, "sendMessageAcknowledgement", expectedMessageId);
            }
        };


        //act
        boolean sendMessageResult = connection.sendMessageResult(mockedTransportMessage, expectedResult);

        //assert
        assertTrue(sendMessageResult);
        new Verifications()
        {
            {
                Deencapsulation.invoke(mockDeviceMethod, "sendMessageAcknowledgement", expectedMessageId);
                times = 0;

                Deencapsulation.invoke(mockDeviceTwin, "sendMessageAcknowledgement", expectedMessageId);
                times = 1;

                Deencapsulation.invoke(mockDeviceMessaging, "sendMessageAcknowledgement", expectedMessageId);
                times = 0;
            }
        };
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_055: [If the provided message has message type other than DEVICE_METHODS and DEVICE_TWIN, this function shall invoke the telemetry client to send the ack and return the result.]
    @Test
    public void sendMessageResultForTelemetry() throws TransportException, IOException, MqttException
    {
        //arrange
        baseExpectations();
        openExpectations(null);
        final IotHubMessageResult expectedResult = IotHubMessageResult.COMPLETE;
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);
        connection.open();
        final int expectedMessageId = 12;
        Map<IotHubTransportMessage, Integer> receivedMessagesToAcknowledge = new ConcurrentHashMap<>();
        receivedMessagesToAcknowledge.put(mockedTransportMessage, expectedMessageId);
        Deencapsulation.setField(connection, "receivedMessagesToAcknowledge", receivedMessagesToAcknowledge);
        new NonStrictExpectations()
        {
            {
                mockedTransportMessage.getMessageType();
                result = MessageType.DEVICE_TELEMETRY;

                Deencapsulation.invoke(mockDeviceMethod, "sendMessageAcknowledgement", expectedMessageId);
                Deencapsulation.invoke(mockDeviceTwin, "sendMessageAcknowledgement", expectedMessageId);
                Deencapsulation.invoke(mockDeviceMessaging, "sendMessageAcknowledgement", expectedMessageId);
            }
        };


        //act
        boolean sendMessageResult = connection.sendMessageResult(mockedTransportMessage, expectedResult);

        //assert
        assertTrue(sendMessageResult);
        new Verifications()
        {
            {
                Deencapsulation.invoke(mockDeviceMethod, "sendMessageAcknowledgement", expectedMessageId);
                times = 0;

                Deencapsulation.invoke(mockDeviceTwin, "sendMessageAcknowledgement", expectedMessageId);
                times = 0;

                Deencapsulation.invoke(mockDeviceMessaging, "sendMessageAcknowledgement", expectedMessageId);
                times = 1;
            }
        };
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_058: [This function shall attempt to receive a message.]
    //Tests_SRS_MQTTIOTHUBCONNECTION_34_060: [If a transport message is successfully received, and the message has a type of DEVICE_TWIN, this function shall set the callback and callback context of this object from the saved values in config for methods.]
    //Tests_SRS_MQTTIOTHUBCONNECTION_34_063: [If a transport message is successfully received, this function shall notify its listener that a message was received and provide the received message.]
    @Test
    public void onMessageArrivedReceivesMessageForTwin() throws TransportException, IOException, MqttException
    {
        //arrange
        final int expectedMessageId = 2000;
        final Object callbackContext = new Object();
        baseExpectations();
        openExpectations(null);
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        connection.setListener(mockedIotHubListener);
        connection.open();
        new Expectations()
        {
            {
                mockDeviceMethod.receive();
                result = null;

                mockDeviceTwin.receive();
                result = mockedTransportMessage;

                mockedTransportMessage.getMessageType();
                result = MessageType.DEVICE_TWIN;

                mockConfig.getDeviceTwinMessageCallback();
                result = mockedMessageCallback;

                mockedTransportMessage.setMessageCallback(mockedMessageCallback);

                mockConfig.getDeviceTwinMessageContext();
                result = callbackContext;

                mockedTransportMessage.setMessageCallbackContext(callbackContext);

                mockedIotHubListener.onMessageReceived(mockedTransportMessage, null);
            }
        };

        //act
        connection.onMessageArrived(expectedMessageId);

        //assert
        Map<IotHubTransportMessage, Integer> receivedMessagesToAcknowledge = Deencapsulation.getField(connection, "receivedMessagesToAcknowledge");
        assertEquals(1, receivedMessagesToAcknowledge.size());
        assertTrue(receivedMessagesToAcknowledge.containsKey(mockedTransportMessage));
        assertEquals(expectedMessageId, (int) receivedMessagesToAcknowledge.get(mockedTransportMessage));

    }


    //Tests_SRS_MQTTIOTHUBCONNECTION_34_061: [If a transport message is successfully received, and the message has a type of DEVICE_METHODS, this function shall set the callback and callback context of this object from the saved values in config for twin.]
    @Test
    public void onMessageArrivedReceivesMessageForMethods() throws TransportException, IOException, MqttException
    {
        //arrange
        final int expectedMessageId = 2000;
        final Object callbackContext = new Object();
        baseExpectations();
        openExpectations(null);
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        connection.setListener(mockedIotHubListener);
        connection.open();
        new Expectations()
        {
            {
                mockDeviceMethod.receive();
                result = mockedTransportMessage;

                mockedTransportMessage.getMessageType();
                result = MessageType.DEVICE_METHODS;

                mockConfig.getDirectMethodsMessageCallback();
                result = mockedMessageCallback;

                mockedTransportMessage.setMessageCallback(mockedMessageCallback);

                mockConfig.getDirectMethodsMessageContext();
                result = callbackContext;

                mockedTransportMessage.setMessageCallbackContext(callbackContext);

                mockedIotHubListener.onMessageReceived(mockedTransportMessage, null);
            }
        };

        //act
        connection.onMessageArrived(expectedMessageId);
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_062: [If a transport message is successfully received, and the message has a type of DEVICE_TELEMETRY, this function shall set the callback and callback context of this object from the saved values in config for telemetry.]
    @Test
    public void onMessageArrivedReceivesMessageForTelemetry() throws TransportException, IOException, MqttException
    {
        //arrange
        final int expectedMessageId = 2000;
        final Object callbackContext = new Object();
        baseExpectations();
        openExpectations(null);
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        connection.setListener(mockedIotHubListener);
        connection.open();
        new Expectations()
        {
            {
                mockDeviceMethod.receive();
                result = null;

                mockDeviceTwin.receive();
                result = null;

                mockDeviceMessaging.receive();
                result = mockedTransportMessage;

                mockedTransportMessage.getMessageType();
                result = MessageType.DEVICE_TELEMETRY;

                mockedTransportMessage.getInputName();
                result = "inputName";

                mockConfig.getDeviceTelemetryMessageCallback("inputName");
                result = mockedMessageCallback;

                mockedTransportMessage.setMessageCallback(mockedMessageCallback);

                mockedTransportMessage.getInputName();
                result = "inputName";

                mockConfig.getDeviceTelemetryMessageContext("inputName");
                result = callbackContext;

                mockedTransportMessage.setMessageCallbackContext(callbackContext);

                mockedIotHubListener.onMessageReceived(mockedTransportMessage, null);
            }
        };

        //act
        connection.onMessageArrived(expectedMessageId);
    }

    //Tests_SRS_MQTTIOTHUBCONNECTION_34_064: [This function shall return the saved connectionId.]
    @Test
    public void getConnectionIdReturnsSavedConnectionId() throws TransportException
    {
        //arrange
        String expectedConnectionId = "1234";
        baseExpectations();
        MqttIotHubConnection connection = new MqttIotHubConnection(mockConfig);
        Deencapsulation.setField(connection, "connectionId", expectedConnectionId);

        //act
        String actualConnectionId = connection.getConnectionId();

        //assert
        assertEquals(expectedConnectionId, actualConnectionId);
    }

    private void baseExpectations()
    {
        new NonStrictExpectations() {
            {
                mockConfig.getIotHubHostname(); result = iotHubHostName;
                mockConfig.getIotHubName(); result = hubName;
                mockConfig.getDeviceId(); result = deviceId;

                mockConfig.getProductInfo();
                result = mockedProductInfo;

                mockedProductInfo.getUserAgentString();
                result = "someUserAgentString";

                mockedMessage.getBytes();
                result = expectedMessageBody;

                mockedMessage.getMessageType();
                result = MessageType.UNKNOWN;

                mockConfig.getProxySettings();
                result = null;
            }
        };
    }

    private void openExpectations(final ProxySettings proxySettings) throws TransportException, MqttException
    {
        new NonStrictExpectations()
        {
            {
                new MqttAsyncClient(anyString, anyString, (MemoryPersistence) any);
                result = mockedMqttConnection;
                new MqttMessaging(anyString, null, anyString, anyBoolean, (MqttConnectOptions) any, (Map) any, (Queue) any);
                result = mockDeviceMessaging;
                new MqttTwin(anyString, (MqttConnectOptions) any, (Map) any, (Queue) any);
                result = mockDeviceTwin;
                new MqttDirectMethod(anyString, (MqttConnectOptions) any, (Map) any, (Queue) any);
                result = mockDeviceMethod;
                mockDeviceMessaging.start();
                result = null;
            }
        };
    }
}
