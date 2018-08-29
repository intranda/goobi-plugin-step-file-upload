package de.intranda.goobi.plugins;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.beans.Step;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.plugin.interfaces.AbstractStepPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IStepPlugin;
import org.primefaces.event.FileUploadEvent;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j
public class FileUploadPlugin extends AbstractStepPlugin implements IStepPlugin, IPlugin {

    private static final String PLUGIN_NAME = "intranda_step_fileUpload";

    private String folder;
    private Path path;

    private String allowedTypes;

    private String currentFile = null;
    private List<String> uploadedFiles = new ArrayList<>();

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
            myconfig = xmlConfig
                    .configurationAt("//config[./project = '" + projectName + "'][./step = '" + step.getTitel() + "']");
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
        try {
            if (myconfig.getString("folder", "master").equals("master")) {
                folder = myStep.getProzess().getImagesOrigDirectory(false);
            } else {
                folder = myStep.getProzess().getImagesTifDirectory(false);
            }
            path = Paths.get(folder);
            if (!StorageProvider.getInstance().isFileExists(path)) {
                StorageProvider.getInstance().createDirectories(path);
            }
            loadUploadedFiles();
        } catch (SwapException | DAOException | IOException | InterruptedException e) {
            log.error(e);
        }

    }

    private void loadUploadedFiles() {
        if (!StorageProvider.getInstance().isFileExists(path)) {
            Helper.setFehlerMeldung("couldNotCreateImageFolder");
        } else {
            uploadedFiles = StorageProvider.getInstance().list(path.toString());
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

    public void handleFileUpload(FileUploadEvent event) {
        try {
            copyFile(event.getFile().getFileName(), event.getFile().getInputstream());

        } catch (IOException e) {
            log.error(e);
        }

        loadUploadedFiles();
    }

    public void copyFile(String fileName, InputStream in) {

        try {

            // write the inputStream to destination file
            StorageProvider.getInstance().uploadFile(in, Paths.get(folder, fileName));

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

        }

    }

    public void downloadAllImages() {

        BufferedInputStream buf = null;

        try {
            Path tempfile = Files.createTempFile(myStep.getProzess().getTitel(), ".zip");

            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempfile.toFile()));

            for (String file : uploadedFiles) {

                Path currentImagePath = Paths.get(path.toString(), file);
                InputStream in = StorageProvider.getInstance().newInputStream(currentImagePath);
                out.putNextEntry(new ZipEntry(file));
                byte[] b = new byte[1024];
                int count;

                while ((count = in.read(b)) > 0) {
                    out.write(b, 0, count);
                }
                in.close();

            }
            out.close();

            FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
            ExternalContext ec = facesContext.getExternalContext();
            ec.responseReset();
            ec.setResponseContentType("application/zip");
            ec.setResponseContentLength((int) Files.size(tempfile));

            ec.setResponseHeader("Content-Disposition", "attachment; filename=" + myStep.getProzess().getTitel() + ".zip");
            OutputStream responseOutputStream = ec.getResponseOutputStream();

            FileInputStream input = new FileInputStream(tempfile.toString());
            buf = new BufferedInputStream(input);
            int readBytes = 0;

            //read from the file; write to the ServletOutputStream
            while ((readBytes = buf.read()) != -1) {
                responseOutputStream.write(readBytes);
            }
            responseOutputStream.flush();
            responseOutputStream.close();
            facesContext.responseComplete();
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (buf != null) {
                try {
                    buf.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

}
