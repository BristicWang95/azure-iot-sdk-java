/**
 * Code generated by Microsoft (R) AutoRest Code Generator.
 * Changes may cause incorrect behavior and will be lost if the code is
 * regenerated.
 */

package com.microsoft.azure.sdk.iot.service.digitaltwin.generated.implementation;

import retrofit2.Retrofit;
import com.microsoft.azure.sdk.iot.service.digitaltwin.generated.DigitalTwins;
import com.google.common.reflect.TypeToken;
import com.microsoft.azure.sdk.iot.service.digitaltwin.generated.models.DigitalTwinGetHeaders;
import com.microsoft.azure.sdk.iot.service.digitaltwin.generated.models.DigitalTwinInvokeComponentCommandHeaders;
import com.microsoft.azure.sdk.iot.service.digitaltwin.generated.models.DigitalTwinInvokeRootLevelCommandHeaders;
import com.microsoft.azure.sdk.iot.service.digitaltwin.generated.models.DigitalTwinUpdateHeaders;
import com.microsoft.rest.RestException;
import com.microsoft.rest.ServiceCallback;
import com.microsoft.rest.ServiceFuture;
import com.microsoft.rest.ServiceResponseWithHeaders;
import com.microsoft.rest.Validator;
import java.io.IOException;
import java.util.List;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.Response;
import rx.functions.Func1;
import rx.Observable;

/**
 * An instance of this class provides access to all the operations defined
 * in DigitalTwins.
 */
public class DigitalTwinsImpl implements DigitalTwins {
    /** The Retrofit service to perform REST calls. */
    private DigitalTwinsService service;
    /** The service client containing this operation class. */
    private IotHubGatewayServiceAPIsImpl client;

    /**
     * Initializes an instance of DigitalTwins.
     *
     * @param retrofit the Retrofit instance built from a Retrofit Builder.
     * @param client the instance of the service client containing this operation class.
     */
    public DigitalTwinsImpl(Retrofit retrofit, IotHubGatewayServiceAPIsImpl client) {
        this.service = retrofit.create(DigitalTwinsService.class);
        this.client = client;
    }

    /**
     * The interface defining all the services for DigitalTwins to be
     * used by Retrofit to perform actually REST calls.
     */
    interface DigitalTwinsService {
        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.sdk.iot.service.digitaltwin.generated.DigitalTwins getDigitalTwin" })
        @GET("digitaltwins/{id}")
        Observable<Response<ResponseBody>> getDigitalTwin(@Path("id") String id, @Query("api-version") String apiVersion);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.sdk.iot.service.digitaltwin.generated.DigitalTwins updateDigitalTwin" })
        @PATCH("digitaltwins/{id}")
        Observable<Response<ResponseBody>> updateDigitalTwin(@Path("id") String id, @Body List<Object> digitalTwinPatch, @Header("If-Match") String ifMatch, @Query("api-version") String apiVersion);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.sdk.iot.service.digitaltwin.generated.DigitalTwins invokeRootLevelCommand" })
        @POST("digitaltwins/{id}/commands/{commandName}")
        Observable<Response<ResponseBody>> invokeRootLevelCommand(@Path("id") String id, @Path("commandName") String commandName, @Body Object payload, @Query("api-version") String apiVersion, @Query("connectTimeoutInSeconds") Integer connectTimeoutInSeconds, @Query("responseTimeoutInSeconds") Integer responseTimeoutInSeconds);

        @Headers({ "Content-Type: application/json; charset=utf-8", "x-ms-logging-context: com.microsoft.azure.sdk.iot.service.digitaltwin.generated.DigitalTwins invokeComponentCommand" })
        @POST("digitaltwins/{id}/components/{componentPath}/commands/{commandName}")
        Observable<Response<ResponseBody>> invokeComponentCommand(@Path("id") String id, @Path("componentPath") String componentPath, @Path("commandName") String commandName, @Body Object payload, @Query("api-version") String apiVersion, @Query("connectTimeoutInSeconds") Integer connectTimeoutInSeconds, @Query("responseTimeoutInSeconds") Integer responseTimeoutInSeconds);

    }

    /**
     * Gets a digital twin.
     *
     * @param id Digital Twin ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws RestException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the Object object if successful.
     */
    public Object getDigitalTwin(String id) {
        return getDigitalTwinWithServiceResponseAsync(id).toBlocking().single().body();
    }

    /**
     * Gets a digital twin.
     *
     * @param id Digital Twin ID.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Object> getDigitalTwinAsync(String id, final ServiceCallback<Object> serviceCallback) {
        return ServiceFuture.fromHeaderResponse(getDigitalTwinWithServiceResponseAsync(id), serviceCallback);
    }

    /**
     * Gets a digital twin.
     *
     * @param id Digital Twin ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<Object> getDigitalTwinAsync(String id) {
        return getDigitalTwinWithServiceResponseAsync(id).map(new Func1<ServiceResponseWithHeaders<Object, DigitalTwinGetHeaders>, Object>() {
            @Override
            public Object call(ServiceResponseWithHeaders<Object, DigitalTwinGetHeaders> response) {
                return response.body();
            }
        });
    }

    /**
     * Gets a digital twin.
     *
     * @param id Digital Twin ID.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<ServiceResponseWithHeaders<Object, DigitalTwinGetHeaders>> getDigitalTwinWithServiceResponseAsync(String id) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter id is required and cannot be null.");
        }
        final String apiVersion = "2020-09-30";
        return service.getDigitalTwin(id, apiVersion)
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponseWithHeaders<Object, DigitalTwinGetHeaders>>>() {
                @Override
                public Observable<ServiceResponseWithHeaders<Object, DigitalTwinGetHeaders>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponseWithHeaders<Object, DigitalTwinGetHeaders> clientResponse = getDigitalTwinDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponseWithHeaders<Object, DigitalTwinGetHeaders> getDigitalTwinDelegate(Response<ResponseBody> response) throws RestException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<Object, RestException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<Object>() { }.getType())
                .buildWithHeaders(response, DigitalTwinGetHeaders.class);
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws RestException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    public void updateDigitalTwin(String id, List<Object> digitalTwinPatch) {
        updateDigitalTwinWithServiceResponseAsync(id, digitalTwinPatch).toBlocking().single().body();
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Void> updateDigitalTwinAsync(String id, List<Object> digitalTwinPatch, final ServiceCallback<Void> serviceCallback) {
        return ServiceFuture.fromHeaderResponse(updateDigitalTwinWithServiceResponseAsync(id, digitalTwinPatch), serviceCallback);
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponseWithHeaders} object if successful.
     */
    public Observable<Void> updateDigitalTwinAsync(String id, List<Object> digitalTwinPatch) {
        return updateDigitalTwinWithServiceResponseAsync(id, digitalTwinPatch).map(new Func1<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>, Void>() {
            @Override
            public Void call(ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponseWithHeaders} object if successful.
     */
    public Observable<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>> updateDigitalTwinWithServiceResponseAsync(String id, List<Object> digitalTwinPatch) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter id is required and cannot be null.");
        }
        if (digitalTwinPatch == null) {
            throw new IllegalArgumentException("Parameter digitalTwinPatch is required and cannot be null.");
        }
        Validator.validate(digitalTwinPatch);
        final String apiVersion = "2020-09-30";
        final String ifMatch = null;
        return service.updateDigitalTwin(id, digitalTwinPatch, ifMatch, apiVersion)
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>>>() {
                @Override
                public Observable<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders> clientResponse = updateDigitalTwinDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @param ifMatch the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws RestException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     */
    public void updateDigitalTwin(String id, List<Object> digitalTwinPatch, String ifMatch) {
        updateDigitalTwinWithServiceResponseAsync(id, digitalTwinPatch, ifMatch).toBlocking().single().body();
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @param ifMatch the String value
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Void> updateDigitalTwinAsync(String id, List<Object> digitalTwinPatch, String ifMatch, final ServiceCallback<Void> serviceCallback) {
        return ServiceFuture.fromHeaderResponse(updateDigitalTwinWithServiceResponseAsync(id, digitalTwinPatch, ifMatch), serviceCallback);
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @param ifMatch the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponseWithHeaders} object if successful.
     */
    public Observable<Void> updateDigitalTwinAsync(String id, List<Object> digitalTwinPatch, String ifMatch) {
        return updateDigitalTwinWithServiceResponseAsync(id, digitalTwinPatch, ifMatch).map(new Func1<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>, Void>() {
            @Override
            public Void call(ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders> response) {
                return response.body();
            }
        });
    }

    /**
     * Updates a digital twin.
     *
     * @param id Digital Twin ID.
     * @param digitalTwinPatch json-patch contents to update.
     * @param ifMatch the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceResponseWithHeaders} object if successful.
     */
    public Observable<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>> updateDigitalTwinWithServiceResponseAsync(String id, List<Object> digitalTwinPatch, String ifMatch) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter id is required and cannot be null.");
        }
        if (digitalTwinPatch == null) {
            throw new IllegalArgumentException("Parameter digitalTwinPatch is required and cannot be null.");
        }
        Validator.validate(digitalTwinPatch);
        final String apiVersion = "2020-09-30";
        return service.updateDigitalTwin(id, digitalTwinPatch, ifMatch, apiVersion)
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>>>() {
                @Override
                public Observable<ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders> clientResponse = updateDigitalTwinDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponseWithHeaders<Void, DigitalTwinUpdateHeaders> updateDigitalTwinDelegate(Response<ResponseBody> response) throws RestException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<Void, RestException>newInstance(this.client.serializerAdapter())
                .register(202, new TypeToken<Void>() { }.getType())
                .buildWithHeaders(response, DigitalTwinUpdateHeaders.class);
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws RestException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the Object object if successful.
     */
    public Object invokeRootLevelCommand(String id, String commandName) {
        return invokeRootLevelCommandWithServiceResponseAsync(id, commandName).toBlocking().single().body();
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Object> invokeRootLevelCommandAsync(String id, String commandName, final ServiceCallback<Object> serviceCallback) {
        return ServiceFuture.fromHeaderResponse(invokeRootLevelCommandWithServiceResponseAsync(id, commandName), serviceCallback);
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<Object> invokeRootLevelCommandAsync(String id, String commandName) {
        return invokeRootLevelCommandWithServiceResponseAsync(id, commandName).map(new Func1<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>, Object>() {
            @Override
            public Object call(ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders> response) {
                return response.body();
            }
        });
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>> invokeRootLevelCommandWithServiceResponseAsync(String id, String commandName) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter id is required and cannot be null.");
        }
        if (commandName == null) {
            throw new IllegalArgumentException("Parameter commandName is required and cannot be null.");
        }
        final String apiVersion = "2020-09-30";
        final Object payload = null;
        final Integer connectTimeoutInSeconds = null;
        final Integer responseTimeoutInSeconds = null;
        return service.invokeRootLevelCommand(id, commandName, payload, apiVersion, connectTimeoutInSeconds, responseTimeoutInSeconds)
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>>>() {
                @Override
                public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders> clientResponse = invokeRootLevelCommandDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws RestException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the Object object if successful.
     */
    public Object invokeRootLevelCommand(String id, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds) {
        return invokeRootLevelCommandWithServiceResponseAsync(id, commandName, payload, connectTimeoutInSeconds, responseTimeoutInSeconds).toBlocking().single().body();
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Object> invokeRootLevelCommandAsync(String id, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds, final ServiceCallback<Object> serviceCallback) {
        return ServiceFuture.fromHeaderResponse(invokeRootLevelCommandWithServiceResponseAsync(id, commandName, payload, connectTimeoutInSeconds, responseTimeoutInSeconds), serviceCallback);
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<Object> invokeRootLevelCommandAsync(String id, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds) {
        return invokeRootLevelCommandWithServiceResponseAsync(id, commandName, payload, connectTimeoutInSeconds, responseTimeoutInSeconds).map(new Func1<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>, Object>() {
            @Override
            public Object call(ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders> response) {
                return response.body();
            }
        });
    }

    /**
     * Invoke a digital twin root level command.
     * Invoke a digital twin root level command.
     *
     * @param id the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>> invokeRootLevelCommandWithServiceResponseAsync(String id, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter id is required and cannot be null.");
        }
        if (commandName == null) {
            throw new IllegalArgumentException("Parameter commandName is required and cannot be null.");
        }
        final String apiVersion = "2020-09-30";
        return service.invokeRootLevelCommand(id, commandName, payload, apiVersion, connectTimeoutInSeconds, responseTimeoutInSeconds)
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>>>() {
                @Override
                public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders> clientResponse = invokeRootLevelCommandDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponseWithHeaders<Object, DigitalTwinInvokeRootLevelCommandHeaders> invokeRootLevelCommandDelegate(Response<ResponseBody> response) throws RestException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<Object, RestException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<Object>() { }.getType())
                .buildWithHeaders(response, DigitalTwinInvokeRootLevelCommandHeaders.class);
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws RestException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the Object object if successful.
     */
    public Object invokeComponentCommand(String id, String componentPath, String commandName) {
        return invokeComponentCommandWithServiceResponseAsync(id, componentPath, commandName).toBlocking().single().body();
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Object> invokeComponentCommandAsync(String id, String componentPath, String commandName, final ServiceCallback<Object> serviceCallback) {
        return ServiceFuture.fromHeaderResponse(invokeComponentCommandWithServiceResponseAsync(id, componentPath, commandName), serviceCallback);
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<Object> invokeComponentCommandAsync(String id, String componentPath, String commandName) {
        return invokeComponentCommandWithServiceResponseAsync(id, componentPath, commandName).map(new Func1<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>, Object>() {
            @Override
            public Object call(ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders> response) {
                return response.body();
            }
        });
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>> invokeComponentCommandWithServiceResponseAsync(String id, String componentPath, String commandName) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter id is required and cannot be null.");
        }
        if (componentPath == null) {
            throw new IllegalArgumentException("Parameter componentPath is required and cannot be null.");
        }
        if (commandName == null) {
            throw new IllegalArgumentException("Parameter commandName is required and cannot be null.");
        }
        final String apiVersion = "2020-09-30";
        final Object payload = null;
        final Integer connectTimeoutInSeconds = null;
        final Integer responseTimeoutInSeconds = null;
        return service.invokeComponentCommand(id, componentPath, commandName, payload, apiVersion, connectTimeoutInSeconds, responseTimeoutInSeconds)
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>>>() {
                @Override
                public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders> clientResponse = invokeComponentCommandDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @throws RestException thrown if the request is rejected by server
     * @throws RuntimeException all other wrapped checked exceptions if the request fails to be sent
     * @return the Object object if successful.
     */
    public Object invokeComponentCommand(String id, String componentPath, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds) {
        return invokeComponentCommandWithServiceResponseAsync(id, componentPath, commandName, payload, connectTimeoutInSeconds, responseTimeoutInSeconds).toBlocking().single().body();
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param serviceCallback the async ServiceCallback to handle successful and failed responses.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the {@link ServiceFuture} object
     */
    public ServiceFuture<Object> invokeComponentCommandAsync(String id, String componentPath, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds, final ServiceCallback<Object> serviceCallback) {
        return ServiceFuture.fromHeaderResponse(invokeComponentCommandWithServiceResponseAsync(id, componentPath, commandName, payload, connectTimeoutInSeconds, responseTimeoutInSeconds), serviceCallback);
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<Object> invokeComponentCommandAsync(String id, String componentPath, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds) {
        return invokeComponentCommandWithServiceResponseAsync(id, componentPath, commandName, payload, connectTimeoutInSeconds, responseTimeoutInSeconds).map(new Func1<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>, Object>() {
            @Override
            public Object call(ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders> response) {
                return response.body();
            }
        });
    }

    /**
     * Invoke a digital twin command.
     * Invoke a digital twin command.
     *
     * @param id the String value
     * @param componentPath the String value
     * @param commandName the String value
     * @param payload the Object value
     * @param connectTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @param responseTimeoutInSeconds Maximum interval of time, in seconds, that the digital twin command will wait for the answer.
     * @throws IllegalArgumentException thrown if parameters fail the validation
     * @return the observable to the Object object
     */
    public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>> invokeComponentCommandWithServiceResponseAsync(String id, String componentPath, String commandName, Object payload, Integer connectTimeoutInSeconds, Integer responseTimeoutInSeconds) {
        if (id == null) {
            throw new IllegalArgumentException("Parameter id is required and cannot be null.");
        }
        if (componentPath == null) {
            throw new IllegalArgumentException("Parameter componentPath is required and cannot be null.");
        }
        if (commandName == null) {
            throw new IllegalArgumentException("Parameter commandName is required and cannot be null.");
        }
        final String apiVersion = "2020-09-30";
        return service.invokeComponentCommand(id, componentPath, commandName, payload, apiVersion, connectTimeoutInSeconds, responseTimeoutInSeconds)
            .flatMap(new Func1<Response<ResponseBody>, Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>>>() {
                @Override
                public Observable<ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders>> call(Response<ResponseBody> response) {
                    try {
                        ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders> clientResponse = invokeComponentCommandDelegate(response);
                        return Observable.just(clientResponse);
                    } catch (Throwable t) {
                        return Observable.error(t);
                    }
                }
            });
    }

    private ServiceResponseWithHeaders<Object, DigitalTwinInvokeComponentCommandHeaders> invokeComponentCommandDelegate(Response<ResponseBody> response) throws RestException, IOException, IllegalArgumentException {
        return this.client.restClient().responseBuilderFactory().<Object, RestException>newInstance(this.client.serializerAdapter())
                .register(200, new TypeToken<Object>() { }.getType())
                .buildWithHeaders(response, DigitalTwinInvokeComponentCommandHeaders.class);
    }

}
