// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.sdk.iot.service.query;

import com.fasterxml.jackson.databind.util.ArrayIterator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.microsoft.azure.sdk.iot.service.devicetwin.Twin;
import com.microsoft.azure.sdk.iot.service.exceptions.IotHubException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class TwinsQueryResponse
{
    private final transient Gson gson;

    Iterator<Twin> twins;

    @Setter(AccessLevel.PACKAGE) // value is retrieved from header, not json payload
    @Getter
    String continuationToken = "";

    final QueryClient queryClient;
    final String originalQuery;

    public TwinsQueryResponse(String json, QueryClient queryClient, String originalQuery)
    {
        gson = new GsonBuilder().disableHtmlEscaping().create();

        try
        {
            JsonObject[] twinJsonArray = gson.fromJson(json, JsonObject[].class);
            List<Twin> twinArray = new ArrayList<>();
            for (JsonObject twinJson : twinJsonArray)
            {
                twinArray.add(Twin.fromJson(twinJson.toString()));
            }

            this.twins = twinArray.iterator();
        }
        catch (JsonSyntaxException malformed)
        {
            throw new IllegalArgumentException("Malformed json:" + malformed);
        }

        this.queryClient = queryClient;
        this.originalQuery = originalQuery;
    }

    public boolean hasNext()
    {
        return this.twins.hasNext() || this.continuationToken != null;
    }

    public Twin next() throws IotHubException, IOException
    {
        return next(QueryPageOptions.builder().build());
    }

    public Twin next(QueryPageOptions pageOptions) throws IotHubException, IOException
    {
        try
        {
            return this.twins.next();
        }
        catch (NoSuchElementException ex)
        {
            // previous list of jobs has been exhausted. Get next page of results if there is a next page

            if (this.continuationToken == null)
            {
                return null;
            }

            QueryPageOptions nextPageOptions =
                QueryPageOptions.builder()
                    .continuationToken(this.continuationToken)
                    .pageSize(pageOptions.pageSize)
                    .build();

            TwinsQueryResponse nextPage = this.queryClient.queryTwins(this.originalQuery, nextPageOptions);
            this.twins = nextPage.twins;
            this.continuationToken = nextPage.continuationToken;

            return this.twins.next();
        }
    }
}
