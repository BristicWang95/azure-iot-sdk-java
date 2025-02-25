/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.sdk.iot.device.edge;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Json parser for a method request. Used to invoke methods on other devices/modules
 */
// This suppression below is addressing warnings of fields used for serialization.
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class MethodRequest
{
    private static final String METHOD_NAME_KEY_NAME = "methodName";
    @Expose(deserialize = false)
    @SerializedName(METHOD_NAME_KEY_NAME)
    private String methodName;

    private static final String RESPONSE_TIMEOUT_KEY_NAME = "responseTimeoutInSeconds";
    @Expose(deserialize = false)
    @SerializedName(RESPONSE_TIMEOUT_KEY_NAME)
    private Integer responseTimeoutInSeconds; //Integer, not int because Integer is nullable, so json won't include this field if it is left as the default value

    private static final String CONNECT_TIMEOUT_KEY_NAME = "connectTimeoutInSeconds";
    @Expose(deserialize = false)
    @SerializedName(CONNECT_TIMEOUT_KEY_NAME)
    private Integer connectionTimeoutInSeconds; //Integer, not int because Integer is nullable, so json won't include this field if it is left as the default value

    private static final String PAYLOAD_KEY_NAME = "payload";
    @Expose(deserialize = false)
    @SerializedName(PAYLOAD_KEY_NAME)
    private String payload;

    /**
     * Constructor for a MethodRequest. Uses default responseTimeout and connectionTimeout which is to never timeout
     * @param methodName The method to be invoked
     * @param payload the payload attached to that method
     * @throws IllegalArgumentException if the provided methodName is null or empty
     */
    public MethodRequest(String methodName, String payload) throws IllegalArgumentException
    {
        this(methodName, payload, null, null);
    }

    /**
     * Constructor for a MethodRequest.
     * @param methodName The method to be invoked
     * @param payload the payload attached to that method
     * @param responseTimeout the timeout in seconds for the response to be received
     * @param connectionTimeout the timeout in seconds for the connection to be established
     * @throws IllegalArgumentException if the provided methodName is null or empty
     */
    public MethodRequest(String methodName, String payload, Integer responseTimeout, Integer connectionTimeout) throws IllegalArgumentException
    {
        if (methodName == null || methodName.isEmpty())
        {
            throw new IllegalArgumentException("MethodName cannot be null or empty");
        }

        this.payload = payload;
        this.methodName = methodName;
        this.responseTimeoutInSeconds = responseTimeout;
        this.connectionTimeoutInSeconds = connectionTimeout;
    }

    /**
     * Return the json representation of this object
     * @return the json representation of this object
     */
    public String toJson()
    {
        return new GsonBuilder().create().toJson(this);
    }

    //empty constructor for gson
    MethodRequest() { }
}
