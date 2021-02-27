package com.correvate.fileuploader.util;

import com.correvate.fileuploader.rest.FileUploadController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    // we may extract buffer size to properties
    public static final int BUFFER_SIZE = 4096;

    public static void unzip(final String zippedFile, final String outPutDir) throws IOException {

        final File destDir = new File(outPutDir);

        final byte[] buffer = new byte[BUFFER_SIZE];
        final ZipInputStream zis = new ZipInputStream(new FileInputStream(zippedFile));

        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            final File newFile = newFile(destDir, zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                final File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                final FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

    public static void deleteDirectory(Path path) {
        deleteDirectory(path.toFile());
    }

    public static void deleteDirectory(File file) {
        if (file.isDirectory()) {
            File[] contents = file.listFiles();
            for (File f: contents) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }

    public static byte[] zipFiles(final Map<String, byte[]> fileContentMap) throws IOException {

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final ZipOutputStream zipOut = new ZipOutputStream(out)) {

            for (final String fileName : fileContentMap.keySet()) {

                LOG.debug("Archiving {}", fileName);

                final ByteArrayInputStream fis = new ByteArrayInputStream(fileContentMap.get(fileName));

                final ZipEntry zipEntry = new ZipEntry(fileName);
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[BUFFER_SIZE];
                int length;
                while((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                zipOut.closeEntry();

                LOG.debug("Archived {}", fileName);
            }
            zipOut.close();
            out.close();

            return out.toByteArray();
        }

    }

    public static Path createTempDir(String tmpDirPrefix) throws IOException {
        return Files.createTempDirectory(tmpDirPrefix);
    }

    public static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

}
