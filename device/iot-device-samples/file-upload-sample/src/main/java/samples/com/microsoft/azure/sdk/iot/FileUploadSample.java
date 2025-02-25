// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package samples.com.microsoft.azure.sdk.iot;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobClientBuilder;
import com.microsoft.azure.sdk.iot.device.FileUploadCompletionNotification;
import com.microsoft.azure.sdk.iot.device.FileUploadSasUriRequest;
import com.microsoft.azure.sdk.iot.device.FileUploadSasUriResponse;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * File Upload Sample for an IoT Hub. This is a completed sample
 * that upload files and directories with subdirectories. It is
 * useful to test uploads in parallel.
 */
public class FileUploadSample
{
    private static final List<String> fileNameList = new ArrayList<>();

    /**
     * Upload file or directories to blobs using IoT Hub.
     *
     * @param args 
     * args[0] = IoT Hub connection string
     * args[1] = File or directory to upload
     */
    public static void main(String[] args)
            throws IOException, URISyntaxException
    {
        String connString;
        String fullFileName;

        System.out.println("Starting...");
        System.out.println("Beginning setup.");


        if (args.length == 2)
        {
            connString = args[0];
            fullFileName = args[1];
        }
        else
        {
            System.out.format(
                    "Expected the following argument but received: %d.\n"
                            + "The program should be called with the following args: \n"
                            + "[Device connection string] - String containing Hostname, Device Id & Device Key in the following formats: HostName=<iothub_host_name>;DeviceId=<device_id>;SharedAccessKey=<device_key>\n"
                            + "[File or Directory to upload] - String containing the full path for the file or directory to upload.\n",
                    args.length);
            return;
        }

        // File upload will always use HTTPS, DeviceClient will use this protocol only
        //   for the other services like Device Telemetry, Device Method and Device Twin.
        IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;

        System.out.println("Successfully read input parameters.");
        System.out.format("Using communication protocol %s.\n",
                protocol.name());

        DeviceClient client = new DeviceClient(connString, protocol);

        System.out.println("Successfully created an IoT Hub client.");
        
        try
        {
            uploadFileOrDirectory(client, fullFileName);
        }
        catch (Exception e)
        {
            System.out.println("On exception, shutting down \n" + " Cause: " + e.getCause() + " \nERROR: " +  e.getMessage());
            System.out.println("Shutting down...");
            client.close();
        }

        System.out.println("Press any key to exit...");

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        scanner.nextLine();
        System.out.println("Shutting down...");
        client.close();
    }

    private static void uploadFileOrDirectory(DeviceClient client, String fullFileName) throws IOException, URISyntaxException {
        File file = new File(fullFileName);
        if(file.isDirectory())
        {
            uploadFileOrDirectoryRecursive(client, file.getPath(), "");
        }
        else
        {
            uploadFile(client, file.getParent(), file.getName());
        }
    }

    private static void uploadFileOrDirectoryRecursive(DeviceClient client, String baseDirectory, String relativePath) throws IOException, URISyntaxException {
        String[] fileNameList;

        File file = new File(baseDirectory, relativePath);
        if(file.isDirectory())
        {
            fileNameList = file.list();
            if(fileNameList != null)
            {
                for (String fileNameInDirectory:fileNameList)
                {
                    File newDir = new File(relativePath, fileNameInDirectory);
                    uploadFileOrDirectoryRecursive(client, baseDirectory, newDir.toString());
                }
            }
        }
        else
        {
            uploadFile(client, baseDirectory, relativePath);
        }
    }

    private static void uploadFile(DeviceClient client, String baseDirectory, String relativeFileName) throws IOException
    {
        File file = new File(baseDirectory, relativeFileName);

        try
        {
            if (relativeFileName.startsWith("\\"))
            {
                relativeFileName = relativeFileName.substring(1);
            }

            int index = fileNameList.size();
            fileNameList.add(relativeFileName);

            System.out.println("Getting SAS URI for upload file " + fileNameList.get(index));
            FileUploadSasUriResponse sasUriResponse = client.getFileUploadSasUri(new FileUploadSasUriRequest(file.getName()));

            try
            {
                System.out.println("Uploading file " + fileNameList.get(index) + " with the retrieved SAS URI...");

                BlobClient blobClient =
                    new BlobClientBuilder()
                        .endpoint(sasUriResponse.getBlobUri().toString())
                        .buildClient();

                blobClient.uploadFromFile(file.getPath());
            }
            catch (Exception e)
            {
                // Note that this is done even when the file upload fails. IoT Hub has a fixed number of SAS URIs allowed active
                // at any given time. Once you are done with the file upload, you should free your SAS URI so that other
                // SAS URIs can be generated. If a SAS URI is not freed through this API, then it will free itself eventually
                // based on how long SAS URIs are configured to live on your IoT Hub.
                FileUploadCompletionNotification completionNotification = new FileUploadCompletionNotification(sasUriResponse.getCorrelationId(), false);
                client.completeFileUpload(completionNotification);
                System.out.println("Failed to upload file " + fileNameList.get(index));
                e.printStackTrace();
                return;
            }

            FileUploadCompletionNotification completionNotification = new FileUploadCompletionNotification(sasUriResponse.getCorrelationId(), true);
            client.completeFileUpload(completionNotification);

            System.out.println("Finished file upload for file " + fileNameList.get(index));
        }
        finally
        {
            client.close();
        }
    }
}
