package com.correvate.fileuploader.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.correvate.fileuploader.util.FileUtils;
import com.github.javafaker.Faker;
import com.github.javafaker.Lorem;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadControllerTest {

	public static final int NUM_OF_FILES_TO_SEND = 10;

	@LocalServerPort
	private int port;

	private RestTemplate restTemplate;

	@BeforeEach
	public void beforeAll(){
		restTemplate = new RestTemplate();
	}

	@Test
	void expectMultipleFilesUploading() throws IOException {
		// GIVEN
        final Faker instance = new Faker();
        final Lorem lorem = instance.lorem();

		final Map<String, Long> fileCheckSumMap = new HashMap<>();

        final MultiValueMap<String, HttpEntity<byte[]>> body
                = new LinkedMultiValueMap<>();

        for (int i = 0; i < NUM_OF_FILES_TO_SEND; i++) {
            final byte[] fileContent = lorem.paragraph().getBytes();
            final String fileName = "file" + i + ".txt";

			fileCheckSumMap.put(fileName, FileUtils.getCRC32Checksum(fileContent));
			final String multiPartRequestParamName = "files";
			body.add(multiPartRequestParamName, getTempFile(fileContent, fileName, multiPartRequestParamName));
        }

		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		final HttpEntity<MultiValueMap<String, HttpEntity<byte[]>>> requestEntity
				= new HttpEntity<>(body, headers);

		final String serverUrl = "http://localhost:"+ port + "/multi-upload";

		// WHEN
		final ResponseEntity<byte[]> exchange = restTemplate.exchange(serverUrl, HttpMethod.POST, requestEntity, byte[].class);

		final Path tempDir = FileUtils.createTempDir(UUID.randomUUID().toString().substring(8));

		// saving archive
		final Path zipFilePath = Path.of(tempDir + File.separator + "zipFile.zip");
		Files.write(zipFilePath, exchange.getBody());

		// unzip archive
		final String outPutDir = tempDir + File.separator;
		FileUtils.unzip(zipFilePath.toFile().getAbsolutePath(), outPutDir);

		// delete zipFile as we have extracted the archive
		Files.delete(zipFilePath);

		// get all unzipped files
		final Stream<Path> unzippedFiles = Files.list(Path.of(outPutDir));

		// THEN
		// compare each file with form data files that have been sent to server
        assertThat(unzippedFiles.anyMatch(el -> {
            final Long checkSum = fileCheckSumMap.get(el.toFile().getName());
            try {
                return checkSum != null && checkSum == FileUtils.getCRC32Checksum(Files.readAllBytes(el));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        })).isTrue();

        // delete temp directories
        FileUtils.deleteDirectory(Path.of(outPutDir));
        FileUtils.deleteDirectory(tempDir);
	}

	private static HttpEntity<byte[]> getTempFile(byte[] fileContent,
												  final String multiPartFileName,
												  final String multiPartRequestParamName) {
		final MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();

		final ContentDisposition contentDisposition = ContentDisposition
				.builder("form-data")
				.name(multiPartRequestParamName)
				.filename(multiPartFileName)
				.build();

		fileMap.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

		return new HttpEntity<>(fileContent, fileMap);
	}

}
