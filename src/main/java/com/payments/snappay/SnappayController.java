package com.payments.snappay;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Controller
public class SnappayController {
    // Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0


    // Change to your Bucket Name
    private final String bucketName = "myTestBucket";

    private final S3Service s3Service;
    private final AnalyzePhotos photos;

    private final SendMessages sendMessage;

    @Autowired
    SnappayController(
            S3Service s3Service,
            AnalyzePhotos photos,
            SendMessages sendMessage) {
        this.s3Service = s3Service;
        this.photos = photos;
        this.sendMessage = sendMessage;
    }

    @GetMapping("/")
    public String root() {
        return "index";
    }

    @GetMapping("/process")
    public String process() {
        return "process";
    }

    @GetMapping("/photo")
    public String photo() {
        return "upload";
    }

    @RequestMapping(value = "/createBucket", method = RequestMethod.POST)
    @ResponseBody
    void createBucket(HttpServletRequest request, HttpServletResponse response) {
        s3Service.createBucket(bucketName);
    }

    @RequestMapping(value = "/getimages", method = RequestMethod.GET)
    @ResponseBody
    String getImages(HttpServletRequest request, HttpServletResponse response) {
        return s3Service.ListAllObjects(bucketName);
    }

    // Generates a report that analyzes photos in a given bucket.
//        @RequestMapping(value = "/report", method = RequestMethod.POST)
//        @ResponseBody
//        String report(HttpServletRequest request, HttpServletResponse response) {
//            // Get a list of key names in the given bucket.
//            String email = request.getParameter("email");
//            ArrayList<String> myKeys = s3Service.ListBucketObjects(bucketName);
//            ArrayList<List<WorkItem>> myList = new ArrayList<>();
//            for (String myKey : myKeys) {
//                byte[] keyData = s3Service.getObjectBytes(bucketName, myKey);
//                ArrayList<WorkItem> item = photos.DetectLabels(keyData, myKey);
//                myList.add(item);
//            }
//
//            // Now we have a list of WorkItems describing the photos in the S3 bucket.
//            InputStream excelData = excel.exportExcel(myList);
//            try {
//                // Email the report.
//                sendMessage.sendReport(excelData, email);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            return "The photos have been analyzed and the report is sent";
//        }

    // Upload a video to analyze.
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    @ResponseBody
    public ModelAndView singleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            // Put the file into the bucket.
            byte[] bytes = file.getBytes();
            String name = file.getOriginalFilename();
            s3Service.putObject(bytes, bucketName, name);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView(new RedirectView("photo"));
    }

    // This controller method downloads the given image from the Amazon S3 bucket.
    @RequestMapping(value = "/downloadphoto", method = RequestMethod.GET)
    void buildDynamicReportDownload(HttpServletRequest request, HttpServletResponse response) {
        try {
            String photoKey = request.getParameter("photoKey");
            byte[] photoBytes = s3Service.getObjectBytes(bucketName, photoKey);
            InputStream is = new ByteArrayInputStream(photoBytes);

            // Define the required information here.
            response.setContentType("image/png");
            response.setHeader("Content-disposition", "attachment; filename=" + photoKey);
            org.apache.commons.io.IOUtils.copy(is, response.getOutputStream());
            response.flushBuffer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


