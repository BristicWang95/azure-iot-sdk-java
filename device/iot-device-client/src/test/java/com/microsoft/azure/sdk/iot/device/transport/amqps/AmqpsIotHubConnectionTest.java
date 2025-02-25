/*
 * Copyright (c) Microsoft. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.sdk.iot.device.transport.amqps;

import com.microsoft.azure.proton.transport.proxy.impl.ProxyHandlerImpl;
import com.microsoft.azure.proton.transport.proxy.impl.ProxyImpl;
import com.microsoft.azure.proton.transport.ws.WebSocketHandler;
import com.microsoft.azure.proton.transport.ws.impl.WebSocketImpl;
import com.microsoft.azure.sdk.iot.device.ClientConfiguration;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionString;
import com.microsoft.azure.sdk.iot.device.IotHubMessageResult;
import com.microsoft.azure.sdk.iot.device.ProxySettings;
import com.microsoft.azure.sdk.iot.device.auth.IotHubSasTokenAuthenticationProvider;
import com.microsoft.azure.sdk.iot.device.auth.IotHubX509SoftwareAuthenticationProvider;
import com.microsoft.azure.sdk.iot.device.exceptions.ProtocolException;
import com.microsoft.azure.sdk.iot.device.exceptions.TransportException;
import com.microsoft.azure.sdk.iot.device.transport.https.IotHubUri;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.microsoft.azure.sdk.iot.device.transport.IotHubListener;
import com.microsoft.azure.sdk.iot.device.transport.IotHubTransportMessage;
import com.microsoft.azure.sdk.iot.device.transport.amqps.exceptions.AmqpConnectionThrottledException;
import com.microsoft.azure.sdk.iot.device.transport.amqps.exceptions.AmqpSessionWindowViolationException;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Received;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Handler;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.SslDomain;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.impl.TransportInternal;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.apache.qpid.proton.reactor.FlowController;
import org.apache.qpid.proton.reactor.Handshaker;
import org.apache.qpid.proton.reactor.Reactor;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertFalse;


/**
 * Unit tests for AmqpsIotHubConnection.
 * Coverage :
 * 94% method,
 * 88% line
 */

@SuppressWarnings("ThrowableNotThrown")
public class AmqpsIotHubConnectionTest {

    final String hostName = "test.host.name";
    final String hubName = "test.iothub";
    final String deviceId = "test-deviceId";
    final String deviceKey = "test-devicekey?&test";
    final String amqpPort = "5671";
    final String amqpWebSocketPort = "443";

    @Mocked
    AmqpsCbsSessionHandler mockAmqpsCbsSessionHandler;

    @Mocked
    ProtocolException mockedProtocolException;

    @Mocked
    TransportException mockedTransportException;

    @Mocked
    AmqpConnectionThrottledException mockedAmqpConnectionThrottledException;

    @Mocked
    protected Handshaker mockHandshaker;

    @Mocked
    protected FlowController mockFlowController;

    @Mocked
    protected Proton mockProton;

    @Mocked
    protected ReactorRunner mockReactorRunner;

    @Mocked
    protected Reactor mockReactor;

    @Mocked
    protected ClientConfiguration mockConfig;

    @Mocked
    protected IotHubUri mockIotHubUri;

    @Mocked
    protected Message mockProtonMessage;

    @Mocked
    protected AmqpsMessage mockAmqpsMessage;

    @Mocked
    protected Sender mockSender;

    @Mocked
    protected Receiver mockReceiver;

    @Mocked
    protected Connection mockConnection;

    @Mocked
    protected Session mockSession;

    @Mocked
    protected Event mockEvent;

    @Mocked
    protected Future mockReactorFuture;

    @Mocked
    protected ExecutorService mockExecutorService;

    @Mocked
    protected ScheduledExecutorService mockScheduledExecutorService;

    @Mocked
    protected Delivery mockDelivery;

    @Mocked
    protected Transport mockTransport;

    @Mocked
    protected TransportInternal mockTransportInternal;

    @Mocked
    protected Sasl mockSasl;

    @Mocked
    protected SslDomain mockSslDomain;

    @Mocked
    protected SSLContext mockSSLContext;

    @Mocked
    protected WebSocketImpl mockWebSocket;

    @Mocked
    IotHubListener mockedIotHubListener;

    @Mocked
    Target mockTarget;

    @Mocked
    Source mockSource;

    @Mocked
    CountDownLatch mockAuthLatch;

    @Mocked
    CountDownLatch mockWorkerLinkLatch;

    @Mocked
    CountDownLatch mockCloseLatch;

    @Mocked
    AmqpsSendResult mockAmqpsSendResult;

    @Mocked
    IotHubConnectionString mockConnectionString;

    @Mocked
    IotHubSasTokenAuthenticationProvider mockIotHubSasTokenAuthenticationProvider;

    @Mocked
    IotHubX509SoftwareAuthenticationProvider mockIotHubX509AuthenticationProvider;

    @Mocked
    com.microsoft.azure.sdk.iot.device.Message mockIoTMessage;

    @Mocked
    IotHubTransportMessage mockedTransportMessage;

    @Mocked
    ErrorCondition mockedErrorCondition;

    @Mocked
    Symbol mockedSymbol;

    @Mocked
    ApplicationProperties mockedApplicationProperties;

    @Mocked
    Queue<ClientConfiguration> mockedQueue;

    @Mocked
    Rejected mockedRejected;

    @Mocked
    Received mockedReceived;

    @Mocked
    Modified mockedModified;

    @Mocked
    Released mockedReleased;

    @Mocked
    MessageImpl mockedMessageImpl;

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_002: [The constructor shall save the configuration into private member variables.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_003: [The constructor shall initialize the sender and receiver
    // endpoint private member variables using the send/receiveEndpointFormat constants and device id.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_004: [The constructor shall initialize a new Handshaker
    // (Proton) object to handle communication handshake.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_005: [The constructor shall initialize a new FlowController
    // (Proton) object to handle communication flow.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_006: [The constructor shall set its state to DISCONNECTED.]
    @Test
    public void constructorCopiesAllData() throws TransportException
    {
        baseExpectations();
        new NonStrictExpectations()
        {
            {
                mockConfig.isUsingWebsocket();
                result = false;
            }
        };

        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        Set<ClientConfiguration> clientConfigurations = Deencapsulation.getField(connection, "clientConfigurations");
        ClientConfiguration actualConfig = clientConfigurations.iterator().next();
        String actualHostName = Deencapsulation.getField(connection, "hostName");

        assertEquals(mockConfig, actualConfig);
        assertEquals(hostName, actualHostName);

        new Verifications()
        {
            {
                new Handshaker();
                times = 1;
            }
        };

        IotHubConnectionStatus actualState = Deencapsulation.getField(connection, "state");
        assertEquals(IotHubConnectionStatus.DISCONNECTED, actualState);
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_003: [The constructor shall throw TransportException if the Proton reactor creation failed.]
    @Test (expected = TransportException.class)
    public void constructorCreatesProtonReactorThrows() throws TransportException, IOException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                Proton.reactor((AmqpsIotHubConnection)any);
                result = new IOException();
            }
        };

        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        //act
        connection.open();
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_001: [The constructor shall initialize the AmqpsIotHubConnection member variable with the given config.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_017: [The constructor shall set the AMQP socket port using the configuration.]
    @Test
    public void constructorSetsHostNameCorrectlyWhenWebSocketsAreEnabled() throws TransportException
    {
        // arrange
        baseExpectations();
        new NonStrictExpectations()
        {
            {
                mockConfig.getIotHubHostname();
                result = hostName;
                mockConfig.getDeviceId();
                result = deviceId;
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.isUsingWebsocket();
                result = true;
            }
        };

        // act
        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        // assert
        String actualHostName = Deencapsulation.getField(connection, "hostName");
        assertEquals(hostName, actualHostName);
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_001: [The constructor shall initialize the AmqpsIotHubConnection member variable with the given config.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_017: [The constructor shall set the AMQP socket port using the configuration.]
    @Test
    public void constructorSetsHostNameCorrectlyWhenWebSocketsAreDisabled() throws TransportException
    {
        // arrange
        baseExpectations();
        new NonStrictExpectations()
        {
            {
                mockConfig.getIotHubHostname();
                result = hostName;
                mockConfig.getDeviceId();
                result = deviceId;
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.isUsingWebsocket();
                result = false;
            }
        };

        // act
        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig,"");

        // assert
        String actualHostName = Deencapsulation.getField(connection, "hostName");
        assertEquals(hostName, actualHostName);
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_007: [If the AMQPS connection is already open, the function shall do nothing.]
    @Test
    public void openDoesNothingIfTheConnectionIsAlreadyOpen() throws TransportException
    {
        baseExpectations();

        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);

        Deencapsulation.setField(connection, "state", IotHubConnectionStatus.CONNECTED);

        connection.open();

        new Verifications()
        {
            {
                Deencapsulation.invoke(mockConfig, "getSasTokenAuthentication");
                times = 0;
            }
        };
    }

    @Test
    public void openWaitsForAuthLinksToOpen() throws TransportException, InterruptedException
    {
        // arrange
        baseExpectations();
        final CountDownLatch closeLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(0);
        final CountDownLatch workerLinkLatch = new CountDownLatch(0);

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);

        new Expectations()
        {
            {
                new CountDownLatch(anyInt);
                result = authLatch;

                authLatch.await(anyLong, TimeUnit.SECONDS);
                result = true;
            }
        };

        // act
        connection.open();
    }

    @Test
    public void openWaitsForWorkerLinksToOpen() throws TransportException, InterruptedException
    {
        // arrange
        baseExpectations();
        final CountDownLatch closeLatch = new CountDownLatch(1);
        final CountDownLatch authLatch = new CountDownLatch(0);
        final CountDownLatch workerLinkLatch = new CountDownLatch(0);

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);

        new Expectations()
        {
            {
                new CountDownLatch(anyInt);
                result = workerLinkLatch;

                workerLinkLatch.await(anyLong, TimeUnit.SECONDS);
                result = true;
            }
        };

        // act
        connection.open();
    }

    @Test
    public void openClearsLocalStateIfOpenInterrupted() throws TransportException, InterruptedException
    {
        // arrange
        baseExpectations();
        final CountDownLatch workerLinkLatch = new CountDownLatch(0);

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);

        new Expectations()
        {
            {
                new CountDownLatch(anyInt);
                result = workerLinkLatch;

                workerLinkLatch.await(anyLong, TimeUnit.SECONDS);
                result = new InterruptedException("test interrupted exception"); // simulates getting interrupted while opening
            }
        };

        // act
        try
        {
            connection.open();
            fail("expected an exception to be thrown");
        }
        catch (TransportException e)
        {
            // expected exception
        }

        Map<String, AmqpsSessionHandler> sessionHandlers = Deencapsulation.getField(connection, "sessionHandlers");
        Queue<AmqpsSasTokenRenewalHandler> sasTokenRenewalHandlers = Deencapsulation.getField(connection, "sasTokenRenewalHandlers");

        assertTrue(sessionHandlers.isEmpty());
        assertTrue(sasTokenRenewalHandlers.isEmpty());
    }

    @Test
    public void openClearsLocalStateIfWorkerLinksTimeout() throws TransportException, InterruptedException
    {
        // arrange
        baseExpectations();
        final CountDownLatch workerLinkLatch = new CountDownLatch(0);

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);

        new Expectations()
        {
            {
                new CountDownLatch(anyInt);
                result = workerLinkLatch;

                workerLinkLatch.await(anyLong, TimeUnit.SECONDS);
                result = false; // simulates timing out waiting for the worker links to open
            }
        };

        // act
        try
        {
            connection.open();
            fail("expected an exception to be thrown");
        }
        catch (TransportException e)
        {
            // expected exception
        }

        Map<String, AmqpsSessionHandler> sessionHandlers = Deencapsulation.getField(connection, "sessionHandlers");
        Queue<AmqpsSasTokenRenewalHandler> sasTokenRenewalHandlers = Deencapsulation.getField(connection, "sasTokenRenewalHandlers");

        assertTrue(sessionHandlers.isEmpty());
        assertTrue(sasTokenRenewalHandlers.isEmpty());
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_011: [If any exception is thrown while attempting to trigger the reactor, the function shall closeNow the connection and throw an IOException.]
    @Test (expected = IOException.class)
    public void openThrowsIfProtonReactorThrows() throws TransportException
    {
        // arrange
        baseExpectations();

        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        new NonStrictExpectations()
        {
            {
                new ReactorRunner((Reactor) any, (IotHubListener) any, anyString, anyString, anyString, (ReactorRunnerStateCallback) any);
                result = new IOException();
            }
        };

        // act
        connection.open();
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_009: [The function shall trigger the Reactor (Proton) to begin running.]
    @Test
    public void openTriggersProtonReactor(@Mocked final Reactor mockedReactor) throws TransportException, InterruptedException
    {
        //arrange
        baseExpectations();
        final CountDownLatch closeLatch = new CountDownLatch(1);

        new Expectations()
        {
            {
                new CountDownLatch(anyInt);
                result = mockAuthLatch;

                mockAuthLatch.await(anyLong, TimeUnit.MILLISECONDS);
                result = true;
            }
        };

        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);
        setLatches(connection);

        //act
        try
        {
            connection.open();
        }
        catch (TransportException e)
        {
            //exception will be thrown, but this unit test does not care
        }

        //assert
        new Verifications()
        {
            {
                new ReactorRunner((Reactor) any, (IotHubListener) any, anyString, anyString, anyString, (ReactorRunnerStateCallback) any);
                times = 1;
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_010: [The function shall wait for the reactor to be ready and for
    // enough link credit to become available.]
    @Test
    public void openWaitsForReactorToBeReadyAndForEnoughLinkCreditToBeAvailable() throws TransportException, InterruptedException
    {
        //arrange
        baseExpectations();
        final CountDownLatch closeLatch = new CountDownLatch(1);

        new Expectations()
        {
            {
                new CountDownLatch(anyInt);
                result = mockAuthLatch;

                mockAuthLatch.await(anyLong, TimeUnit.MILLISECONDS);
                result = true;
            }
        };
        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);
        setLatches(connection);

        //act
        try
        {
            connection.open();
        }
        catch (TransportException e)
        {
            //exception will be thrown, but we aren't testing for what it is nor do we care that it threw
        }
    }

    @Test
    public void closeStillFreesReactorIfWaitLatchThrows() throws Exception
    {
        baseExpectations();

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "connection", mockConnection);
        Deencapsulation.setField(connection, "reactor", mockReactor);

        this.setLatches(connection);

        new NonStrictExpectations()
        {
            {
                mockCloseLatch.await(anyLong, TimeUnit.MILLISECONDS);
                result = new InterruptedException();
            }
        };

        connection.close();

        new Verifications()
        {
            {
                mockReactor.free();
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_012: [The function shall set the status of the AMQPS connection to DISCONNECTED.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_013: [The function shall close the AmqpsIotHubConnection and the AMQP connection.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_34_014: [If this object's proton reactor is not null, this function shall stop the Proton reactor.]
    @Test
    public void closeClosesAllProtonVariablesAndStopsProtonReactor() throws TransportException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                mockReactorFuture.cancel(true);
                mockExecutorService.shutdownNow();
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        Deencapsulation.setField(connection, "reactor", mockReactor);
        Deencapsulation.setField(connection, "state", IotHubConnectionStatus.CONNECTED);
        Deencapsulation.setField(connection, "executorService", mockExecutorService);
        Deencapsulation.setField(connection, "connection", mockConnection);
        setLatches(connection);

        connection.close();

        IotHubConnectionStatus actualState = Deencapsulation.getField(connection, "state");
        assertEquals(IotHubConnectionStatus.DISCONNECTED, actualState);

        new Verifications()
        {
            {
                mockExecutorService.shutdownNow();
                times = 1;
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_005: [The function shall throw IOException if the executor shutdown is interrupted.]
    @Test (expected = InterruptedException.class)
    public void closeThrowsIfShutdownThrows() throws TransportException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                mockReactorFuture.cancel(true);
                mockExecutorService.shutdown();
                mockExecutorService.shutdownNow();
                result = new InterruptedException();
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        Deencapsulation.setField(connection, "state", IotHubConnectionStatus.CONNECTED);
        Deencapsulation.setField(connection, "executorService", mockExecutorService);
        Deencapsulation.setField(connection, "connection", mockConnection);
        Deencapsulation.setField(connection, "reactor", mockReactor);
        setLatches(connection);

        connection.close();

        IotHubConnectionStatus actualState = Deencapsulation.getField(connection, "state");
        assertEquals(IotHubConnectionStatus.DISCONNECTED, actualState);

        new Verifications()
        {
            {
                mockConnection.close();
                times = 1;
                mockExecutorService.shutdown();
                times = 1;
                mockExecutorService.shutdownNow();
                times = 1;
                mockScheduledExecutorService.shutdownNow();
                times = 1;
                mockReactor.free();
                times = 1;
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_033: [The event handler shall set the current handler to handle the connection events.]
    @Test
    public void onReactorInit(final @Mocked AmqpsSessionHandler mockAmqpsSessionHandler) throws TransportException
    {
        baseExpectations();

        final int sendPeriod = Deencapsulation.getField(AmqpsIotHubConnection.class, "SEND_MESSAGES_PERIOD_MILLIS");
        final int expectedSasTokenRenewalPeriod = 444;

        new NonStrictExpectations()
        {
            {
                mockEvent.getReactor();
                result = mockReactor;
                mockReactor.connectionToHost(anyString, anyInt, (Handler) any);
                mockConfig.getAuthenticationProvider();
                result = mockIotHubSasTokenAuthenticationProvider;
                mockConfig.getSasTokenAuthentication();
                result = mockIotHubSasTokenAuthenticationProvider;
                mockIotHubSasTokenAuthenticationProvider.getMillisecondsBeforeProactiveRenewal();
                result = expectedSasTokenRenewalPeriod;
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Map<String, AmqpsSessionHandler> amqpsSessionHandlerList = new ConcurrentHashMap<>();
        amqpsSessionHandlerList.put("someDevice", mockAmqpsSessionHandler);
        Deencapsulation.setField(connection, "sessionHandlers", amqpsSessionHandlerList);

        connection.onReactorInit(mockEvent);

        new Verifications()
        {
            {
                mockEvent.getReactor();
                mockReactor.schedule(sendPeriod, connection);
                mockReactor.connectionToHost(anyString, anyInt, connection);
            }
        };
    }

    @Test
    public void onReactorInitWithProxySettings(@Mocked final ProxySettings mockProxySettings) throws TransportException
    {
        baseExpectations();

        final int expectedSasTokenRenewalPeriod = 444;
        final String expectedProxyHostname = "127.0.0.1";
        final int expectedProxyPort = 1234;
        new NonStrictExpectations()
        {
            {
                mockConfig.isUsingWebsocket();
                result = true;
                mockConfig.getProxySettings();
                result = mockProxySettings;
                mockProxySettings.getHostname();
                result = expectedProxyHostname;
                mockProxySettings.getPort();
                result = expectedProxyPort;
                mockEvent.getReactor();
                result = mockReactor;
                mockReactor.connectionToHost(anyString, anyInt, (Handler) any);
                mockConfig.getAuthenticationProvider();
                result = mockIotHubSasTokenAuthenticationProvider;
                mockConfig.getSasTokenAuthentication();
                result = mockIotHubSasTokenAuthenticationProvider;
                mockIotHubSasTokenAuthenticationProvider.getMillisecondsBeforeProactiveRenewal();
                result = expectedSasTokenRenewalPeriod;
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        connection.onReactorInit(mockEvent);

        new Verifications()
        {
            {
                mockReactor.connectionToHost(expectedProxyHostname, expectedProxyPort, connection);
            }
        };
    }

    @Test
    public void onReactorInitX509() throws TransportException
    {
        baseExpectations();

        final int sendPeriod = Deencapsulation.getField(AmqpsIotHubConnection.class, "SEND_MESSAGES_PERIOD_MILLIS");
        final int expectedSasTokenRenewalPeriod = 444;

        new NonStrictExpectations()
        {
            {
                mockEvent.getReactor();
                result = mockReactor;
                mockReactor.connectionToHost(anyString, anyInt, (Handler) any);
                mockConfig.getAuthenticationProvider();
                result = mockIotHubX509AuthenticationProvider;
                mockConfig.getSasTokenAuthentication();
                result = mockIotHubSasTokenAuthenticationProvider;
                mockIotHubSasTokenAuthenticationProvider.getMillisecondsBeforeProactiveRenewal();
                result = expectedSasTokenRenewalPeriod;
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        connection.onReactorInit(mockEvent);

        new Verifications()
        {
            {
                mockEvent.getReactor();
                mockReactor.schedule(sendPeriod, connection);

                mockReactor.schedule(expectedSasTokenRenewalPeriod, mockAmqpsCbsSessionHandler);
                times = 0;

                mockReactor.connectionToHost(anyString, anyInt, connection);
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_033: [The event handler shall set the current handler to handle the connection events.]
    @Test
    public void onReactorInitWithWebSockets() throws TransportException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                mockConfig.isUsingWebsocket();
                result = true;
                mockEvent.getReactor();
                result = mockReactor;
                mockReactor.connectionToHost(anyString, anyInt, (Handler) any);
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        connection.onReactorInit(mockEvent);

        new Verifications()
        {
            {
                mockEvent.getReactor();
                times = 1;
                mockReactor.connectionToHost(anyString, anyInt, (Handler) connection);
                times = 1;
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_011: [The function shall call countdown on close latch and open latch.]
    // Test_SRS_AMQPSIOTHUBCONNECTION_12_008: [The function shall set the reactor member variable to null.]
    @Test
    public void onReactorFinalNoReconnect() throws TransportException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                mockConfig.isUsingWebsocket();
                result = true;
                mockCloseLatch.getCount();
                result = 1;
                mockConnection.getReactor();
                result = mockReactor;
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        this.setLatches(connection);
        Deencapsulation.setField(connection, "connection", mockConnection);
        Deencapsulation.setField(connection, "state", IotHubConnectionStatus.DISCONNECTED);

        new MockUp<AmqpsIotHubConnection>()
        {
            @Mock
            void openAsync()
            {
                Deencapsulation.setField(connection, "state", IotHubConnectionStatus.CONNECTED);
            }
        };

        connection.onReactorFinal(mockEvent);

        IotHubConnectionStatus state = Deencapsulation.getField(connection, "state");
        assertEquals(IotHubConnectionStatus.DISCONNECTED, state);

        new Verifications()
        {
            {
                mockCloseLatch.countDown();
                times = 1;
            }
        };
    }

    // Test_SRS_AMQPSIOTHUBCONNECTION_12_010: [The function shall log the error if openAsync failed.]
    @Test
    public void onReactorFinalReconnectFailed() throws TransportException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                mockConfig.isUsingWebsocket();
                result = true;
                mockCloseLatch.getCount();
                result = 1;
                mockConnection.getReactor();
                result = mockReactor;
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        this.setLatches(connection);
        Deencapsulation.setField(connection, "connection", mockConnection);
        Deencapsulation.setField(connection, "state", IotHubConnectionStatus.DISCONNECTED);

        new MockUp<AmqpsIotHubConnection>()
        {
            @Mock
            void openAsync() throws IOException
            {
                throw new IOException();
            }
        };

        connection.onReactorFinal(mockEvent);

        IotHubConnectionStatus state = Deencapsulation.getField(connection, "state");
        assertEquals(IotHubConnectionStatus.DISCONNECTED, state);

        new Verifications()
        {
            {
                mockCloseLatch.countDown();
                times = 1;
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_025: [The event handler shall get the Connection (Proton) object from the event handler and set the host name on the connection.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_15_029: [The event handler shall open the connection.]
    // Tests_SRS_AMQPSIOTHUBCONNECTION_12_009: [The event handler shall call the amqpsConnectionHandler.onConnectionInit function with the connection.]
    @Test
    public void onConnectionInit() throws TransportException
    {
        baseExpectations();

        new NonStrictExpectations()
        {
            {
                mockEvent.getConnection();
                result = mockConnection;
            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.onConnectionInit(mockEvent);

        new Verifications()
        {
            {
                mockEvent.getConnection();
                mockConnection.setHostname(hostName);
                mockConnection.open();
            }
        };
    }

    @Test
    public void onConnectionBoundWebSocketsWithProxy(@Mocked final ProxyHandlerImpl mockProxyHandlerImpl, @Mocked final ProxyImpl mockProxyImpl, @Mocked final ProxySettings mockProxySettings) throws TransportException, IOException
    {
        baseExpectations();
        new Expectations()
        {
            {
                mockConfig.isUsingWebsocket();
                result = true;
                mockEvent.getTransport();
                result = mockTransportInternal;
                new WebSocketImpl(anyInt);
                result = mockWebSocket;
                mockWebSocket.configure(anyString, anyString, anyString, anyInt, anyString, (Map<String, String>) any, (WebSocketHandler) any);
                mockTransportInternal.addTransportLayer(mockWebSocket);
                mockConfig.getProxySettings();
                result = mockProxySettings;
                new ProxyHandlerImpl();
                result = mockProxyHandlerImpl;
                new ProxyImpl();
                result = mockProxyImpl;
                mockProxyImpl.configure(anyString, null, mockProxyHandlerImpl, (Transport) any);

            }
        };

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "amqpsCbsSessionHandler", mockAmqpsCbsSessionHandler);

        connection.onConnectionBound(mockEvent);

        new Verifications()
        {
            {
                mockTransportInternal.addTransportLayer(mockProxyImpl);
                times = 1;
            }
        };
    }

    // Tests_SRS_AMQPSIOTHUBCONNECTION_34_054: [The function shall save the given listener.]
    @Test
    public void setListenerSuccess() throws TransportException
    {
        // arrange
        baseExpectations();
        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        
        // act
        connection.setListener(mockedIotHubListener);

        // assert
        IotHubListener listener = Deencapsulation.getField(connection, "listener");
        assertEquals(mockedIotHubListener, listener);
    }

    //Tests_SRS_AMQPSIOTHUBCONNECTION_34_060 [If the provided event object's transport holds an error condition object, this function shall report the associated TransportException to this object's listeners.]
    @Test
    public void OnTransportErrorReportsErrorCodeIfPresent() throws TransportException
    {
        //arrange
        final StringBuilder methodsCalled = new StringBuilder();
        new MockUp<AmqpsIotHubConnection>()
        {
            @Mock void scheduleReconnection(TransportException throwable)
            {
                methodsCalled.append("scheduleReconnection");
            }
        };
        baseExpectations();
        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        connection.setListener(mockedIotHubListener);
        new NonStrictExpectations()
        {
            {
                mockEvent.getTransport();
                result = mockTransport;
                mockTransport.getCondition();
                result = mockedErrorCondition;
                mockedErrorCondition.getCondition();
                result = mockedSymbol;
                mockedSymbol.toString();
                result = AmqpSessionWindowViolationException.errorCode;
                Deencapsulation.invoke(connection, "scheduleReconnection", new Class[] {TransportException.class}, (TransportException) any);
            }
        };

        //act
        connection.onTransportError(mockEvent);

        //assert
        assertTrue(methodsCalled.toString().contains("scheduleReconnection"));
    }

    // Tests_SRS_AMQPSTRANSPORT_34_072: [If the provided message is not saved in the saved map of messages to acknowledge, this function shall return false.]
    @Test
    public void sendMessageDoesNotAckIfNoAssociatedAmqpsMessage() throws TransportException
    {
        //arrange
        baseExpectations();
        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "state", IotHubConnectionStatus.CONNECTED);

        new NonStrictExpectations()
        {
            {
                mockedTransportMessage.getConnectionDeviceId();
                result = deviceId;
            }
        };

        //act
        connection.sendMessageResult(mockedTransportMessage, IotHubMessageResult.ABANDON);
        Deencapsulation.invoke(connection, "sendQueuedAcknowledgements");

        //assert
        new Verifications()
        {
            {
                mockAmqpsMessage.acknowledge((Accepted) any);
                times = 0;
                mockAmqpsMessage.acknowledge((Rejected) any);
                times = 0;
                mockAmqpsMessage.acknowledge((Released) any);
                times = 0;
            }
        };
    }

    // Tests_SRS_AMQPSTRANSPORT_34_073: [If this object is not CONNECTED, this function shall return false.]
    @Test
    public void sendMessageResultDoesNotAckIfNotConnected() throws TransportException
    {
        //arrange
        baseExpectations();
        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "state", IotHubConnectionStatus.DISCONNECTED);

        new NonStrictExpectations()
        {
            {
                mockedTransportMessage.getConnectionDeviceId();
                result = deviceId;
            }
        };

        //act
        connection.sendMessageResult(mockedTransportMessage, IotHubMessageResult.ABANDON);
        Deencapsulation.invoke(connection, "sendQueuedAcknowledgements");

        //assert
        new Verifications()
        {
            {
                mockAmqpsMessage.acknowledge((Accepted) any);
                times = 0;
                mockAmqpsMessage.acknowledge((Rejected) any);
                times = 0;
                mockAmqpsMessage.acknowledge((Released) any);
                times = 0;
            }
        };
    }

    @Test
    public void SendMessageQueuesMessage() throws TransportException
    {
        //arrange
        Queue<com.microsoft.azure.sdk.iot.device.Message> messagesToSend = new ConcurrentLinkedQueue<>();
        baseExpectations();

        final AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");

        Deencapsulation.setField(connection, "messagesToSend", messagesToSend);

        //act
        connection.sendMessage(mockIoTMessage);

        //assert
        assertEquals(1, messagesToSend.size());
    }

    // Tests_SRS_AMQPSTRANSPORT_34_094: [This function shall return the saved connection id.]
    @Test
    public void getConnectionIdReturnsSavedConnectionId() throws TransportException
    {
        //arrange
        String expectedConnectionId = "1234";
        baseExpectations();
        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "connectionId", expectedConnectionId);

        //act
        String actualConnectionId = connection.getConnectionId();

        //assert
        Assert.assertEquals(expectedConnectionId, actualConnectionId);
    }

    @Test
    public void onMessageAcknowledgedAccepted()
    {
        // arrange
        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);

        new Expectations()
        {
            {
                mockedIotHubListener.onMessageSent(mockIoTMessage, null, null);
            }
        };

        // act
        connection.onMessageAcknowledged(mockIoTMessage, Accepted.getInstance(), null);
    }

    @Test
    public void onMessageAcknowledgedReleased()
    {
        // arrange
        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);

        new Expectations()
        {
            {
                new ProtocolException(anyString);
                result = mockedProtocolException;

                mockedProtocolException.setRetryable(true);

                mockedIotHubListener.onMessageSent(mockIoTMessage, null, mockedProtocolException);
            }
        };

        // act
        connection.onMessageAcknowledged(mockIoTMessage, Released.getInstance(), null);
    }

    @Test
    public void onMessageAcknowledgedRejected(@Mocked final Rejected mockRejected)
    {
        // arrange
        AmqpsIotHubConnection connection = new AmqpsIotHubConnection(mockConfig, "");
        Deencapsulation.setField(connection, "listener", mockedIotHubListener);

        new Expectations()
        {
            {
                mockRejected.getError();

                mockedIotHubListener.onMessageSent(mockIoTMessage, null, (TransportException) any);
            }
        };

        // act
        connection.onMessageAcknowledged(mockIoTMessage, mockRejected, null);
    }

    private void baseExpectations() throws TransportException
    {
        new NonStrictExpectations() {
            {
                mockConfig.getAuthenticationType();
                result = ClientConfiguration.AuthType.SAS_TOKEN;
                mockConfig.getIotHubHostname();
                result = hostName;
                mockConfig.getIotHubName();
                result = hubName;
                mockConfig.getDeviceId();
                result = deviceId;
                mockConfig.isUsingWebsocket();
                result = false;
            }
        };
    }

    private void setLatches(AmqpsIotHubConnection connection)
    {
        Deencapsulation.setField(connection, "authenticationSessionOpenedLatch", mockAuthLatch);
        Map<String, CountDownLatch> deviceSessionCountDownLatches = new HashMap<>();
        deviceSessionCountDownLatches.put("someDevice", mockWorkerLinkLatch);
        Deencapsulation.setField(connection, "deviceSessionsOpenedLatches", deviceSessionCountDownLatches);
        Deencapsulation.setField(connection, "closeReactorLatch", mockCloseLatch);
    }
}