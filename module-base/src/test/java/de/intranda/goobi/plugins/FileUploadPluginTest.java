package de.intranda.goobi.plugins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import de.sub.goobi.helper.Helper;

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

    /**
     * Builds a plugin instance whose step/process resolve the given config folder to the given path. A resolved folder of {@code null} reproduces
     * Process.getConfiguredImageFolder() returning null for a folder name that is unknown to Goobi.
     */
    private static FileUploadPlugin pluginResolvingFolder(String configFolder, String resolvedFolder) throws Exception {
        Process process = mock(Process.class);
        when(process.getConfiguredImageFolder(anyString())).thenReturn(resolvedFolder);
        Step step = mock(Step.class);
        when(step.getProzess()).thenReturn(process);

        FileUploadPlugin plugin = new FileUploadPlugin();
        plugin.setConfigFolder(configFolder);
        Field stepField = AbstractStepPlugin.class.getDeclaredField("myStep");
        stepField.setAccessible(true);
        stepField.set(plugin, step);
        return plugin;
    }

    @Test
    void changeFolder_unknownFolderName_doesNotThrow() throws Exception {
        FileUploadPlugin plugin = pluginResolvingFolder("photos", null);
        try (MockedStatic<Helper> helper = mockStatic(Helper.class)) {
            assertDoesNotThrow(plugin::changeFolder,
                    "An unknown folder name is a misconfiguration, not a reason to break the whole task page");
        }
    }

    @Test
    void changeFolder_unknownFolderName_reportsActionableError() throws Exception {
        FileUploadPlugin plugin = pluginResolvingFolder("photos", null);
        try (MockedStatic<Helper> helper = mockStatic(Helper.class)) {
            plugin.changeFolder();

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            helper.verify(() -> Helper.setFehlerMeldung(captor.capture()));
            String message = captor.getValue();
            assertTrue(message.contains("photos"),
                    "Message must name the folder that could not be resolved, was: " + message);
            assertTrue(message.contains("process.folder.images.photos"),
                    "Message must name the missing goobi_config.properties key, was: " + message);
        }
    }

    @Test
    void changeFolder_unknownFolderName_leavesFileListEmpty() throws Exception {
        FileUploadPlugin plugin = pluginResolvingFolder("photos", null);
        try (MockedStatic<Helper> helper = mockStatic(Helper.class)) {
            plugin.changeFolder();

            assertTrue(plugin.getUploadedFiles().isEmpty(),
                    "Without a resolvable folder there is nothing to list");
        }
    }

    @Test
    void loadUploadedFiles_afterUnknownFolderName_doesNotThrow() throws Exception {
        FileUploadPlugin plugin = pluginResolvingFolder("photos", null);
        try (MockedStatic<Helper> helper = mockStatic(Helper.class)) {
            plugin.changeFolder();

            assertDoesNotThrow(plugin::loadUploadedFiles,
                    "The overview tab must not NPE while the folder stays unresolved");
        }
    }
}
