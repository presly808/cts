package com.correvate.fileuploader.rest;

import com.correvate.fileuploader.util.FileUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileUploadController {

    private static final Logger LOG = LoggerFactory.getLogger(FileUploadController.class);

    // we may extract buffer size to properties
    private static final String ARCHIVE_NAME = "files.zip";

    @PostMapping("/multi-upload")
    public ResponseEntity multiUpload(@RequestParam("files") final MultipartFile[] files) throws IOException {

        LOG.info("Going to archive files and prepare for downloading");

        final Map<String, byte[]> fileContentMap = Arrays.stream(files)
                .collect(Collectors.toMap(MultipartFile::getOriginalFilename, file -> {
                    try {
                        return file.getBytes();
                    } catch (IOException e) {
                        LOG.error("Error while getting multipartFile bytes fileName {}. " +
                                "This file will be skipped in the archive", file.getOriginalFilename());
                        return null;
                    }
                }));

        long startTimeMillis = System.currentTimeMillis();
        final byte[] zipFileBytes = FileUtils.zipFiles(fileContentMap);
        long endTimeMillis = System.currentTimeMillis();

        LOG.info("Archive has been prepared in {} millis. Size {} Bytes",
                endTimeMillis - startTimeMillis, zipFileBytes.length);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + ARCHIVE_NAME);

        final InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(zipFileBytes));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(zipFileBytes.length)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

}
