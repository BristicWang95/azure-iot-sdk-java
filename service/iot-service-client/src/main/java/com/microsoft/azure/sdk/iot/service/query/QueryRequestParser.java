/*
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 */

package com.microsoft.azure.sdk.iot.service.query;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.azure.sdk.iot.service.ParserUtility;

@SuppressWarnings("unused") // A number of private members are unused but may be filled in or used by serialization
class QueryRequestParser
{
    private static final String QUERY_TAG = "query";
    // This suppression below is addressing warnings of field used for serialization.
    @SuppressWarnings("FieldCanBeLocal")
    @Expose(deserialize = false)
    @SerializedName(QUERY_TAG)
    private String query = null;

    /**
     * CONSTRUCTOR
     * Create an instance of the QueryRequestParser based on the provided query.
     *
     * @param query is the name of the blob (file name in the blob)
     * @throws IllegalArgumentException if the query is null, empty, or not valid.
     */
    QueryRequestParser(String query) throws IllegalArgumentException
    {
        ParserUtility.validateQuery(query);
        this.query = query;
    }

    /**
     * Convert this class in a valid json.
     *
     * @return a valid json that represents the content of this class.
     */
    String toJson()
    {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();

        //Codes_SRS_QUERY_REQUEST_PARSER_25_004: [The toJson shall return a string with a json that represents the contents of the QueryRequestParser.]
        return gson.toJson(this);
    }

    /**
     * Empty constructor: Used only to keep GSON happy.
     */
    @SuppressWarnings("unused")
    QueryRequestParser()
    {
    }
}
