/**
 * Code generated by Microsoft (R) AutoRest Code Generator.
 * Changes may cause incorrect behavior and will be lost if the code is
 * regenerated.
 */

package com.microsoft.azure.sdk.iot.service.digitaltwin.generated.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Defines headers for UpdateDigitalTwin operation.
 */
public class DigitalTwinUpdateHeaders {
    /**
     * Weak Etag of the modified resource.
     */
    @JsonProperty(value = "ETag")
    private String eTag;

    /**
     * URI of the digital twin.
     */
    @JsonProperty(value = "Location")
    private String location;

    /**
     * Get weak Etag of the modified resource.
     *
     * @return the eTag value
     */
    public String eTag() {
        return this.eTag;
    }

    /**
     * Set weak Etag of the modified resource.
     *
     * @param eTag the eTag value to set
     * @return the DigitalTwinUpdateHeaders object itself.
     */
    public DigitalTwinUpdateHeaders withETag(String eTag) {
        this.eTag = eTag;
        return this;
    }

    /**
     * Get uRI of the digital twin.
     *
     * @return the location value
     */
    public String location() {
        return this.location;
    }

    /**
     * Set uRI of the digital twin.
     *
     * @param location the location value to set
     * @return the DigitalTwinUpdateHeaders object itself.
     */
    public DigitalTwinUpdateHeaders withLocation(String location) {
        this.location = location;
        return this;
    }

}
