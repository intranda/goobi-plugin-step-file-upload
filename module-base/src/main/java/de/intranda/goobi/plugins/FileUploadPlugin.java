package de.intranda.goobi.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.IOUtils;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.FilesystemHelper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class FileUploadPlugin extends AbstractStepPlugin implements IStepPlugin, IPlugin {

    private static final long serialVersionUID = 1881863588326852628L;

    private static final String PLUGIN_NAME = "intranda_step_fileUpload";

    @Getter
    @Setter
    private String configFolder;
    private long size;
    private transient Path path;

    private String allowedTypes;

    private String currentFile = null;
    private List<String> uploadedFiles = new ArrayList<>();
    @Getter
    private List<String> allowedFolder = null;

    @Override
    public void initialize(Step step, String returnPath) {
        super.returnPath = returnPath;
        super.myStep = step;
        String projectName = step.getProzess().getProjekt().getTitel();
        XMLConfiguration xmlConfig = ConfigPlugins.getPluginConfig(PLUGIN_NAME);
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());

        SubnodeConfiguration myconfig = null;

        // order of configuration is:
        // 1.) project name and step name matches
        // 2.) step name matches and project is *
        // 3.) project name matches and step name is *
        // 4.) project name and step name are *
        try {
            myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '" + step.getTitel() + "']");
        } catch (IllegalArgumentException e) {
            try {
                myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '" + step.getTitel() + "']");
            } catch (IllegalArgumentException e1) {
                try {
                    myconfig = xmlConfig.configurationAt("//config[./project = '" + projectName + "'][./step = '*']");
                } catch (IllegalArgumentException e2) {
                    myconfig = xmlConfig.configurationAt("//config[./project = '*'][./step = '*']");
                }
            }
        }

        allowedTypes = myconfig.getString("regex", "/(\\.|\\/)(gif|jpe?g|png|tiff?|jp2|pdf)$/");
        allowedFolder = Arrays.asList(myconfig.getStringArray("folder"));
        configFolder = allowedFolder.get(0);
        changeFolder();
    }

    public void changeFolder() {
        try {
            String folder = myStep.getProzess().getConfiguredImageFolder(configFolder);
            path = Paths.get(folder);
            if (!StorageProvider.getInstance().isFileExists(path)) {
                StorageProvider.getInstance().createDirectories(path);
            }
            loadUploadedFiles();
        } catch (SwapException | DAOException | IOException e) {
            log.error(e);
        }

    }

    public void loadUploadedFiles() {
        this.uploadedFiles = StorageProvider.getInstance().list(path.toString());
        try {
            this.size = StorageProvider.getInstance().getDirectorySize(path);
        } catch (IOException e) {
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
        return getTitle();
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

    /**
     * get the size of a file that is listed inside of the configured directory
     * 
     * @param file name of the file to get the size of
     * @return size as String in MB, GB or TB
     */
    public String getFileSize(String file) {
        String result = "-";
        Path f = Paths.get(path.toString(), file);
        try {
            long fileSize = StorageProvider.getInstance().getFileSize(f);
            result = FilesystemHelper.getFileSizeShort(fileSize);
        } catch (IOException e) {
            log.error(e);
        }
        return result;
    }

    /**
     * get the size of the configured directory
     * 
     * @return size as String in MB, GB or TB
     */
    public String getDirectorySize() {
        return FilesystemHelper.getFileSizeShort(size);
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public String getAllowedTypes() {
        return allowedTypes;
    }

    public void deleteFile() {
        // delete file
        Path f = Paths.get(path.toString(), currentFile);
        try {
            StorageProvider.getInstance().deleteFile(f);
        } catch (IOException e) {
            log.error(e);
        }
        loadUploadedFiles();
    }

    public void downloadFile() {
        Path f = Paths.get(path.toString(), currentFile);
        try (InputStream in = StorageProvider.getInstance().newInputStream(f)) {
            FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
            ExternalContext ec = facesContext.getExternalContext();
            ec.responseReset();
            ec.setResponseHeader("Content-Disposition", "attachment; filename=" + f.getFileName().toString());
            ec.setResponseContentLength((int) StorageProvider.getInstance().getFileSize(f));

            IOUtils.copy(in, ec.getResponseOutputStream());

            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
    }

    public void deleteAllFiles() {
        for (String file : uploadedFiles) {
            Path f = Paths.get(path.toString(), file);
            try {
                StorageProvider.getInstance().deleteFile(f);
            } catch (IOException e) {
                log.error(e);
            }
        }
        loadUploadedFiles();
    }

    public void downloadAllImages() {

        try {
            FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
            ExternalContext ec = facesContext.getExternalContext();
            ec.responseReset();
            ec.setResponseContentType("application/zip");
            ec.setResponseHeader("Content-Disposition", "attachment; filename=" + myStep.getProzess().getTitel() + ".zip");

            try (ZipOutputStream out = new ZipOutputStream(ec.getResponseOutputStream())) {

                for (String file : uploadedFiles) {

                    Path currentImagePath = Paths.get(path.toString(), file);
                    try (InputStream in = StorageProvider.getInstance().newInputStream(currentImagePath)) {
                        out.putNextEntry(new ZipEntry(file));
                        byte[] b = new byte[1024];
                        int count;

                        while ((count = in.read(b)) > 0) {
                            out.write(b, 0, count);
                        }
                    }
                }
            }

            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        }
    }

}
