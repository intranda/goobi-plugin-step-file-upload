/**
 * Plugin JavaScript for File Upload functionality
 */

function initializeUploader() {
    var uploaderEl = document.getElementById("uploader");

    if(uploaderEl && window.uploaderConfig) {
        // Check if window.uploader exists and is a valid riot component (should have unmount method)
        if(!window.uploader || typeof window.uploader.unmount !== 'function') {
            // Clear any invalid uploader reference
            if(window.uploader && typeof window.uploader.unmount !== 'function') {
                window.uploader = null;
            }

            window.uploader = mountUploader(
                uploaderEl,
                {
                    plugin_name: window.uploaderConfig.pluginTitle,
                    goobi_opts: window.uploaderConfig.options
                }
            );
        }
    }
}

function unmountUploader() {
    if(window.uploader) {
        window.uploader.unmount();
        window.uploader = null;
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    initializeUploader();
});

faces.ajax.addOnEvent(function(data) {
    if(data.status === 'success') {
        initializeUploader();
    }
});