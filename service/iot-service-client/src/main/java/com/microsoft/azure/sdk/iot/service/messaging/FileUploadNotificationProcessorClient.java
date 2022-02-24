// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.sdk.iot.service.messaging;

import com.azure.core.credential.AzureSasCredential;
import com.azure.core.credential.TokenCredential;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import com.microsoft.azure.sdk.iot.service.transport.amqps.EventReceivingConnectionHandler;
import com.microsoft.azure.sdk.iot.service.transport.amqps.ReactorRunner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A client for handling file upload notifications. For more details on what file upload notifications are, see
 * <a href="https://docs.microsoft.com/azure/iot-hub/iot-hub-devguide-file-upload#service-file-upload-notifications">this document</>.
 */
@Slf4j
public class FileUploadNotificationProcessorClient
{
    private static final int START_REACTOR_TIMEOUT_MILLISECONDS = 60 * 1000; // 60 seconds
    private static final int STOP_REACTOR_TIMEOUT_MILLISECONDS = 60 * 1000; // 60 seconds

    private final EventReceivingConnectionHandler eventReceivingConnectionHandler;
    private final Consumer<ErrorContext> errorProcessor; // may be null if user doesn't provide one

    private ReactorRunner reactorRunner;

    public FileUploadNotificationProcessorClient(
        String hostName,
        TokenCredential credential,
        IotHubServiceClientProtocol iotHubServiceClientProtocol,
        Function<FileUploadNotification, AcknowledgementType> fileUploadNotificationProcessor)
    {
        this(hostName, credential, iotHubServiceClientProtocol, fileUploadNotificationProcessor, FileUploadNotificationProcessorClientOptions.builder().build());
    }

    public FileUploadNotificationProcessorClient(
        String hostName,
        TokenCredential credential,
        IotHubServiceClientProtocol protocol,
        Function<FileUploadNotification, AcknowledgementType> fileUploadNotificationProcessor,
        FileUploadNotificationProcessorClientOptions options)
    {
        Objects.requireNonNull(options, "Options cannot be null");

        this.errorProcessor = options.getErrorProcessor();
        this.eventReceivingConnectionHandler =
            new EventReceivingConnectionHandler(
                hostName,
                credential,
                protocol,
                fileUploadNotificationProcessor,
                null,
                this.errorProcessor,
                options.getProxyOptions(),
                options.getSslContext(),
                options.getKeepAliveInterval());
    }

    public FileUploadNotificationProcessorClient(
        String hostName,
        AzureSasCredential sasTokenProvider,
        IotHubServiceClientProtocol protocol,
        Function<FileUploadNotification, AcknowledgementType> fileUploadNotificationProcessor)
    {
        this(hostName, sasTokenProvider, protocol, fileUploadNotificationProcessor, FileUploadNotificationProcessorClientOptions.builder().build());
    }

    public FileUploadNotificationProcessorClient(
        String hostName,
        AzureSasCredential sasTokenProvider,
        IotHubServiceClientProtocol protocol,
        Function<FileUploadNotification, AcknowledgementType> fileUploadNotificationProcessor,
        FileUploadNotificationProcessorClientOptions options)
    {
        Objects.requireNonNull(options, "Options cannot be null");

        this.errorProcessor = options.getErrorProcessor();
        this.eventReceivingConnectionHandler =
            new EventReceivingConnectionHandler(
                hostName,
                sasTokenProvider,
                protocol,
                fileUploadNotificationProcessor,
                null,
                this.errorProcessor,
                options.getProxyOptions(),
                options.getSslContext(),
                options.getKeepAliveInterval());
    }

    public FileUploadNotificationProcessorClient(
        String connectionString,
        IotHubServiceClientProtocol protocol,
        Function<FileUploadNotification, AcknowledgementType> fileUploadNotificationProcessor)
    {
        this(connectionString, protocol, fileUploadNotificationProcessor, FileUploadNotificationProcessorClientOptions.builder().build());
    }

    public FileUploadNotificationProcessorClient(
        String connectionString,
        IotHubServiceClientProtocol protocol,
        Function<FileUploadNotification, AcknowledgementType> fileUploadNotificationProcessor,
        FileUploadNotificationProcessorClientOptions options)
    {
        Objects.requireNonNull(options, "Options cannot be null");

        this.errorProcessor = options.getErrorProcessor();
        this.eventReceivingConnectionHandler =
            new EventReceivingConnectionHandler(
                connectionString,
                protocol,
                fileUploadNotificationProcessor,
                null,
                this.errorProcessor,
                options.getProxyOptions(),
                options.getSslContext(),
                options.getKeepAliveInterval());
    }

    public synchronized void start() throws IotHubException, IOException, InterruptedException
    {
        start(START_REACTOR_TIMEOUT_MILLISECONDS);
    }

    public synchronized void start(int timeoutMilliseconds) throws IotHubException, IOException, InterruptedException
    {
        if (this.reactorRunner != null && this.eventReceivingConnectionHandler != null && this.eventReceivingConnectionHandler.isOpen())
        {
            //already open
            return;
        }

        if (timeoutMilliseconds < 0)
        {
            throw new IllegalArgumentException("timeoutMilliseconds must be greater than or equal to 0");
        }

        AtomicReference<IotHubException> iotHubException = new AtomicReference<>(null);
        AtomicReference<IOException> ioException = new AtomicReference<>(null);

        log.debug("Opening FileUploadNotificationProcessorClient");

        this.reactorRunner = new ReactorRunner(
            this.eventReceivingConnectionHandler.getHostName(),
            "FileUploadNotificationProcessor",
            this.eventReceivingConnectionHandler);

        final CountDownLatch openLatch = new CountDownLatch(1);
        this.eventReceivingConnectionHandler.setOnConnectionOpenedCallback(() -> openLatch.countDown());

        new Thread(() ->
        {
            try
            {
                reactorRunner.run();

                log.trace("FileUploadNotificationProcessorClient Amqp reactor stopped, checking that the connection was opened");
                this.eventReceivingConnectionHandler.verifyConnectionWasOpened();

                log.trace("FileUploadNotificationProcessorClient  reactor did successfully open the connection, returning without exception");
            }
            catch (IOException e)
            {
                log.debug("FileUploadNotificationProcessorClient Amqp connection encountered an exception", e);

                ioException.set(e);
            }
            catch (IotHubException e)
            {
                log.debug("FileUploadNotificationProcessorClient Amqp connection encountered an exception", e);

                iotHubException.set(e);
            }
            finally
            {
                openLatch.countDown();
            }
        }).start();

        if (timeoutMilliseconds == 0)
        {
            // wait indefinitely
            openLatch.await();
        }
        else
        {
            boolean timedOut = !openLatch.await(timeoutMilliseconds, TimeUnit.MILLISECONDS);

            if (timedOut)
            {
                throw new IOException("Timed out waiting for the connection to the service to open");
            }
        }

        if (ioException.get() != null)
        {
            throw ioException.get();
        }

        if (iotHubException.get() != null)
        {
            throw iotHubException.get();
        }

        log.info("Started FileUploadNotificationProcessorClient");
    }

    public synchronized boolean isRunning()
    {
        return this.reactorRunner != null && this.reactorRunner.isRunning();
    }

    public synchronized void stop() throws InterruptedException
    {
        this.stop(STOP_REACTOR_TIMEOUT_MILLISECONDS);
    }

    public synchronized void stop(int timeoutMilliseconds) throws InterruptedException
    {
        if (this.reactorRunner == null)
        {
            return;
        }

        this.reactorRunner.stop(timeoutMilliseconds);
        this.reactorRunner = null;

        log.info("Stopped FileUploadNotificationProcessorClient");
    }
}
