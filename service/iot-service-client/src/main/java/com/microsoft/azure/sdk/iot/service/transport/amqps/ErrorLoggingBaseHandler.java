/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.sdk.iot.service.transport.amqps;

import com.microsoft.azure.sdk.iot.service.messaging.ErrorContext;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.proton.engine.BaseHandler;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;

import java.util.function.Consumer;

@Slf4j
public class ErrorLoggingBaseHandler extends BaseHandler
{
    protected ProtonJExceptionParser protonJExceptionParser;
    protected Consumer<ErrorContext> errorProcessor;

    @Override
    public void onLinkRemoteClose(Event event)
    {
        if (event.getLink().getLocalState().equals(EndpointState.ACTIVE))
        {
            protonJExceptionParser = new ProtonJExceptionParser(event);
            if (protonJExceptionParser.getError() == null)
            {
                log.error("Amqp link {} was closed remotely", event.getLink().getName());
            }
            else
            {
                if (event.getLink().getName() != null)
                {
                    log.error("Amqp link {} was closed remotely with exception {} with description {}", event.getLink().getName(), protonJExceptionParser.getError(), protonJExceptionParser.getErrorDescription());
                }
                else
                {
                    log.error("Unknown amqp link was closed remotely with exception {} with description {}", protonJExceptionParser.getError(), protonJExceptionParser.getErrorDescription());
                }
            }

            this.processError();
        }
        else
        {
            // If the link closes remotely, but local state is already closed, then no error occurred.
            log.trace("Amqp link {} closed remotely after being closed locally", event.getLink().getName());
        }
    }

    @Override
    public void onSessionRemoteClose(Event event)
    {
        if (event.getSession().getLocalState().equals(EndpointState.ACTIVE))
        {
            protonJExceptionParser = new ProtonJExceptionParser(event);
            if (protonJExceptionParser.getError() == null)
            {
                log.error("Amqp session was closed remotely with an unknown exception");
            }
            else
            {
                log.error("Amqp session was closed remotely with exception {} with description {}", protonJExceptionParser.getError(), protonJExceptionParser.getErrorDescription());
            }

            this.processError();
        }
        else
        {
            // If the session closes remotely, but local state is already closed, then no error occurred.
            log.trace("Amqp session closed remotely after being closed locally");
        }
    }

    @Override
    public void onConnectionRemoteClose(Event event)
    {
        if (event.getConnection().getLocalState().equals(EndpointState.ACTIVE))
        {
            protonJExceptionParser = new ProtonJExceptionParser(event);
            if (protonJExceptionParser.getError() == null)
            {
                log.error("Amqp connection was closed remotely with an unknown exception");
            }
            else
            {
                log.error("Amqp connection was closed remotely with exception {} with description {}", protonJExceptionParser.getError(), protonJExceptionParser.getErrorDescription());
            }

            this.processError();
        }
        else
        {
            // If the connection closes remotely, but local state is already closed, then no error occurred.
            log.trace("Amqp connection closed remotely after being closed locally");
        }
    }

    @Override
    public void onTransportError(Event event)
    {
        protonJExceptionParser = new ProtonJExceptionParser(event);
        if (protonJExceptionParser.getError() == null)
        {
            log.error("Amqp transport threw an unknown exception");
        }
        else
        {
            log.error("Amqp transport threw exception {} with description {}", protonJExceptionParser.getError(), protonJExceptionParser.getErrorDescription());
        }

        this.processError();
    }

    private void processError()
    {
        if (this.errorProcessor != null)
        {
            if (this.protonJExceptionParser.getIotHubException() != null)
            {
                this.errorProcessor.accept(new ErrorContext(this.protonJExceptionParser.getIotHubException()));
            }
            else if (this.protonJExceptionParser.getNetworkException() != null)
            {
                this.errorProcessor.accept(new ErrorContext(this.protonJExceptionParser.getNetworkException()));
            }
        }
    }
}
