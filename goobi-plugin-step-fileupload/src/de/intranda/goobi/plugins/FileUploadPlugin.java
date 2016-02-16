package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;
import org.primefaces.event.FileUploadEvent;

import de.sub.goobi.helper.NIOFileUtils;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import lombok.extern.log4j.Log4j;

@PluginImplementation
@Log4j
public class FileUploadPlugin extends AbstractStepPlugin implements IStepPlugin, IPlugin {

    private static final String PLUGIN_NAME = "intranda_step_fileUpload";

    private String masterFolder;
    private Path folder;

    private String currentFile = null;
    private List<String> uploadedFiles = new ArrayList<String>();

    public void initialize(Step step, String returnPath) {
        super.returnPath = returnPath;
        super.myStep = step;
        try {
            masterFolder = myStep.getProzess().getImagesOrigDirectory(false);
            folder = Paths.get(masterFolder);
            if (!Files.exists(folder)) {
                Files.createDirectory(folder);
            }
            uploadedFiles = NIOFileUtils.list(folder.toString());

        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            log.error(e);
        }

    }

    @Override
    public boolean execute() {
        return false;
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.PART;
    }

    @Override
    public String getPagePath() {
        return null;
    }

    @Override
    public String getTitle() {
        return PLUGIN_NAME;
    }

    @Override
    public String getDescription() {
        return PLUGIN_NAME;
    }

    public void setUploadedFiles(List<String> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public List<String> getUploadedFiles() {
        return uploadedFiles;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public void deleteFile() {
        // remove from list
        if (uploadedFiles.contains(currentFile)) {
            uploadedFiles.remove(currentFile);
        }
        // delete file
        File f = new File(folder.toString(), currentFile);
        FileUtils.deleteQuietly(f);

    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            copyFile(event.getFile().getFileName(), event.getFile().getInputstream());

        } catch (IOException e) {
            log.error(e);
        }
        uploadedFiles.add(event.getFile().getFileName());
    }

    public void copyFile(String fileName, InputStream in) {
        OutputStream out = null;

        try {

            // write the inputStream to a FileOutputStream
            out = new FileOutputStream(new File(masterFolder, fileName));

            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }

        }

    }
}
