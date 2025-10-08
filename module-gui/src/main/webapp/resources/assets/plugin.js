/**
 * Plugin JavaScript for File Upload functionality
 * Simplified approach with uploader outside AJAX render areas
 */

function initializeUploader() {
    var uploaderEl = document.getElementById("uploader");
    var persistentContainer = document.getElementById("uploader-persistent-container");
    const uploaderContainer = document.querySelector("[id$='uploaderTabContent']");
    const tabState = uploaderContainer.dataset.tabState === 'tab2' ? 'overview' : 'upload';

    if(uploaderEl && window.uploaderConfig && persistentContainer) {
        if (tabState === 'upload') {
            console.log("Displaying uploader in persistent container.");
            persistentContainer.style.display = 'block';
            // Mount the uploader if it hasn't been mounted yet
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
        } else if (tabState === 'overview') {
            console.log("Hiding uploader in persistent container.");
            persistentContainer.style.display = 'none';
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

// Re-initialize after AJAX updates
faces.ajax.addOnEvent(function(data) {
    if(data.status === 'success') {
        // Small delay to ensure DOM is updated
        initializeUploader();
    }
});
