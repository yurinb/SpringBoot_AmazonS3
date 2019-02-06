/*
@author YuriB
Class that makes the connection with Amazon S3 and handle upload, update and delete actions
 */
package com.example.demo.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AmazonClient {

    private AmazonS3 s3client;

    // Injects application.properties config values
    @Value("${amazonProperties.endpointUrl}")
    private String endpointUrl;
    @Value("${amazonProperties.bucketName}")
    private String bucketName;
    @Value("${amazonProperties.accessKey}")
    private String accessKey;
    @Value("${amazonProperties.secretKey}")
    private String secretKey;

    // Builds the Amazon S3 client to handle actions with bucket
    @PostConstruct
    private void initializeAmazon() {
        BasicAWSCredentials creds = new BasicAWSCredentials(this.accessKey, this.secretKey);
        s3client = AmazonS3Client.builder()
                .withRegion("sa-east-1")
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .build();
    }

    /**
     * Upload MultipartFile from POSTs requests to Amazon S3 bucket
     *
     * @param multipartFile - File to upload
     * @return String - File URL Ex:
     * https://s3-sa-east-1.amazonaws.com/bucketx/img.jpg
     */
    public String uploadFile(MultipartFile multipartFile) {
        String fileUrl = "";
        try {
            File file = convertMultiPartToFile(multipartFile);
            String fileName = generateFileName(multipartFile);
            fileUrl = endpointUrl + "/" + bucketName + "/" + fileName;
            uploadFileTos3bucket(fileName, file);
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileUrl;
    }

    /**
     * Removes file from Amazon S3 bucket by URL
     *
     * @param fileUrl - Ex: https://s3-sa-east-1.amazonaws.com/bucketx/img.jpg
     * @return String - "Succesfully deleted"
     */
    public String deleteFileFromS3Bucket(String fileUrl) {
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        s3client.deleteObject(new DeleteObjectRequest(bucketName + "/", fileName));
        return "Successfully deleted";
    }

    // Amazon S3 uses File to upload. We need to convert Multipart that comes from client to send it.
    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    // When  upload the same file twice or more, the name needs to be different or uploads fail.
    private String generateFileName(MultipartFile multiPart) {
        // Generate a unique name with current time and replace blank spaces with
        return new Date().getTime() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
    }

    // upload File
    private void uploadFileTos3bucket(String fileName, File file) {
        s3client.putObject(new PutObjectRequest(bucketName, fileName, file)
                .withCannedAcl(com.amazonaws.services.s3.model.CannedAccessControlList.Private));
    }
}
