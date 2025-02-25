/*
 *
 *  Copyright (c) Microsoft. All rights reserved.
 *  Licensed under the MIT license. See LICENSE file in the project root for full license information.
 *
 */

package com.microsoft.azure.sdk.iot.device.transport;

import com.microsoft.azure.sdk.iot.device.exceptions.TransportException;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;

/**
 * Represents a retry policy that performs exponential backoff with jitter retries.
 */
@Slf4j
public class ExponentialBackoffWithJitter implements RetryPolicy
{
    private int retryCount = Integer.MAX_VALUE;
    private long minBackoff = 100;
    private long maxBackoff = 10*1000; //10 seconds
    private long deltaBackoff = 100;
    private boolean firstFastRetry = true;

    private final SecureRandom random = new SecureRandom();

    /**
     * Constructor with default backoff values and firstFastRetry
     */
    public ExponentialBackoffWithJitter()
    {
        // Let us know when we've created a new exponential backoff
        this.logCreation();
    }

    /**
     * Constructor.
     *
     * @param retryCount the max number of retries allowed in the policies.
     * @param minBackoff the min interval between each retry.
     * @param maxBackoff the max interval between each retry.
     * @param deltaBackoff the max delta allowed between retries.
     * @param firstFastRetry indicates whether the first retry should be immediate.
     */
    public ExponentialBackoffWithJitter(int retryCount, long minBackoff, long maxBackoff, long deltaBackoff, boolean firstFastRetry)
    {
        if (retryCount <= 0)
        {
            throw new IllegalArgumentException("retryCount cannot be less than or equal to 0.");
        }

        this.retryCount = retryCount;
        this.minBackoff = minBackoff;
        this.maxBackoff = maxBackoff;
        this.deltaBackoff = deltaBackoff;
        this.firstFastRetry = firstFastRetry;

        // Let us know when we've created a new exponential backoff
        this.logCreation();
    }

    /**
     * Determines whether the operation should be retried and the interval until the next retry.
     *
     * @param currentRetryCount the number of retries for the given operation
     * @return the retry decision.
     */
    public RetryDecision getRetryDecision(int currentRetryCount, TransportException lastException)
    {
        if (currentRetryCount == 0 && this.firstFastRetry) {
            return new RetryDecision(true, 0);
        }

        // F(x) = min(Cmin+ (2^(x-1)-1) * rand(C * (1 – Jd), C*(1-Ju)), Cmax) where  x is the xth retry.]
        if (currentRetryCount < this.retryCount)
        {
            int deltaBackoffLowbound = (int)(this.deltaBackoff * 0.8);
            int deltaBackoffUpperbound = (int)(this.deltaBackoff * 1.2);
            long randomDeltaBackOff = random.nextInt(deltaBackoffUpperbound - deltaBackoffLowbound);
            long exponentialBackOffWithJitter = (int)((Math.pow(2.0, currentRetryCount) - 1.0) * (randomDeltaBackOff + deltaBackoffLowbound));
            long finalWaitTimeUntilNextRetry = (int)Math.min(this.minBackoff + (double)exponentialBackOffWithJitter, this.maxBackoff);
            return new RetryDecision(true, finalWaitTimeUntilNextRetry);
        }

        return new RetryDecision(false, 0);
    }

    private void logCreation()
    {
        log.info("NOTE: A new instance of ExponentialBackoffWithJitter has been created with the following properties. Retry Count: {}, Min Backoff Interval: {}, Max Backoff Interval: {}, Max Time Between Retries: {}, Fast Retry Enabled: {}", this.retryCount, this.minBackoff, this.maxBackoff, this.deltaBackoff, this.firstFastRetry);
    }
}
