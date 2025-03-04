package com.payments.snappay;

// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Component
public class AnalyzePhotos {
    private RekognitionClient getClient() {
        return RekognitionClient.builder()
                .credentialsProvider(InstanceProfileCredentialsProvider.create())
                .region(Region.AP_SOUTH_1)
                .build();
    }

    public ArrayList<WorkItem> DetectLabels(byte[] bytes, String key) {
        try {
            RekognitionClient rekClient = RekognitionClient.builder()
                    .credentialsProvider(InstanceProfileCredentialsProvider.create())
                    .region(Region.AP_SOUTH_1)
                    .build();

            SdkBytes sourceBytes = SdkBytes.fromByteArray(bytes);
            Image souImage = Image.builder()
                    .bytes(sourceBytes)
                    .build();

            DetectLabelsRequest detectLabelsRequest = DetectLabelsRequest.builder()
                    .image(souImage)
                    .maxLabels(10)
                    .build();

            DetectLabelsResponse labelsResponse = rekClient.detectLabels(detectLabelsRequest);
            List<Label> labels = labelsResponse.labels();
            System.out.println("Detected labels for the given photo");
            ArrayList<WorkItem> list = new ArrayList<>();
            WorkItem item;
            for (Label label : labels) {
                item = new WorkItem();
                item.setKey(key); // identifies the photo
                item.setConfidence(label.confidence().toString());
                item.setName(label.name());
                list.add(item);
            }
            return list;

        } catch (RekognitionException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public void createMyCollection(String collectionId) {
        try (RekognitionClient rekClient = getClient()) {

            CreateCollectionRequest collectionRequest = CreateCollectionRequest.builder()
                    .collectionId(collectionId)
                    .build();

            CreateCollectionResponse collectionResponse = rekClient.createCollection(collectionRequest);
            System.out.println("CollectionArn: " + collectionResponse.collectionArn());
            System.out.println("Status code: " + collectionResponse.statusCode().toString());
        } catch (RekognitionException e) {
            System.out.println(e.getMessage());
        }
    }

    public String searchFaceInCollection(String collectionId, MultipartFile sourceImage) {
        try (RekognitionClient rekClient = getClient()) {
            InputStream sourceStream = new BufferedInputStream(sourceImage.getInputStream());
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);
            Image souImage = Image.builder()
                    .bytes(sourceBytes)
                    .build();

            SearchFacesByImageRequest facesByImageRequest = SearchFacesByImageRequest.builder()
                    .image(souImage)
                    .maxFaces(10)
                    .faceMatchThreshold(70F)
                    .collectionId(collectionId)
                    .build();

            SearchFacesByImageResponse imageResponse = rekClient.searchFacesByImage(facesByImageRequest);
            System.out.println("Faces matching in the collection");
            List<FaceMatch> faceImageMatches = imageResponse.faceMatches();
            for (FaceMatch face : faceImageMatches) {
                System.out.println("The similarity level is  " + face.similarity());
                System.out.println(face.face().faceId());
                if(face.similarity()>95)
                return face.face().faceId();
            }
        } catch (RekognitionException | FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<String> listAllCollections() {
        try (RekognitionClient rekClient = getClient()) {
            ListCollectionsRequest listCollectionsRequest = ListCollectionsRequest.builder()
                    .maxResults(10)
                    .build();

            ListCollectionsResponse response = rekClient.listCollections(listCollectionsRequest);
            List<String> collectionIds = response.collectionIds();
            for (String resultId : collectionIds) {
                System.out.println(resultId);
            }
            return collectionIds;
        } catch (RekognitionException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public String addToCollection(String collectionId, MultipartFile sourceImage) {
        try (RekognitionClient rekClient = getClient()) {
            InputStream sourceStream = new BufferedInputStream(sourceImage.getInputStream());
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);
            Image souImage = Image.builder()
                    .bytes(sourceBytes)
                    .build();

            IndexFacesRequest facesRequest = IndexFacesRequest.builder()
                    .collectionId(collectionId)
                    .image(souImage)
                    .maxFaces(1)
                    .qualityFilter(QualityFilter.AUTO)
                    .detectionAttributes(Attribute.DEFAULT)
                    .build();

            IndexFacesResponse facesResponse = rekClient.indexFaces(facesRequest);
            System.out.println("Results for the image");
            System.out.println("\n Faces indexed:");
            List<FaceRecord> faceRecords = facesResponse.faceRecords();
            for (FaceRecord faceRecord : faceRecords) {
                System.out.println("  Face ID: " + faceRecord.face().faceId());
                System.out.println("  Location:" + faceRecord.faceDetail().boundingBox().toString());
                return faceRecord.face().faceId();
            }

            List<UnindexedFace> unindexedFaces = facesResponse.unindexedFaces();
            System.out.println("Faces not indexed:");
            for (UnindexedFace unindexedFace : unindexedFaces) {
                System.out.println("  Location:" + unindexedFace.faceDetail().boundingBox().toString());
                System.out.println("  Reasons:");
                for (Reason reason : unindexedFace.reasons()) {
                    System.out.println("Reason:  " + reason);
                }
            }
            return null;
        } catch (RekognitionException | FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void deleteFacesCollection(String collectionId) {

        try (RekognitionClient rekClient = getClient()) {
            DeleteFacesRequest deleteFacesRequest = DeleteFacesRequest.builder()
                    .collectionId(collectionId)
                    .faceIds(listFacesCollection(collectionId))
                    .build();

            rekClient.deleteFaces(deleteFacesRequest);
            System.out.println("The faces were deleted from the collection.");

        } catch (RekognitionException e) {
            System.out.println(e.getMessage());
        }
    }

    public List<String> listFacesCollection(String collectionId) {
        List<String> faceIds =  new ArrayList<String>();
        try (RekognitionClient rekClient = getClient()) {
            ListFacesRequest facesRequest = ListFacesRequest.builder()
                    .collectionId(collectionId)
                    .maxResults(10)
                    .build();

            ListFacesResponse facesResponse = rekClient.listFaces(facesRequest);
            List<Face> faces = facesResponse.faces();
            for (Face face : faces) {
                System.out.println("Confidence level there is a face: " + face.confidence());
                System.out.println("The face Id value is " + face.faceId());
                faceIds.add(face.faceId());
            }
        } catch (RekognitionException e) {
            System.out.println(e.getMessage());
        }
        return faceIds;
    }
}