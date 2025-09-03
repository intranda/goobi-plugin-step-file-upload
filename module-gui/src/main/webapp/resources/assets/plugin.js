/**
 * Plugin JavaScript for File Upload functionality
 */

function initializeUploader() {
    var uploaderEl = document.getElementById("uploader");
    var placeholderEl = document.getElementById("uploader-placeholder");
    var persistentContainer = document.getElementById("uploader-persistent-container");

    if(uploaderEl && window.uploaderConfig) {
        // Check if we're on tab 1 (uploader should be visible)
        if(placeholderEl && persistentContainer) {
            // Position the persistent container over the placeholder
            positionUploaderContainer();
            persistentContainer.style.display = 'block';

            // Mount the uploader if it hasn't been mounted yet
            if(!window.uploader || typeof window.uploader.unmount !== 'function') {
                // Clear any invalid uploader reference
                // Sometimes the reference persists even though the uploader is not mounted
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

            // Set up observer to watch for content changes
            observeUploaderChanges();
        } else {
            // Tab 2 is active or placeholder not found - hide the uploader
            if(persistentContainer) {
                persistentContainer.style.display = 'none';
            }
        }
    }
}

function positionUploaderContainer() {
    var placeholderEl = document.getElementById("uploader-placeholder");
    var persistentContainer = document.getElementById("uploader-persistent-container");
    var uploaderEl = document.getElementById("uploader");

    if(placeholderEl && persistentContainer) {
        var rect = placeholderEl.getBoundingClientRect();
        var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        var scrollLeft = window.pageXOffset || document.documentElement.scrollLeft;

        persistentContainer.style.top = (rect.top + scrollTop) + 'px';
        persistentContainer.style.left = (rect.left + scrollLeft) + 'px';
        persistentContainer.style.width = rect.width + 'px';

        // Dynamically adjust placeholder height to match uploader content
        if(uploaderEl) {
            var uploaderHeight = uploaderEl.offsetHeight;
            var minHeight = 120; // minimum height from CSS
            var neededHeight = Math.max(minHeight, uploaderHeight);

            placeholderEl.style.height = neededHeight + 'px';
        }
    }
}

function observeUploaderChanges() {
    var uploaderEl = document.getElementById("uploader");

    if(uploaderEl && window.MutationObserver) {
        // Disconnect existing observer if any exist
        if(window.uploaderObserver) {
            window.uploaderObserver.disconnect();
        }

        // Create new observer to watch for content changes
        window.uploaderObserver = new MutationObserver(function(mutations) {
            // Debounce the repositioning to avoid excessive calls
            clearTimeout(window.uploaderResizeTimeout);
            window.uploaderResizeTimeout = setTimeout(function() {
                positionUploaderContainer();
            }, 100);
        });

        // Start observing
        window.uploaderObserver.observe(uploaderEl, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['style', 'class']
        });
    }
}

function unmountUploader() {
    if(window.uploader) {
        window.uploader.unmount();
        window.uploader = null;
    }

    // Clean up observer
    if(window.uploaderObserver) {
        window.uploaderObserver.disconnect();
        window.uploaderObserver = null;
    }

    // Clear timeout
    if(window.uploaderResizeTimeout) {
        clearTimeout(window.uploaderResizeTimeout);
        window.uploaderResizeTimeout = null;
    }

    var persistentContainer = document.getElementById("uploader-persistent-container");
    if(persistentContainer) {
        persistentContainer.style.display = 'none';
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
        setTimeout(function() {
            initializeUploader();
        }, 50);
    }
});

// Handle window resize to reposition uploader
window.addEventListener('resize', function() {
    positionUploaderContainer();
});

// Handle scroll to reposition uploader if needed
window.addEventListener('scroll', function() {
    var persistentContainer = document.getElementById("uploader-persistent-container");
    if(persistentContainer && persistentContainer.style.display !== 'none') {
        positionUploaderContainer();
    }
});