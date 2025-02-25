// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package samples.com.microsoft.azure.sdk.iot.device;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.microsoft.azure.sdk.iot.device.*;
import com.microsoft.azure.sdk.iot.device.twin.*;
import com.microsoft.azure.sdk.iot.provisioning.device.*;
import com.microsoft.azure.sdk.iot.provisioning.device.internal.exceptions.ProvisioningDeviceClientException;
import com.microsoft.azure.sdk.iot.provisioning.security.SecurityProviderSymmetricKey;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.microsoft.azure.sdk.iot.device.IotHubStatusCode.OK;

@Slf4j
public class Thermostat {

    public enum StatusCode {
        COMPLETED (200),
        IN_PROGRESS (202),
        NOT_FOUND (404);

        private final int value;
        StatusCode(int value) {
            this.value = value;
        }
    }

    private static final ReportedPropertiesCallback sendReportedPropertiesResponseCallback = (statusCode, e, callbackContext) ->
    {
        if (statusCode == OK)
        {
            System.out.println("Reported properties updated successfully");
        }
        else
        {
            System.out.println("Reported properties failed to be updated. Status code: " + statusCode);
            e.printStackTrace();
        }
    };

    // DTDL interface used: https://github.com/Azure/iot-plugandplay-models/blob/main/dtmi/com/example/thermostat-1.json
    private static final String deviceConnectionString = System.getenv("IOTHUB_DEVICE_CONNECTION_STRING");
    private static final String deviceSecurityType = System.getenv("IOTHUB_DEVICE_SECURITY_TYPE");
    private static final String MODEL_ID = "dtmi:com:example:Thermostat;1";

    // Environmental variables for Dps
    private static final String scopeId = System.getenv("IOTHUB_DEVICE_DPS_ID_SCOPE");
    private static final String globalEndpoint = System.getenv("IOTHUB_DEVICE_DPS_ENDPOINT");
    private static final String deviceSymmetricKey = System.getenv("IOTHUB_DEVICE_DPS_DEVICE_KEY");
    private static final String registrationId = System.getenv("IOTHUB_DEVICE_DPS_DEVICE_ID");

    private static final ProvisioningDeviceClientTransportProtocol provisioningProtocol = ProvisioningDeviceClientTransportProtocol.MQTT;
    private static final int MAX_TIME_TO_WAIT_FOR_REGISTRATION = 1000; // in milli seconds

    // Plug and play features are available over MQTT, MQTT_WS, AMQPS, and AMQPS_WS.
    private static final IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

    private static final Random random = new Random();
    private static final Gson gson = new Gson();

    // HashMap to hold the temperature updates sent over.
    // NOTE: Memory constrained device should leverage storage capabilities of an external service to store this information and perform computation.
    // See https://docs.microsoft.com/en-us/azure/event-grid/compare-messaging-services for more details.
    private static final Map<Date, Double> temperatureReadings = new HashMap<>();

    private static DeviceClient deviceClient;
    private static double temperature = 0.0d;
    private static double maxTemperature = 0.0d;
    private static boolean temperatureReset = true;

    static class ProvisioningStatus
    {
        ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationInfoClient = new ProvisioningDeviceClientRegistrationResult();
        Exception exception;
    }

    static class ProvisioningDeviceClientRegistrationCallbackImpl implements ProvisioningDeviceClientRegistrationCallback
    {
        @Override
        public void run(ProvisioningDeviceClientRegistrationResult provisioningDeviceClientRegistrationResult, Exception exception, Object context)
        {
            if (context instanceof ProvisioningStatus)
            {
                ProvisioningStatus status = (ProvisioningStatus) context;
                status.provisioningDeviceClientRegistrationInfoClient = provisioningDeviceClientRegistrationResult;
                status.exception = exception;
            }
            else
            {
                System.out.println("Received unknown context");
            }
        }
    }

    public static void main(String[] args) throws URISyntaxException, IOException, ProvisioningDeviceClientException, InterruptedException {

        // This sample follows the following workflow:
        // -> Initialize device client instance.
        // -> Set handler to receive "targetTemperature" updates, and send the received update over reported property.
        // -> Set handler to receive "getMaxMinReport" command, and send the generated report as command response.
        // -> Periodically send "temperature" over telemetry.
        // -> Send "maxTempSinceLastReboot" over property update, when a new max temperature is set.

        // This environment variable indicates if DPS or IoT Hub connection string will be used to provision the device.
        // Expected values: (case-insensitive)
        // "DPS" - The sample will use DPS to provision the device.
        // "connectionString" - The sample will use IoT Hub connection string to provision the device.

        if ((deviceSecurityType == null) || deviceSecurityType.isEmpty())
        {
            throw new IllegalArgumentException("Device security type needs to be specified, please set the environment variable \"IOTHUB_DEVICE_SECURITY_TYPE\"");
        }

        log.debug("Initialize the device client.");

        switch (deviceSecurityType.toLowerCase())
        {
            case "dps":
            {
                if (validateArgsForDpsFlow())
                {
                    initializeAndProvisionDevice();
                    break;
                }
                throw new IllegalArgumentException("Required environment variables are not set for DPS flow, please recheck your environment.");
            }
            case "connectionstring":
            {
                if (validateArgsForIotHubFlow())
                {
                    initializeDeviceClient();
                    break;
                }
                throw new IllegalArgumentException("Required environment variables are not set for IoT Hub flow, please recheck your environment.");
            }
            default:
            {
                throw new IllegalArgumentException("Unrecognized value for IOTHUB_DEVICE_SECURITY_TYPE received: {s_deviceSecurityType}." +
                        " It should be either \"DPS\" or \"connectionString\" (case-insensitive).");
            }
        }

        log.debug("Start twin and set handler to receive \"targetTemperature\" updates.");


        deviceClient.subscribeToDesiredPropertiesAsync(
            (statusCode, context) ->
            {
                if (statusCode == OK)
                {
                    log.info("Successfully subscribed to desired properties. Getting initial state");
                    deviceClient.getTwinAsync(
                        (twin, getTwinContext) ->
                        {
                            log.info("Initial twin state received");
                            log.info(twin.toString());
                        },
                        null);
                }
                else
                {
                    log.info("Failed to subscribe to desired properties. Error code {}", statusCode);
                    System.exit(-1);
                }
            },
            null,
            (twin, context) ->
            {
                TwinCollection desiredProperties = twin.getDesiredProperties();
                for (String desiredPropertyKey : desiredProperties.keySet())
                {
                    TargetTemperatureUpdateCallback.onPropertyChanged(new Property(desiredPropertyKey, desiredProperties.get(desiredPropertyKey)), null);
                }
            },
            null);

        log.debug("Set handler to receive \"getMaxMinReport\" command.");
        String methodName = "getMaxMinReport";
        deviceClient.subscribeToMethodsAsync(new GetMaxMinReportMethodCallback(), methodName, new MethodIotHubEventCallback(), methodName);

        new Thread(new Runnable() {
            @SneakyThrows({InterruptedException.class, IOException.class})
            @Override
            public void run() {
                while (true) {
                    if (temperatureReset) {
                        // Generate a random value between 5.0°C and 45.0°C for the current temperature reading.
                        temperature = BigDecimal.valueOf(random.nextDouble() * 40 + 5).setScale(1, RoundingMode.HALF_UP).doubleValue();
                        temperatureReset = false;
                    }

                    sendTemperatureReading();
                    Thread.sleep(5 * 1000);
                }
            }
        }).start();
    }

    private static void initializeAndProvisionDevice() throws ProvisioningDeviceClientException, IOException, URISyntaxException, InterruptedException {
        SecurityProviderSymmetricKey securityClientSymmetricKey = new SecurityProviderSymmetricKey(deviceSymmetricKey.getBytes(StandardCharsets.UTF_8), registrationId);
        ProvisioningDeviceClient provisioningDeviceClient;
        ProvisioningStatus provisioningStatus = new ProvisioningStatus();

        provisioningDeviceClient = ProvisioningDeviceClient.create(globalEndpoint, scopeId, provisioningProtocol, securityClientSymmetricKey);

        AdditionalData additionalData = new AdditionalData();
        additionalData.setProvisioningPayload(String.format("{\"modelId\": \"%s\"}", MODEL_ID));

        provisioningDeviceClient.registerDevice(new ProvisioningDeviceClientRegistrationCallbackImpl(), provisioningStatus, additionalData);

        while (provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() != ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED)
        {
            if (provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ERROR ||
                    provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_DISABLED ||
                    provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_FAILED)
            {
                provisioningStatus.exception.printStackTrace();
                System.out.println("Registration error, bailing out");
                break;
            }
            System.out.println("Waiting for Provisioning Service to register");
            Thread.sleep(MAX_TIME_TO_WAIT_FOR_REGISTRATION);
        }

        ClientOptions options = ClientOptions.builder().modelId(MODEL_ID).build();

        if (provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getProvisioningDeviceClientStatus() == ProvisioningDeviceClientStatus.PROVISIONING_DEVICE_STATUS_ASSIGNED) {
            System.out.println("IotHUb Uri : " + provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri());
            System.out.println("Device ID : " + provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId());

            String iotHubUri = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getIothubUri();
            String deviceId = provisioningStatus.provisioningDeviceClientRegistrationInfoClient.getDeviceId();

            log.debug("Opening the device client.");
            deviceClient = new DeviceClient(iotHubUri, deviceId, securityClientSymmetricKey, IotHubClientProtocol.MQTT, options);
            deviceClient.open(false);
        }
    }

    private static boolean validateArgsForIotHubFlow()
    {
        return !(deviceConnectionString == null || deviceConnectionString.isEmpty());
    }

    private static boolean validateArgsForDpsFlow()
    {
        return !((globalEndpoint == null || globalEndpoint.isEmpty())
                && (scopeId == null || scopeId.isEmpty())
                && (registrationId == null || registrationId.isEmpty())
                && (deviceSymmetricKey == null || deviceSymmetricKey.isEmpty()));
    }

    /**
     * Initialize the device client instance over Mqtt protocol, setting the ModelId into ClientOptions.
     * This method also sets a connection status change callback, that will get triggered any time the device's connection status changes.
     */
    private static void initializeDeviceClient() throws URISyntaxException, IOException {
        ClientOptions options = ClientOptions.builder().modelId(MODEL_ID).build();
        deviceClient = new DeviceClient(deviceConnectionString, protocol, options);

        deviceClient.setConnectionStatusChangeCallback((status, statusChangeReason, throwable, callbackContext) -> {
            log.debug("Connection status change registered: status={}, reason={}", status, statusChangeReason);

            if (throwable != null) {
                log.debug("The connection status change was caused by the following Throwable: {}", throwable.getMessage());
                throwable.printStackTrace();
            }
        }, deviceClient);

        deviceClient.open(false);
    }

    /**
     * The desired property update callback, which receives the target temperature as a desired property update,
     * and updates the current temperature value over telemetry and reported property update.
     */
    private static class TargetTemperatureUpdateCallback
    {
        final static String propertyName = "targetTemperature";

        @SneakyThrows(InterruptedException.class)
        public static void onPropertyChanged(Property property, Object context) {
            if (property.getKey().equalsIgnoreCase(propertyName)) {
                double targetTemperature = ((Number)property.getValue()).doubleValue();
                log.debug("Property: Received - {\"{}\": {}°C}.", propertyName, targetTemperature);

                EmbeddedPropertyUpdate pendingUpdate = new EmbeddedPropertyUpdate(targetTemperature, StatusCode.IN_PROGRESS.value, property.getVersion(), null);
                TwinCollection reportedPropertyPending = new TwinCollection();
                reportedPropertyPending.put(propertyName, pendingUpdate);
                deviceClient.updateReportedPropertiesAsync(reportedPropertyPending, sendReportedPropertiesResponseCallback, null);
                log.debug("Property: Update - {\"{}\": {}°C} is {}", propertyName, targetTemperature, StatusCode.IN_PROGRESS);

                // Update temperature in 2 steps
                double step = (targetTemperature - temperature) / 2;
                for (int i = 1; i <=2; i++) {
                    temperature = BigDecimal.valueOf(temperature + step).setScale(1, RoundingMode.HALF_UP).doubleValue();
                    Thread.sleep(5 * 1000);
                }

                EmbeddedPropertyUpdate completedUpdate = new EmbeddedPropertyUpdate(temperature, StatusCode.COMPLETED.value, property.getVersion(), "Successfully updated target temperature");
                TwinCollection reportedProperties = new TwinCollection();
                reportedProperties.put(propertyName, completedUpdate);
                deviceClient.updateReportedPropertiesAsync(reportedProperties, sendReportedPropertiesResponseCallback, null);

                log.debug("Property: Update - {\"{}\": {}°C} is {}", propertyName, temperature, StatusCode.COMPLETED);
            } else {
                log.debug("Property: Received an unrecognized property update from service.");
            }
        }
    }

    @AllArgsConstructor
    private static class EmbeddedPropertyUpdate {
        @NonNull
        @SerializedName("value")
        public Object value;
        @NonNull
        @SerializedName("ac")
        public Integer ackCode;
        @NonNull
        @SerializedName("av")
        public Integer ackVersion;
        @SerializedName("ad")
        public String ackDescription;
    }

    /**
     * The callback to handle "getMaxMinReport" command.
     * This method will returns the max, min and average temperature from the specified time to the current time.
     */
    private static class GetMaxMinReportMethodCallback implements MethodCallback
    {
        final String commandName = "getMaxMinReport";

        @SneakyThrows
        @Override
        public DirectMethodResponse onMethodInvoked(String methodName, Object methodData, Object context) {
            if (methodName.equalsIgnoreCase(commandName)) {

                String jsonRequest = new String((byte[]) methodData, StandardCharsets.UTF_8);
                Date since = getCommandRequestValue(jsonRequest, Date.class);
                log.debug("Command: Received - Generating min, max, avg temperature report since {}.", since);

                double runningTotal = 0;
                Map<Date, Double> filteredReadings = new HashMap<>();
                for (Map.Entry<Date, Double> entry : temperatureReadings.entrySet()) {
                    if (entry.getKey().after(since)) {
                        filteredReadings.put(entry.getKey(), entry.getValue());
                        runningTotal += entry.getValue();
                    }
                }

                if (filteredReadings.size() > 1) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    double maxTemp = Collections.max(filteredReadings.values());
                    double minTemp = Collections.min(filteredReadings.values());
                    double avgTemp = runningTotal / filteredReadings.size();
                    String startTime = sdf.format(Collections.min(filteredReadings.keySet()));
                    String endTime = sdf.format(Collections.max(filteredReadings.keySet()));

                    String responsePayload = String.format(
                            "{\"maxTemp\": %.1f, \"minTemp\": %.1f, \"avgTemp\": %.1f, \"startTime\": \"%s\", \"endTime\": \"%s\"}",
                            maxTemp,
                            minTemp,
                            avgTemp,
                            startTime,
                            endTime);

                    log.debug("Command: MaxMinReport since {}: \"maxTemp\": {}°C, \"minTemp\": {}°C, \"avgTemp\": {}°C, \"startTime\": {}, \"endTime\": {}",
                            since,
                            maxTemp,
                            minTemp,
                            avgTemp,
                            startTime,
                            endTime);

                    return new DirectMethodResponse(StatusCode.COMPLETED.value, responsePayload);
                }

                log.debug("Command: No relevant readings found since {}, cannot generate any report.", since);
                return new DirectMethodResponse(StatusCode.NOT_FOUND.value, null);
            }

            log.error("Command: Unknown command {} invoked from service.", methodName);
            return new DirectMethodResponse(StatusCode.NOT_FOUND.value, null);
        }
    }

    /**
     * The callback to be invoked in response to command invocation from IoT Hub.
     */
    private static class MethodIotHubEventCallback implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            String commandName = (String) callbackContext;
            log.debug("Command - Response from IoT Hub: command name={}, status={}", commandName, responseStatus.name());
        }
    }

    /**
     * The callback to be invoked when a telemetry response is received from IoT Hub.
     */
    private static class MessageIotHubEventCallback implements IotHubEventCallback {

        @Override
        public void execute(IotHubStatusCode responseStatus, Object callbackContext) {
            Message msg = (Message) callbackContext;
            log.debug("Telemetry - Response from IoT Hub: message Id={}, status={}", msg.getMessageId(), responseStatus.name());
        }
    }

    private static void sendTemperatureReading() throws IOException {
        sendTemperatureTelemetry();

        double currentMaxTemp = Collections.max(temperatureReadings.values());
        if (currentMaxTemp > maxTemperature) {
            maxTemperature = currentMaxTemp;
            updateMaxTemperatureSinceLastReboot();
        }
    }

    private static void sendTemperatureTelemetry() {
        String telemetryName = "temperature";
        String telemetryPayload = String.format("{\"%s\": %f}", telemetryName, temperature);

        Message message = new Message(telemetryPayload);
        message.setContentEncoding(StandardCharsets.UTF_8.name());
        message.setContentType("application/json");

        deviceClient.sendEventAsync(message, new MessageIotHubEventCallback(), message);
        log.debug("Telemetry: Sent - {\"{}\": {}°C} with message Id {}.", telemetryName, temperature, message.getMessageId());
        temperatureReadings.put(new Date(), temperature);
    }

    private static void updateMaxTemperatureSinceLastReboot() throws IOException {
        String propertyName = "maxTempSinceLastReboot";
        TwinCollection reportedProperties = new TwinCollection();
        reportedProperties.put(propertyName, maxTemperature);
        deviceClient.updateReportedPropertiesAsync(reportedProperties, sendReportedPropertiesResponseCallback, null);
        log.debug("Property: Update - {\"{}\": {}°C} is {}.", propertyName, maxTemperature, StatusCode.COMPLETED);
    }

    @SuppressWarnings("SameParameterValue") // For this sample, type is always Date.class, but it can be other service-allowed values as well.
    private static <T> T getCommandRequestValue(@NonNull String jsonPayload, @NonNull Class<T> type) {
        return gson.fromJson(jsonPayload, type);
    }
}
