// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.device.transport.mqtt;

import com.microsoft.azure.sdk.iot.device.twin.DeviceOperations;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.device.exceptions.TransportException;
import com.microsoft.azure.sdk.iot.device.transport.IotHubTransportMessage;
import mockit.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.microsoft.azure.sdk.iot.device.twin.DeviceOperations.*;
import static org.junit.Assert.*;

/* Unit tests for MqttTwin
 * Code coverage: 100% methods, 94% lines
 */
public class MqttTwinTest
{
    final String resTopic = "$iothub/twin/res/#";
    final String mockVersion = "1.0.1";
    final String mockReqId = String.valueOf(100);

    @Mocked
    IOException mockIOException;

    @Mocked
    MqttAsyncClient mockedMqttConnection;

    @Mocked
    MqttConnectOptions mockedConnectOptions;

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_001: [The constructor shall instantiate super class without any parameters.]
    **Tests_SRS_MQTTDEVICETWIN_25_002: [The constructor shall construct device twin response subscribeTopic.]
     */
    @Test
    public void constructorConstructsSubscribeTopicForTwin(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        //arrange

        //act
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        //assert
        String actualSubscribeTopic = Deencapsulation.getField(testTwin, "subscribeTopic");
        assertNotNull(actualSubscribeTopic);
        assertEquals(actualSubscribeTopic, resTopic);
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_019: [start method shall subscribe to twin response topic ($iothub/twin/res/#) if connected.]
     */
    @Test
    public void startSubscribesToDeviceTwinResponse(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        //arrange
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());

        //act

        testTwin.start();
        //assert

        new Verifications()
        {
            {
                Deencapsulation.invoke(mockMqtt, "subscribe", resTopic);
                times = 1;
            }
        };
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_019: [start method shall subscribe to twin response topic ($iothub/twin/res/#) if connected and throw IoException otherwise.]
     */
    @Test (expected = IOException.class)
    public void startThrowsExceptionIfSubscribesFails(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        //arrange
        new StrictExpectations()
        {
            {
                Deencapsulation.invoke(mockMqtt, "subscribe", resTopic);
                result = mockIOException;
            }
        };

        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());

        //act
        testTwin.start();
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_024: [send method shall build the get request topic of the format mentioned in spec ($iothub/twin/GET/?$rid={request id}) if the operation is of type DEVICE_OPERATION_TWIN_GET_REQUEST.]
     */
    @Test
    public void sendPublishesMessageForGetTwinOnCorrectTopic(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        //arrange
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        final String expectedTopic = "$iothub/twin/GET/?$rid="+mockReqId;
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testTwin.start();
        new NonStrictExpectations()
        {
            {
                mockMessage.getBytes();
                result = actualPayload;
                mockMessage.getMessageType();
                result = MessageType.DEVICE_TWIN;
                mockMessage.getDeviceOperationType();
                result = DEVICE_OPERATION_TWIN_GET_REQUEST;
                mockMessage.getRequestId();
                result = mockReqId;
            }
        };

        //act
        testTwin.send(mockMessage);

        //assert
        new Verifications()
        {
            {
                mockMessage.getBytes();
                times = 1;
                Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, mockMessage);
                times = 1;
            }
        };
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_025: [send method shall throw an IllegalArgumentException if message contains a null or empty request id if the operation is of type DEVICE_OPERATION_TWIN_GET_REQUEST.]
     */
    @Test (expected = IllegalArgumentException.class)
    public void sendThrowsIllegalArgumentExceptionForGetTwinOnCorrectTopicIfReqIdIsNullOrEmpty(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        final String expectedTopic = "$iothub/twin/GET/?$rid=" + mockReqId;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            testTwin.start();
            new NonStrictExpectations()
            {
                {
                    mockMessage.getBytes();
                    result = actualPayload;
                    mockMessage.getMessageType();
                    result = MessageType.DEVICE_TWIN;
                    mockMessage.getDeviceOperationType();
                    result = DEVICE_OPERATION_TWIN_GET_REQUEST;
                    mockMessage.getRequestId();
                    result = null;
                }
            };

            //act
            testTwin.send(mockMessage);
        }
        finally
        {
            //assert
            new Verifications()
            {
                {
                    mockMessage.getBytes();
                    times = 1;
                    Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, actualPayload, mockMessage);
                    times = 0;

                }
            };
        }
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_026: [send method shall build the update reported properties request topic of the format mentioned in spec ($iothub/twin/PATCH/properties/reported/?$rid={request id}&$version={base version}) if the operation is of type DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_REQUEST.]
     */
    @Test
    public void sendPublishesMessageForUpdateReportedPropertiesOnCorrectTopic(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        //arrange
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        final String expectedTopic = "$iothub/twin/PATCH/properties/reported/?$rid="+ mockReqId + "&$version=" + mockVersion;
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testTwin.start();
        new NonStrictExpectations()
        {
            {
                mockMessage.getBytes();
                result = actualPayload;
                mockMessage.getMessageType();
                result = MessageType.DEVICE_TWIN;
                mockMessage.getDeviceOperationType();
                result = DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_REQUEST;
                mockMessage.getRequestId();
                result = mockReqId;
                mockMessage.getVersion();
                result = mockVersion;
            }
        };

        //act
        testTwin.send(mockMessage);

        //assert
        new Verifications()
        {
            {
                mockMessage.getBytes();
                times = 1;
                Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, mockMessage);
                times = 1;
            }
        };
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_027: [send method shall throw an IllegalArgumentException if message contains a null or empty request id if the operation is of type DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_REQUEST.]
     */
    @Test (expected = IllegalArgumentException.class)
    public void sendThrowsIllegalArgumentExceptionForUpdateReportedPropertiesOnCorrectTopicIfReqIdIsNullOrEmpty(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        final String expectedTopic = "$iothub/twin/PATCH/properties/reported/?$rid=" + mockReqId + "&$version=" + mockVersion;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            testTwin.start();
            new NonStrictExpectations()
            {
                {
                    mockMessage.getBytes();
                    result = actualPayload;
                    mockMessage.getMessageType();
                    result = MessageType.DEVICE_TWIN;
                    mockMessage.getDeviceOperationType();
                    result = DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_REQUEST;
                    mockMessage.getRequestId();
                    result = null;
                }
            };

            //act
            testTwin.send(mockMessage);
        }
        finally
        {
             //assert
            new Verifications()
            {
                {
                    mockMessage.getBytes();
                    times = 1;
                    Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, actualPayload, mockMessage);
                    times = 0;

                }
            };
        }
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_029: [send method shall build the subscribe to desired properties request topic of the format mentioned in spec ($iothub/twin/PATCH/properties/desired/?$version={new version}) if the operation is of type DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_REQUEST.]
     */
    @Test
    public void sendDoesNotPublishesMessageForSubscribeToDesiredPropertiesOnCorrectTopic(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        //arrange
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        final String expectedTopic = "$iothub/twin/PATCH/properties/desired/#";
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testTwin.start();
        new NonStrictExpectations()
        {
            {
                mockMessage.getBytes();
                result = actualPayload;
                mockMessage.getMessageType();
                result = MessageType.DEVICE_TWIN;
                mockMessage.getDeviceOperationType();
                result = DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_REQUEST;
                mockMessage.getVersion();
                result = mockVersion;
            }
        };

        //act
        testTwin.send(mockMessage);

        //assert
        new Verifications()
        {
            {
                mockMessage.getBytes();
                times = 1;
                Deencapsulation.invoke(mockMqtt, "subscribe", expectedTopic);
                times = 1;
                Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, mockMessage);
                times = 0;
            }
        };

    }

    //Tests_SRS_MQTTDEVICETWIN_25_031: [send method shall publish a message to the IOT Hub on the respective publish topic by calling method publish().]
    @Test
    public void sendPublishesMessage(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        //arrange
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testTwin.start();
        new NonStrictExpectations()
        {
            {
                mockMessage.getBytes();
                result = actualPayload;
                mockMessage.getMessageType();
                result = MessageType.DEVICE_TWIN;
                mockMessage.getDeviceOperationType();
                result = DEVICE_OPERATION_TWIN_GET_REQUEST;
                mockMessage.getVersion();
                result = mockVersion;
                mockMessage.getRequestId();
                result = "some request id";
                Deencapsulation.invoke(mockMqtt, "publish", new Class[] {String.class, Message.class}, anyString, (Message) any);
            }
        };

        //act
        testTwin.send(mockMessage);

        //assert
        new Verifications()
        {
            {
                mockMessage.getBytes();
                times = 1;
                Deencapsulation.invoke(mockMqtt, "subscribe", anyString);
                times = 0;
                Deencapsulation.invoke(mockMqtt, "publish", anyString, mockMessage);
                times = 1;
            }
        };
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_032: [send method shall subscribe to desired properties by calling method subscribe() on topic "$iothub/twin/PATCH/properties/desired/#" specified in spec if the operation is DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_REQUEST.]
     */
    @Test
    public void sendSubscribesMessageForSubscribeToDesiredPropertiesOnCorrectTopic(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        //arrange
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        final String expectedTopic = "$iothub/twin/PATCH/properties/desired/?$version="+ mockVersion;
        final String expectedSubscribeTopic = "$iothub/twin/PATCH/properties/desired/#";
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        testTwin.start();
        new NonStrictExpectations()
        {
            {
                mockMessage.getMessageType();
                result = MessageType.DEVICE_TWIN;
                mockMessage.getDeviceOperationType();
                result = DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_REQUEST;
                mockMessage.getVersion();
                result = mockVersion;
                mockMessage.getBytes();
                result = actualPayload;
            }
        };

        //act
        testTwin.send(mockMessage);

        //assert
        new Verifications()
        {
            {
                mockMessage.getBytes();
                times = 1;
                Deencapsulation.invoke(mockMqtt, "subscribe", expectedSubscribeTopic);
                times = 1;
                Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, mockMessage);
                times = 0;
            }
        };
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_021: [send method shall throw an IllegalArgumentException if the message is null.]
     */
    @Test (expected = IllegalArgumentException.class)
    public void sendThrowsIllegalArgumentExceptionIfMessageIsNull(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        final byte[] actualPayload = {0x61, 0x62, 0x63};
        final String expectedTopic = "$iothub/twin/PATCH/properties/reported/?$rid=" + mockReqId + "&$version=" + mockVersion;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());

            //act
            testTwin.send(null);
        }
        finally{
            //assert
            new Verifications()
            {
                {
                    mockMessage.getBytes();
                    times = 0;
                    Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, actualPayload, mockMessage);
                    times = 0;
                }
            };
        }
    }
    @Test
    public void sendDoesNotThrowsIoExceptionIfMessageIsEmpty(@Mocked final Mqtt mockMqtt, @Mocked final IotHubTransportMessage mockMessage) throws TransportException
    {
        final byte[] actualPayload = {};
        final String expectedTopic = "$iothub/twin/PATCH/properties/reported/?$rid=" + mockReqId;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            testTwin.start();
            new NonStrictExpectations()
            {
                {
                    mockMessage.getBytes();
                    result = actualPayload;
                    mockMessage.getMessageType();
                    result = MessageType.DEVICE_TWIN;
                    mockMessage.getDeviceOperationType();
                    result = DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_REQUEST;
                    mockMessage.getRequestId();
                    result = mockReqId;
                }
            };

            //act
            testTwin.send(mockMessage);
        }
        finally{
            //assert
            new Verifications()
            {
                {
                    mockMessage.getBytes();
                    times = 1;
                    Deencapsulation.invoke(mockMqtt, "publish", expectedTopic, mockMessage);
                    times = 1;
                }
            };
        }
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_038: [If the topic is of type response topic then this method shall parse further for status and set it for the message by calling setStatus for the message]
     */
    @Test
    public void receiveParsesResponseTopicForGetTwinSucceeds() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "200" + "/?$rid=" + mockReqId;
        IotHubTransportMessage receivedMessage = null;

        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);
            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_GET_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);

            //act
            receivedMessage = testTwin.receive();

        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_GET_RESPONSE);
            assertEquals(receivedMessage.getRequestId(), mockReqId);
            assertEquals("200", receivedMessage.getStatus());
            assertNull(receivedMessage.getVersion());
        }
    }
    @Test
    public void receiveParsesResponseTopicForUpdateReportedPropertiesSucceeds() throws TransportException
    {
        final byte[] actualPayload = "".getBytes(StandardCharsets.UTF_8);
        /*
            The following does not work
            final byte[] actualPayload = null;
         */
        final String expectedTopic = "$iothub/twin/res/" + "200" + "/?$rid=" + mockReqId + "&$version=" + mockVersion;
        IotHubTransportMessage receivedMessage = null;

        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_RESPONSE);
            assertEquals("200", receivedMessage.getStatus());
            assertEquals(receivedMessage.getRequestId(), mockReqId);
            assertEquals(receivedMessage.getVersion(), mockVersion);
        }
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_042: [If the topic is of type patch for desired properties then this method shall parse further to look for version which if found is set by calling setVersion]
     */
    @Test
    public void receiveParsesPatchTopicForDesiredPropertiesNotificationSucceeds() throws TransportException
    {
        final byte[] actualPayload = "UpdateDesiredPropertiesNotificationData".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/PATCH/properties/desired/" + "?$version=" + mockVersion;
        IotHubTransportMessage receivedMessage = null;

        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            //act
            receivedMessage = (IotHubTransportMessage) testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_RESPONSE);
            assertEquals(receivedMessage.getVersion(), mockVersion);
            assertNull(receivedMessage.getRequestId());
            assertNull(receivedMessage.getStatus());
        }
    }
    /*
    **SRS_MQTTDEVICETWIN_25_039: [If the topic is of type response topic and if status is either a non 3 digit number or not found then receive shall throw TransportException ]
     */
    @Test (expected = IllegalArgumentException.class)
    public void receiveParsesResponseTopicMandatoryStatusNotFoundException() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "?$rid=" + mockReqId;
        IotHubTransportMessage receivedMessage = null;

        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            //act
            receivedMessage = (IotHubTransportMessage) testTwin.receive();
        }
        finally
        {
            //assert
            assertNull(receivedMessage);
        }
    }
    /*
    **Tests_SRS_MQTTDEVICETWIN_25_039: [If the topic is of type response topic and if status is either a non 3 digit number or not found then receive shall throw TransportException ]
     */
    @Test (expected = IllegalArgumentException.class)
    public void receiveParsesResponseTopicInvalidStatusThrowsException() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "abc/" + "?$rid=" + mockReqId;
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);
            Deencapsulation.setField(testTwin, "stateLock", new Object());

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNull(receivedMessage);
        }
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_040: [If the topic is of type response topic then this method shall parse further to look for request id which if found is set by calling setRequestId]
     */
    @Test
    public void receiveSetsReqIdOnResTopic() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "200" + "/?$rid=" + mockReqId;
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);
            Deencapsulation.setField(testTwin, "stateLock", new Object());


            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_GET_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);

            //act
            receivedMessage = testTwin.receive();

        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_GET_RESPONSE);
            assertEquals(receivedMessage.getRequestId(), mockReqId);
            assertEquals("200", receivedMessage.getStatus());
            assertNull(receivedMessage.getVersion());
        }
    }

    @Test
    public void receiveDoesNotSetReqIdOnResTopicIfNotFound() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "200";
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_GET_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);
            Deencapsulation.setField(testTwin, "stateLock", new Object());

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_UNKNOWN);
            assertNull(receivedMessage.getRequestId());
            assertEquals("200", receivedMessage.getStatus());
            assertNull(receivedMessage.getVersion());
        }
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_041: [If the topic is of type response topic then this method shall parse further to look for version which if found is set by calling setVersion]
     */
    @Test
    public void receiveSetsVersionOnResTopic() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "201" + "/?$rid=" + mockReqId + "&$version=" + mockVersion;
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_GET_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);

            //act
            receivedMessage = testTwin.receive();

        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_GET_RESPONSE);
            assertEquals(receivedMessage.getRequestId(), mockReqId);
            assertEquals("201", receivedMessage.getStatus());
            assertEquals(receivedMessage.getVersion(), mockVersion);
        }

    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_041: [If the topic is of type response topic then this method shall parse further to look for version which if found is set by calling setVersion]
     */
    @Test
    public void receiveDoesNotSetVersionOnResTopicIfNotFound() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "201" + "/?$rid=" + mockReqId;
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);
            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_GET_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_GET_RESPONSE);
            assertEquals(receivedMessage.getRequestId(), mockReqId);
            assertEquals("201", receivedMessage.getStatus());
            assertNull(receivedMessage.getVersion());
        }
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_044: [If the topic is of type response then this method shall set data and operation type as DEVICE_OPERATION_TWIN_GET_RESPONSE if data is not null]
     */
    @Test
    public void receiveSetsDataForGetTwinResp() throws TransportException
    {
        final byte[] actualPayload = "GetTwinResponseDataContainingDesiredAndReportedPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/res/" + "200" + "/?$rid=" + mockReqId;
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);
            Deencapsulation.setField(testTwin, "stateLock", new Object());

            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_GET_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_GET_RESPONSE);
            assertEquals(receivedMessage.getRequestId(), mockReqId);
            assertEquals("200", receivedMessage.getStatus());
            assertNull(receivedMessage.getVersion());

            byte[] receivedMessageBytes = receivedMessage.getBytes();
            assertEquals(receivedMessageBytes.length, actualPayload.length);
            for (int i = 0; i < receivedMessageBytes.length; i++)
            {
                assertEquals(receivedMessageBytes[i], actualPayload[i]);
            }

        }
    }

    /*
    ** Tests_SRS_MQTTDEVICETWIN_25_045: [If the topic is of type response then this method shall set empty data and operation type as DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_RESPONSE if data is null or empty]
     */
    @Test
    public void receiveDoesNotSetDataForUpdateReportedPropResp() throws TransportException
    {
        final byte[] actualPayload = "".getBytes(StandardCharsets.UTF_8);
        /*
            The following does not work
            final byte[] actualPayload = null;
         */
        final String expectedTopic = "$iothub/twin/res/" + "200" + "/?$rid=" + mockReqId + "&$version=" + mockVersion;

        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            Map<String, DeviceOperations> requestMap = new HashMap<>();
            requestMap.put(mockReqId, DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_REQUEST);
            Deencapsulation.setField(testTwin, "requestMap", requestMap);
            Deencapsulation.setField(testTwin, "stateLock", new Object());

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_UPDATE_REPORTED_PROPERTIES_RESPONSE);
            assertEquals(receivedMessage.getRequestId(), mockReqId);
            assertEquals("200", receivedMessage.getStatus());
            assertEquals(receivedMessage.getVersion(), mockVersion);

            byte[] receivedMessageBytes = receivedMessage.getBytes();
            assertEquals(0, receivedMessageBytes.length);
        }
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_046: [If the topic is of type patch for desired properties then this method shall set the data and operation type as DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_RESPONSE if data is not null or empty]
     */
    @Test
    public void receiveSetsDataForDesiredPropNotifResp() throws TransportException
    {
        final byte[] actualPayload = "NotificationResponseDataContainingDesiredPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/PATCH/properties/desired/";
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            //act
            receivedMessage = testTwin.receive();

        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_RESPONSE);
            assertNull(receivedMessage.getRequestId());
            assertNull(receivedMessage.getStatus());
            assertNull(receivedMessage.getVersion());

            byte[] receivedMessageBytes = receivedMessage.getBytes();
            assertEquals(receivedMessageBytes.length, actualPayload.length);
            for (int i = 0; i < receivedMessageBytes.length; i++)
            {
                assertEquals(receivedMessageBytes[i], actualPayload[i]);
            }
        }
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_042: [If the topic is of type patch for desired properties then this method shall parse further to look for version which if found is set by calling setVersion]
     */
    @Test
    public void receiveDoesNotSetVersionForDesiredPropNotifRespIfNotFound() throws TransportException
    {
        final byte[] actualPayload = "NotificationResponseDataContainingDesiredPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/PATCH/properties/desired/";
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_RESPONSE);
            assertNull(receivedMessage.getRequestId());
            assertNull(receivedMessage.getStatus());
            assertNull(receivedMessage.getVersion());

            byte[] receivedMessageBytes = receivedMessage.getBytes();
            assertEquals(receivedMessageBytes.length, actualPayload.length);
            for (int i = 0; i < receivedMessageBytes.length; i++)
            {
                assertEquals(receivedMessageBytes[i], actualPayload[i]);
            }
        }
    }

    /*
    **Tests_SRS_MQTTDEVICETWIN_25_042: [If the topic is of type patch for desired properties then this method shall parse further to look for version which if found is set by calling setVersion]
     */
    @Test
    public void receiveSetVersionForDesiredPropNotifRespIfFound() throws TransportException
    {
        final byte[] actualPayload = "NotificationResponseDataContainingDesiredPropertiesDocument".getBytes(StandardCharsets.UTF_8);
        final String expectedTopic = "$iothub/twin/PATCH/properties/desired/" + "?$version=" + mockVersion ;
        IotHubTransportMessage receivedMessage = null;
        try
        {
            //arrange
            MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
            Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
            testreceivedMessages.add(new MutablePair<>(expectedTopic, actualPayload));
            Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);
            Deencapsulation.setField(testTwin, "stateLock", new Object());

            //act
            receivedMessage = testTwin.receive();
        }
        finally
        {
            //assert
            assertNotNull(receivedMessage);
            assertSame(receivedMessage.getMessageType(), MessageType.DEVICE_TWIN);
            assertSame(receivedMessage.getDeviceOperationType(), DEVICE_OPERATION_TWIN_SUBSCRIBE_DESIRED_PROPERTIES_RESPONSE);
            assertNull(receivedMessage.getRequestId());
            assertNull(receivedMessage.getStatus());
            assertEquals(receivedMessage.getVersion(), mockVersion);

            byte[] receivedMessageBytes = receivedMessage.getBytes();
            assertEquals(receivedMessageBytes.length, actualPayload.length);
            for (int i = 0; i < receivedMessageBytes.length; i++)
            {
                assertEquals(receivedMessageBytes[i], actualPayload[i]);
            }
        }
    }

    /*
     * Tests_SRS_MQTTDEVICETWIN_34_034: [If the call peekMessage returns null or empty string then this method shall do nothing and return null]
     */
    @Test
    public void receiveReturnsNullMessageIfTopicNotFound(@Mocked final Mqtt mockMqtt) throws TransportException
    {
        //arrange
        MqttTwin testTwin = new MqttTwin("", mockedConnectOptions, new HashMap<Integer, Message>(), new ConcurrentLinkedQueue<Pair<String, byte[]>>());
        Queue<Pair<String, byte[]>> testreceivedMessages = new ConcurrentLinkedQueue<>();
        testreceivedMessages.add(new MutablePair<String, byte[]>(null, new byte[5]));
        Deencapsulation.setField(testTwin, "receivedMessages", testreceivedMessages);
        Deencapsulation.setField(testTwin, "stateLock", new Object());
        Deencapsulation.setField(testTwin, "receivedMessagesLock", new Object());

        //act
        //assert
        assertNull(testTwin.receive());
    }
}