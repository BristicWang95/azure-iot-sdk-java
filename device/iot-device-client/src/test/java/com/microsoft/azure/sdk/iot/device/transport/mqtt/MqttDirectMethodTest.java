// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.device.transport.mqtt;

import com.microsoft.azure.sdk.iot.device.twin.DeviceOperations;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.device.exceptions.TransportException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubTransportMessage;
import mockit.Deencapsulation;
import mockit.Mocked;
import mockit.Verifications;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.microsoft.azure.sdk.iot.device.twin.DeviceOperations.DEVICE_OPERATION_METHOD_RECEIVE_REQUEST;
import static com.microsoft.azure.sdk.iot.device.twin.DeviceOperations.DEVICE_OPERATION_METHOD_SEND_RESPONSE;
import static com.microsoft.azure.sdk.iot.device.twin.DeviceOperations.DEVICE_OPERATION_METHOD_SUBSCRIBE_REQUEST;
import static com.microsoft.azure.sdk.iot.device.twin.DeviceOperations.DEVICE_OPERATION_UNKNOWN;
import static org.junit.Assert.*;

/* Unit tests for MqttDirectMethod
 * Code coverage: 100% methods, 97% lines
 */
public class MqttDirectMethodTest
{
    @Mocked
    MqttAsyncClient mockedMqttConnection;

    @Mocked
    MqttConnectOptions mockConnectOptions;

    private ConcurrentLinkedQueue<Pair<String, byte[]>> testreceivedMessages;

    @Before
    public void baseConstructorExpectation()
    {
        testreceivedMessages = new ConcurrentLinkedQueue<>();
    }

    /*
    Tests_SRS_MqttDeviceMethod_25_001: [**The constructor shall instantiate super class without any parameters.**]**

    Tests_SRS_MqttDeviceMethod_25_002: [**The constructor shall create subscribe and response topics strings for device methods as per the spec.**]**
     */
    @Test
    public void constructorSucceeds(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        //arrange
        String actualSubscribeTopic = "$iothub/methods/POST/#";
        String actualResTopic = "$iothub/methods/res";

        //act
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());

        //assert
        String testSubscribeTopic = Deencapsulation.getField(testMethod, "subscribeTopic");
        String testResTopic = Deencapsulation.getField(testMethod, "responseTopic");

        assertNotNull(testSubscribeTopic);
        assertNotNull(testResTopic);
        assertEquals(testSubscribeTopic, actualSubscribeTopic);
        assertEquals(testResTopic, actualResTopic);

    }

    /*
    Tests_SRS_MqttDeviceMethod_25_014: [**start method shall just mark that this class is ready to start.**]**
     */
    @Test
    public void startSucceedsCalls(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        //arrange
        final MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());

        //act
        testMethod.start();

        //assert
        new Verifications()
        {
            {
                Deencapsulation.invoke(testMethod, "subscribe", anyString);
                times = 0;
            }
        };
    }


    @Test
    public void startSucceedsDoesNotCallsSubscribeIfStarted(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        //arrange
        final MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();
        //act
        testMethod.start();

        //assert
        new Verifications()
        {
            {
                Deencapsulation.invoke(testMethod, "subscribe", anyString);
                maxTimes = 0;
            }
        };
    }

    /*
    Tests_SRS_MqttDeviceMethod_25_020: [**send method shall subscribe to topic from spec ($iothub/methods/POST/#) if the operation is of type DEVICE_OPERATION_METHOD_SUBSCRIBE_REQUEST.**]**
     */
    @Test
    public void sendSucceedsCallsSubscribe(@Mocked final Mqtt mockMqtt) throws IOException, TransportException
    {
        //arrange
        final String actualSubscribeTopic = "$iothub/methods/POST/#";
        byte[] actualPayload = "TestMessage".getBytes(StandardCharsets.UTF_8);
        IotHubTransportMessage testMessage = new IotHubTransportMessage(actualPayload, MessageType.DEVICE_METHODS);
        testMessage.setDeviceOperationType(DEVICE_OPERATION_METHOD_SUBSCRIBE_REQUEST);
        final MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();

        //act
        testMethod.send(testMessage);

        //assert
        new Verifications()
        {
            {
                Deencapsulation.invoke(testMethod, "subscribe", actualSubscribeTopic);
                maxTimes = 1;
            }
        };
    }

    /*
    Tests_SRS_MqttDeviceMethod_25_022: [**send method shall build the publish topic of the format mentioned in spec ($iothub/methods/res/{status}/?$rid={request id}) and publish if the operation is of type DEVICE_OPERATION_METHOD_SEND_RESPONSE.**]**
     */
    @Test
    public void sendSucceedsCallsPublish(@Mocked final Mqtt mockMqtt) throws IOException, TransportException
    {
        final byte[] actualPayload = "TestMessage".getBytes(StandardCharsets.UTF_8);
        final IotHubTransportMessage testMessage = new IotHubTransportMessage(actualPayload, MessageType.DEVICE_METHODS);
        testMessage.setDeviceOperationType(DEVICE_OPERATION_METHOD_SEND_RESPONSE);
        testMessage.setRequestId("ReqId");
        testMessage.setStatus("testStatus");
        final MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();

        //act
        testMethod.send(testMessage);

        //assert
        new Verifications()
        {
            {
                Deencapsulation.invoke(testMethod, "publish", new Class[] {String.class, Message.class}, anyString, any);
                maxTimes = 1;
            }
        };
    }

    @Test (expected = TransportException.class)
    public void sendThrowsOnInvalidOperation(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        final byte[] actualPayload = "TestMessage".getBytes(StandardCharsets.UTF_8);
        final IotHubTransportMessage testMessage = new IotHubTransportMessage(actualPayload, MessageType.DEVICE_METHODS);
        testMessage.setDeviceOperationType(DEVICE_OPERATION_UNKNOWN);
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();

        //act
        testMethod.send(testMessage);
    }

    //Tests_SRS_MqttDeviceMethod_25_018: [send method shall throw a TransportException if device method has not been started yet.]
    @Test (expected = TransportException.class)
    public void sendThrowsIfNotStarted(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        final byte[] actualPayload = "TestMessage".getBytes(StandardCharsets.UTF_8);
        final IotHubTransportMessage testMessage = new IotHubTransportMessage(actualPayload, MessageType.DEVICE_METHODS);
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());

        //act
        testMethod.send(testMessage);
    }

    //Tests_SRS_MqttDeviceMethod_25_016: [send method shall throw an exception if the message is null.]
    @Test (expected = IllegalArgumentException.class)
    public void sendThrowsOnMessageNull() throws TransportException
    {
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();
        //act
        testMethod.send(null);
    }

    /*
    Tests_SRS_MqttDeviceMethod_25_017: [**send method shall return if the message is not of Type DirectMethod.**]**
     */
    @Test
    public void sendDoesNotSendOnDifferentMessageType(@Mocked final Mqtt mockMqtt, final @Mocked IotHubTransportMessage mockedMessage) throws TransportException
    {
        final byte[] actualPayload = "TestMessage".getBytes(StandardCharsets.UTF_8);
        final IotHubTransportMessage testMessage = new IotHubTransportMessage(actualPayload, MessageType.DEVICE_METHODS);
        testMessage.setMessageType(MessageType.DEVICE_TWIN);
        final MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());

        testMethod.start();

        //act
        testMethod.send(testMessage);

        //assert
        new Verifications()
        {
            {
                Deencapsulation.invoke(testMethod, "publish", anyString, mockedMessage);
                maxTimes = 0;
                Deencapsulation.invoke(testMethod, "subscribe", anyString);
                maxTimes = 0;
            }
        };
    }

    //Tests_SRS_MqttDeviceMethod_25_021: [send method shall throw an IllegalArgumentException if message contains a null or empty request id if the operation is of type DEVICE_OPERATION_METHOD_SEND_RESPONSE.]
    @Test (expected = IllegalArgumentException.class)
    public void sendThrowsOnNullRequestID() throws TransportException
    {
        final byte[] actualPayload = "TestMessage".getBytes(StandardCharsets.UTF_8);
        final IotHubTransportMessage testMessage = new IotHubTransportMessage(actualPayload, MessageType.DEVICE_METHODS);
        testMessage.setMessageType(MessageType.DEVICE_METHODS);
        testMessage.setDeviceOperationType(DEVICE_OPERATION_METHOD_SEND_RESPONSE);
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();

        //act
        testMethod.send(testMessage);
    }

    /*
    * Tests_SRS_MQTTDEVICEMETHOD_25_026: [**This method shall call peekMessage to get the message payload from the received Messages queue corresponding to the messaging client's operation.**]**
    * Tests_SRS_MQTTDEVICEMETHOD_25_028: [**If the topic is of type post topic then this method shall parse further for method name and set it for the message by calling setMethodName for the message**]**
    * Tests_SRS_MQTTDEVICEMETHOD_25_030: [**If the topic is of type post topic then this method shall parse further to look for request id which if found is set by calling setRequestId**]**
    * Tests_SRS_MQTTDEVICEMETHOD_25_032: [**If the topic is of type post topic and if method name and request id has been successfully parsed then this method shall set operation type as DEVICE_OPERATION_METHOD_RECEIVE_REQUEST **]**
    */
    @Test
    public void receiveSucceeds() throws TransportException
    {
        //arrange
        String topic = "$iothub/methods/POST/testMethod/?$rid=10";
        byte[] actualPayload = "TestPayload".getBytes(StandardCharsets.UTF_8);
        testreceivedMessages.add(new MutablePair<>(topic, actualPayload));
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        Deencapsulation.setField(testMethod, "receivedMessages", testreceivedMessages);
        testMethod.start();

        //act
        Message testMessage = testMethod.receive();
        IotHubTransportMessage testDMMessage = (IotHubTransportMessage) testMessage;

        //assert
        assertNotNull(testMessage);
        assertEquals(testMessage.getMessageType(), MessageType.DEVICE_METHODS);
        assertEquals(testDMMessage.getRequestId(), String.valueOf(10));
        assertEquals("testMethod", testDMMessage.getMethodName());
        assertEquals(testDMMessage.getDeviceOperationType(), DEVICE_OPERATION_METHOD_RECEIVE_REQUEST);
    }

    // Tests_SRS_MQTTDEVICEMETHOD_25_026: [**This method shall call peekMessage to get the message payload from the received Messages queue corresponding to the messaging client's operation.**]**
    @Test
    public void receiveReturnsNullMessageIfTopicNotFound() throws TransportException
    {
        //arrange
        Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();

        //act
        Message testMessage = testMethod.receive();

        //assert
        assertNull(testMessage);
    }


    //Tests_SRS_MqttDeviceMethod_34_027: [This method shall parse message to look for Post topic ($iothub/methods/POST/) and return null other wise.]
    @Test
    public void receiveReturnsNullMessageIfTopicWasNotPost() throws TransportException
    {
        //arrange
        String topic = "$iothub/methods/Not_POST/testMethod/?$rid=10";
        byte[] actualPayload = "TestPayload".getBytes(StandardCharsets.UTF_8);
        Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
        testreceivedMessages.add(new MutablePair<>(topic, actualPayload));
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testMethod.start();

        //act
        Message actualMessage = testMethod.receive();

        //assert
        assertNull(actualMessage);
    }

    @Test
    public void receiveReturnsEmptyPayLoadIfNullPayloadParsed() throws TransportException
    {
        //arrange
        String topic = "$iothub/methods/POST/testMethod/?$rid=10";
        byte[] actualPayload = "".getBytes(StandardCharsets.UTF_8);
        testreceivedMessages.add(new MutablePair<>(topic, actualPayload));
        MqttDirectMethod testMethod = new MqttDirectMethod("", mockConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        Deencapsulation.setField(testMethod, "receivedMessages", testreceivedMessages);
        testMethod.start();

        //act
        Message testMessage = testMethod.receive();
        IotHubTransportMessage testDMMessage = (IotHubTransportMessage) testMessage;

        //assert
        assertNotNull(testMessage);
        assertEquals(0, testMessage.getBytes().length);
        assertEquals(testMessage.getMessageType(), MessageType.DEVICE_METHODS);
        assertEquals(testDMMessage.getRequestId(), String.valueOf(10));
        assertEquals("testMethod", testDMMessage.getMethodName());
        assertEquals(testDMMessage.getDeviceOperationType(), DEVICE_OPERATION_METHOD_RECEIVE_REQUEST);
    }
}
