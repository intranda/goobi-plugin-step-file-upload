package de.intranda.goobi.plugins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class FileUploadPluginTest {

    
    @Test
    void resolveSafePath_normalFilename_staysWithinBase() {
        Path base = Paths.get("/uploads/process42/master");
        assertDoesNotThrow(() -> FileUploadPlugin.resolveSafePath(base, "scan_001.tif"));
    }

    @Test
    void resolveSafePath_absolutePathInjection_throwsSecurityException() {
        // Paths.get(base, "/etc/passwd") silently discards the base in Java
        Path base = Paths.get("/uploads/process42/master");
        assertThrows(SecurityException.class,
                () -> FileUploadPlugin.resolveSafePath(base, "/etc/passwd"));
    }

    @Test
    void resolveSafePath_traversalSequence_throwsSecurityException() {
        Path base = Paths.get("/uploads/process42/master");
        assertThrows(SecurityException.class,
                () -> FileUploadPlugin.resolveSafePath(base, "../process99/secret.tif"));
    }

    @Test
    void buildContentDispositionHeader_asciiFilename_producesRfc5987Header() {
        String header = FileUploadPlugin.buildContentDispositionHeader("report.pdf");
        assertTrue(header.startsWith("attachment; filename*=UTF-8''"),
                "Header must use RFC 5987 extended notation");
        assertTrue(header.contains("report.pdf"),
                "Plain ASCII filename must appear unencoded");
    }

    @Test
    void buildContentDispositionHeader_filenameWithSpaces_encodesSpaces() {
        String header = FileUploadPlugin.buildContentDispositionHeader("my scan 001.tif");
        assertTrue(header.contains("%20"), "Spaces must be percent-encoded as %20, not +");
    }

    @Test
    void buildContentDispositionHeader_filenameWithUmlauts_encodesCorrectly() {
        String header = FileUploadPlugin.buildContentDispositionHeader("Akte_Müller.pdf");
        assertTrue(header.startsWith("attachment; filename*=UTF-8''"));
        assertTrue(header.contains("%C3%BC") || header.contains("%c3%bc"),
                "ü must be percent-encoded in UTF-8");
    }
}
