// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package com.microsoft.azure.sdk.iot.device.transport.https;

import com.microsoft.azure.sdk.iot.device.ProxySettings;
import com.microsoft.azure.sdk.iot.device.exceptions.TransportException;
import com.microsoft.azure.sdk.iot.device.transport.HttpProxySocketFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * A wrapper for the Java SE class {@link HttpsURLConnection}. Used to avoid
 * compatibility issues when testing with the mocking framework JMockit, as well
 * as to avoid some undocumented side effects when using HttpsURLConnection.
 * </p>
 * <p>
 * The underlying {@link HttpsURLConnection} is transparently managed by Java. To reuse
 * connections, for each time {@link #connect()} is called, the input streams (input
 * stream or error stream, if input stream is not accessible) must be completely
 * read. Otherwise, the data remains in the stream and the connection will not
 * be reusable.
 * </p>
 */
public class HttpsConnection
{
    /** The underlying HTTP/HTTPS connection. */
    private final HttpURLConnection connection;

    private ProxySettings proxySettings;

    /**
     * The body. {@link HttpURLConnection} silently calls connect() when the output
     * stream is written to. We buffer the body and defer writing to the output
     * stream until {@link #connect()} is called.
     */
    private byte[] body;

    /**
     * Constructor. Opens a connection to the given URL. Can be HTTPS or HTTP
     *
     * @param url the URL for the HTTP/HTTPS connection.
     * @param method the HTTP method (i.e. GET).
     * @throws TransportException if the connection could not be opened.
     */
    public HttpsConnection(URL url, HttpsMethod method) throws TransportException
    {
        this(url, method, null);
    }

    /**
     * Constructor. Opens a connection to the given URL. Can be HTTPS or HTTP
     *
     * @param url the URL for the HTTP/HTTPS connection.
     * @param method the HTTP method (i.e. GET).
     * @param proxySettings The proxy settings to use when connecting. If null, then no proxy will be used
     *
     * @throws TransportException if the connection could not be opened.
     */
    public HttpsConnection(URL url, HttpsMethod method, final ProxySettings proxySettings) throws TransportException
    {
        this(url, method, proxySettings, true);
    }
    /**
     * Constructor. Opens a connection to the given URL. Can be HTTPS or HTTP
     *
     * @param url the URL for the HTTP/HTTPS connection.
     * @param method the HTTP method (i.e. GET).
     * @param proxySettings The proxy settings to use when connecting. If null, then no proxy will be used
     * @param isHttps if true, then this request is an https request as opposed to an http request
     * @throws TransportException if the connection could not be opened.
     */
    public HttpsConnection(URL url, HttpsMethod method, final ProxySettings proxySettings, boolean isHttps) throws TransportException
    {
        final String protocol = url.getProtocol();
        if (isHttps && !protocol.equalsIgnoreCase("HTTPS"))
        {
            String errMsg = String.format("Expected URL that uses protocol "
                            + "HTTPS but received one that uses "
                            + "protocol '%s'.%n",
                    protocol);
            throw new IllegalArgumentException(errMsg);
        }


        if (!isHttps && !protocol.equalsIgnoreCase("HTTP"))
        {
            String errMsg = String.format("Expected URL that uses protocol "
                            + "HTTP but received one that uses "
                            + "protocol '%s'.%n",
                    protocol);
            throw new IllegalArgumentException(errMsg);
        }

        String host = url.getHost();
        if (!isHttps && !host.equalsIgnoreCase("localhost"))
        {
            // Currently, IoT Edge runtime can dictate that this client library sends an HTTP request, but it will
            // only ever be to localhost. The Edge team is currently working to remove this requirement. Eventually,
            // the edge runtime will just make this client use unix domain sockets instead.
            // Once that happens, we can remove support for HTTP requests entirely.
            // https://msazure.visualstudio.com/One/_workitems/edit/8863048
            throw new IllegalArgumentException("Cannot do HTTP requests to any host other than localhost");
        }

        this.body = new byte[0];

        try
        {
            this.connection = (HttpURLConnection) url.openConnection();
            this.connection.setRequestMethod(method.name());
        }
        catch (IOException e)
        {
            throw HttpsConnection.buildTransportException(e);
        }

        this.proxySettings = proxySettings;
    }

    /**
     * Sends the request to the URL given in the constructor.
     *
     * @throws TransportException if the connection could not be established, or the
     * server responded with a bad status code.
     */
    public void connect() throws TransportException
    {
        try
        {
            if (this.body.length > 0)
            {
                this.connection.setDoOutput(true);
                this.connection.getOutputStream().write(this.body);
            }

            this.connection.connect();
        }
        catch (IOException e)
        {
            throw HttpsConnection.buildTransportException(e);
        }
    }

    /**
     * Sets the request method (i.e. POST).
     *
     * @param method the request method.
     *
     * @throws TransportException if the request currently has a non-empty
     * body and the new method is not a POST or a PUT. This is because Java's
     * {@link HttpsURLConnection} silently converts the HTTPS method to POST or PUT if a
     * body is written to the request.
     */
    public void setRequestMethod(HttpsMethod method) throws TransportException
    {
        if (method != HttpsMethod.POST && method != HttpsMethod.PUT)
        {
            if (this.body.length > 0)
            {
                throw new IllegalArgumentException(
                        "Cannot change the request method from POST "
                                + "or PUT when the request body is non-empty.");
            }
        }

        try
        {
            this.connection.setRequestMethod(method.name());
        }
        catch (ProtocolException | SecurityException e)
        {
            // should never happen, since the method names are hard-coded.
            throw new TransportException(e);
        }
    }

    /**
     * Sets the request header field to the given value.
     *
     * @param field the header field name.
     * @param value the header field value.
     */
    public void setRequestHeader(String field, String value)
    {
        this.connection.setRequestProperty(field, value);
    }

    /**
     * Sets the read timeout in milliseconds. The read timeout is the number of
     * milliseconds after the server receives a request and before the server
     * sends data back.
     *
     * @param timeout the read timeout.
     */
    public void setReadTimeout(int timeout)
    {
        this.connection.setReadTimeout(timeout);
    }

    /**
     * Sets the connect timeout in milliseconds.
     * @param timeout the connect timeout in milliseconds.
     */
    public void setConnectTimeout(int timeout)
    {
        this.connection.setConnectTimeout(timeout);
    }

    /**
     * Saves the body to be sent with the request.
     *
     * @param body the request body.
     *
     */
    public void writeOutput(byte[] body)
    {
        HttpsMethod method = HttpsMethod.valueOf(
                this.connection.getRequestMethod());
        if (method != HttpsMethod.POST && method != HttpsMethod.PUT)
        {
            if (body.length > 0)
            {
                throw new IllegalArgumentException(
                        "Cannot write a body to a request that "
                                + "is not a POST or a PUT request.");
            }
        }
        else
        {
            this.body = Arrays.copyOf(body, body.length);
        }
    }

    /**
     * Reads from the input stream (response stream) and returns the response.
     *
     * @return the response body.
     *
     * @throws TransportException if the input stream could not be accessed, for
     * example if the server could not be reached.
     */
    public byte[] readInput() throws TransportException
    {
        try
        {
            byte[] input;
            try (InputStream inputStream = this.connection.getInputStream())
            {
                input = readInputStream(inputStream);
            }

            return input;
        }
        catch (IOException e)
        {
            throw HttpsConnection.buildTransportException(e);
        }
    }

    /**
     * Reads from the error stream and returns the error reason.
     *
     * @return the error reason.
     *
     * @throws TransportException if the input stream could not be accessed, for
     * example if the server could not be reached.
     */
    public byte[] readError() throws TransportException
    {
        try
        {
            byte[] error;
            try (InputStream errorStream = this.connection.getErrorStream())
            {
                error = new byte[0];
                // if there is no error reason, getErrorStream() returns null.
                if (errorStream != null)
                {
                    error = readInputStream(errorStream);
                }
            }

            return error;
        }
        catch (IOException e)
        {
            throw HttpsConnection.buildTransportException(e);
        }
    }

    /**
     * Returns the response status code.
     *
     * @return the response status code.
     *
     * @throws TransportException if no response was received.
     */
    public int getResponseStatus() throws TransportException
    {
        try
        {
            return this.connection.getResponseCode();
        }
        catch (IOException e)
        {
            throw HttpsConnection.buildTransportException(e);
        }
    }

    /**
     * Returns the response headers as a {@link Map}, where the key is the
     * header field name and the values are the values associated with the
     * header field name.
     *
     * @return the response headers.
     *
     */
    public Map<String, List<String>> getResponseHeaders()
    {
        return this.connection.getHeaderFields();
    }

    /**
     * Reads the input stream until the stream is empty.
     *
     * @param stream the input stream.
     *
     * @return the content of the input stream.
     *
     * @throws TransportException if the input stream could not be read from.
     */
    private static byte[] readInputStream(InputStream stream) throws TransportException
    {
        try
        {
            ArrayList<Byte> byteBuffer = new ArrayList<>();
            int nextByte;
            // read(byte[]) reads the byte into the buffer and returns the number
            // of bytes read, or -1 if the end of the stream has been reached.
            while ((nextByte = stream.read()) > -1)
            {
                byteBuffer.add((byte) nextByte);
            }

            int bufferSize = byteBuffer.size();
            byte[] byteArray = new byte[bufferSize];
            for (int i = 0; i < bufferSize; ++i)
            {
                byteArray[i] = byteBuffer.get(i);
            }

            return byteArray;
        }
        catch (IOException e)
        {
            throw HttpsConnection.buildTransportException(e);
        }
    }

    void setSSLContext(SSLContext sslContext) throws IllegalArgumentException
    {
        if (sslContext == null)
        {
            throw new IllegalArgumentException("SSL context cannot be null");
        }
        if (this.connection instanceof HttpsURLConnection)
        {
            if (this.proxySettings != null)
            {
                ((HttpsURLConnection)this.connection).setSSLSocketFactory(new HttpProxySocketFactory(sslContext.getSocketFactory(), proxySettings));
            }
            else
            {
                ((HttpsURLConnection)this.connection).setSSLSocketFactory(sslContext.getSocketFactory());
            }
        }
        else
        {
            throw new UnsupportedOperationException("HTTP connections do not support using ssl socket factory");
        }
    }

    @SuppressWarnings("unused")
    protected HttpsConnection()
    {
        this.connection = null;
    }

    private static TransportException buildTransportException(IOException e)
    {
        TransportException transportException = new TransportException(e);

        if (e instanceof NoRouteToHostException || e instanceof UnknownHostException)
        {
            transportException.setRetryable(true);
        }

        return transportException;
    }

    /**
     * Get the body being used in the http connection
     * @return the body being used in the http connection
     */
    @SuppressWarnings("unused") // This is the only way to get the body from this class, will be needed if extended
    byte[] getBody()
    {
        return this.body;
    }
}
