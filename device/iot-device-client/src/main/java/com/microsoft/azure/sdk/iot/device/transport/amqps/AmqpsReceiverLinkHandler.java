// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.device.transport.amqps;

import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageProperty;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.device.transport.IotHubTransportMessage;
import com.microsoft.azure.sdk.iot.device.transport.TransportUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.Properties;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.engine.*;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
abstract class AmqpsReceiverLinkHandler extends BaseHandler
{
    static final String VERSION_IDENTIFIER_KEY = "com.microsoft:client-version";
    private static final String API_VERSION_KEY = "com.microsoft:api-version";

    private static final String TO_KEY = "to";
    private static final String USER_ID_KEY = "userId";
    private static final String AMQPS_APP_PROPERTY_PREFIX = "iothub-app-";

    // Upon opening a receiver link, the client must extend link credit to the service so that the service
    // can send messages over that link to the client. Each "link credit" corresponds to 1 service to client message.
    // Upon receiving a message over a receiving link, a credit should be refunded to the service so that
    // this initial credit doesn't run out.
    private final Map<Message, AmqpsMessage> receivedMessagesMap = new ConcurrentHashMap<>();
    final Map<Symbol, Object> amqpProperties;
    final String linkCorrelationId;
    String receiverLinkAddress;
    final Receiver receiverLink;
    private final AmqpsLinkStateCallback amqpsLinkStateCallback;

    AmqpsReceiverLinkHandler(Receiver receiver, AmqpsLinkStateCallback amqpsLinkStateCallback, String linkCorrelationId)
    {
        this.amqpProperties = new HashMap<>();
        this.amqpProperties.put(Symbol.getSymbol(API_VERSION_KEY), TransportUtils.IOTHUB_API_VERSION);
        this.receiverLink = receiver;
        this.linkCorrelationId = linkCorrelationId;
        this.amqpsLinkStateCallback = amqpsLinkStateCallback;

        //All events that happen to this receiver link will be handled in this class (onLinkRemoteOpen, for instance)
        BaseHandler.setHandler(receiver, this);

        //This flow controller handles all link credit handling on our behalf
        add(new LoggingFlowController(this.linkCorrelationId));
    }

    @Override
    public void onLinkRemoteOpen(Event event)
    {
        log.debug("{} receiver link with address {} and link correlation id {} was successfully opened", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
        this.amqpsLinkStateCallback.onLinkOpened(this);

        boolean hasFlowController = false;
        Iterator<Handler> children = children();
        while (children.hasNext())
        {
            hasFlowController |= children.next() instanceof LoggingFlowController;
        }

        if (!hasFlowController)
        {
            log.trace("No flow controller detected in {} link with address {} and link correlation id {}. Adding a new flow controller.", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
            add(new LoggingFlowController(this.linkCorrelationId));
        }
    }

    @Override
    public void onLinkLocalOpen(Event event)
    {
        log.trace("{} receiver link with address {} and link correlation id {} opened locally", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
    }

    @Override
    public void onDelivery(Event event)
    {
        // Safe to cast as receiver here since this event only fires when a message is ready to be received over this receiver link
        Receiver receiverLink = (Receiver) event.getLink();
        AmqpsMessage amqpsMessage = this.getMessageFromReceiverLink(receiverLink);

        if (amqpsMessage == null)
        {
            // This happens if the delivery was a partial delivery and a full message can't be formed just yet.
            // onDelivery will be called by the reactor again with another chunk of the partial delivery, and will
            // continue to be called until it is fully formed.
            return;
        }

        IotHubTransportMessage iotHubMessage = this.protonMessageToIoTHubMessage(amqpsMessage);
        this.receivedMessagesMap.put(iotHubMessage, amqpsMessage);
        this.amqpsLinkStateCallback.onMessageReceived(iotHubMessage);

        log.trace("Current link credit on {} receiver link with address {} and link correlation id {} is {}", this.getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId, receiverLink.getCredit());
    }

    @Override
    public void onLinkInit(Event event)
    {
        // This function is called per sender/receiver link after it is instantiated, and before it is opened.
        // It sets some properties on that link, and then opens it.
        Link link = event.getLink();

        Source source = new Source();
        source.setAddress(this.receiverLinkAddress);

        link.setSource(source);

        link.setReceiverSettleMode(ReceiverSettleMode.FIRST);
        link.setProperties(this.amqpProperties);
        link.open();
        log.trace("Opening {} receiver link with address {} and link correlation id {}", this.getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
    }

    @Override
    public void onLinkRemoteClose(Event event)
    {
        Link link = event.getLink();
        if (link.getLocalState() == EndpointState.ACTIVE)
        {
            log.debug("{} receiver link with address {} and link correlation id {} was closed remotely unexpectedly", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
            link.close();
            clearHandlers();
            this.amqpsLinkStateCallback.onLinkClosedUnexpectedly(link.getRemoteCondition());
        }
        else
        {
            log.trace("Closing amqp session now that {} receiver link with address {} and link correlation id {} has closed remotely and locally", getLinkInstanceType(), this.receiverLinkAddress, linkCorrelationId);
            event.getSession().close();
        }
    }

    @Override
    public void onLinkLocalClose(Event event)
    {
        Link link = event.getLink();
        if (link.getRemoteState() == EndpointState.CLOSED)
        {
            log.trace("Closing amqp session now that {} receiver link with address {} and link correlation id {} has closed remotely and locally", getLinkInstanceType(), this.receiverLinkAddress, linkCorrelationId);
            event.getSession().close();
        }
        else
        {
            log.trace("{} receiver link with address {} and link correlation id {} was closed locally", this.getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
        }
    }

    public boolean acknowledgeReceivedMessage(IotHubTransportMessage message, DeliveryState ackType)
    {
        if (this.receivedMessagesMap.containsKey(message))
        {
            this.receivedMessagesMap.remove(message).acknowledge(ackType);
            return true;
        }

        return false;
    }

    abstract String getLinkInstanceType();

    AmqpsMessage getMessageFromReceiverLink(Receiver receiver)
    {
        Delivery delivery = receiver.current();

        if (delivery == null)
        {
            return null;
        }

        if (delivery.isReadable() && !delivery.isPartial())
        {
            int size = delivery.pending();
            byte[] buffer = new byte[size];
            int bytesRead = receiver.recv(buffer, 0, buffer.length);

            log.trace("read {} bytes from {} receiver link with address {} and link correlation id {}", bytesRead, getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);

            boolean receiverLinkAdvanced = receiver.advance();

            if (!receiverLinkAdvanced)
            {
                log.warn("{} receiver link with link correlation id {} did not advance after bytes were read from it", getLinkInstanceType(), this.linkCorrelationId);
            }

            if (size != bytesRead)
            {
                log.warn("Amqp read operation on {} receiver link with link correlation id {} did not read the expected amount of bytes. Read {} but expected {}", getLinkInstanceType(), this.linkCorrelationId, bytesRead, size);
            }

            AmqpsMessage amqpsMessage = new AmqpsMessage();
            amqpsMessage.setDelivery(delivery);
            amqpsMessage.decode(buffer, 0, bytesRead);

            return amqpsMessage;
        }

        if (delivery.isPartial())
        {
            log.trace("Partial delivery received on {} receiver link with address {} and link correlation id {}.", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
        }
        else
        {
            // not partial, but not readable either
            log.warn("Unreadable delivery received on {} receiver link with address {} and link correlation id {}.", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
        }

        return null;
    }

    IotHubTransportMessage protonMessageToIoTHubMessage(AmqpsMessage protonMsg)
    {
        log.trace("Converting proton message to iot hub message for {} receiver link with address {} and link correlation id {}. Proton message correlation id {}", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId, protonMsg.getCorrelationId());
        byte[] msgBody;
        Data d = (Data) protonMsg.getBody();
        if (d != null)
        {
            Binary b = d.getValue();
            msgBody = new byte[b.getLength()];
            ByteBuffer buffer = b.asByteBuffer();
            buffer.get(msgBody);
        }
        else
        {
            msgBody = new byte[0];
        }

        IotHubTransportMessage iotHubTransportMessage = new IotHubTransportMessage(msgBody, MessageType.UNKNOWN);

        Properties properties = protonMsg.getProperties();
        if (properties != null)
        {
            if (properties.getCorrelationId() != null)
            {
                iotHubTransportMessage.setCorrelationId(properties.getCorrelationId().toString());
            }

            if (properties.getMessageId() != null)
            {
                iotHubTransportMessage.setMessageId(properties.getMessageId().toString());
            }

            if (properties.getTo() != null)
            {
                iotHubTransportMessage.setProperty(AMQPS_APP_PROPERTY_PREFIX + TO_KEY, properties.getTo());
            }

            if (properties.getUserId() != null)
            {
                iotHubTransportMessage.setProperty(AMQPS_APP_PROPERTY_PREFIX + USER_ID_KEY, properties.getUserId().toString());
            }

            if (properties.getContentEncoding() != null)
            {
                iotHubTransportMessage.setContentEncoding(properties.getContentEncoding().toString());
            }

            if (properties.getContentType() != null)
            {
                iotHubTransportMessage.setContentType(properties.getContentType().toString());
            }
        }

        if (protonMsg.getApplicationProperties() != null)
        {
            Map<String, Object> applicationProperties = protonMsg.getApplicationProperties().getValue();
            for (Map.Entry<String, Object> entry : applicationProperties.entrySet())
            {
                String propertyKey = entry.getKey();
                if (propertyKey.equalsIgnoreCase(MessageProperty.CONNECTION_DEVICE_ID))
                {
                    iotHubTransportMessage.setConnectionDeviceId(entry.getValue().toString());
                }
                else if (propertyKey.equalsIgnoreCase(MessageProperty.CONNECTION_MODULE_ID))
                {
                    iotHubTransportMessage.setConnectionModuleId(entry.getValue().toString());
                }
                else if (!MessageProperty.RESERVED_PROPERTY_NAMES.contains(propertyKey))
                {
                    iotHubTransportMessage.setProperty(entry.getKey(), entry.getValue().toString());
                }
            }
        }

        return iotHubTransportMessage;
    }

    void close()
    {
        if (this.receiverLink.getLocalState() != EndpointState.CLOSED)
        {
            log.debug("Closing {} receiver link with address {} and link correlation id {}", getLinkInstanceType(), this.receiverLinkAddress, this.linkCorrelationId);
            this.receiverLink.close();
            clearHandlers();
        }
    }

    // Removes any children of this handler (such as LoggingFlowController) and disassociates this handler
    // from the proton reactor. By removing the reference of the proton reactor to this handler, this handler becomes
    // eligible for garbage collection by the JVM. This is important for multiplexed connections where links come and go
    // but the reactor stays alive for a long time.
    private void clearHandlers()
    {
        this.receiverLink.attachments().clear();
        Iterator<Handler> childrenIterator = this.children();
        while (childrenIterator.hasNext())
        {
            childrenIterator.next();
            childrenIterator.remove();
        }
    }
}
