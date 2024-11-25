// main.js
document.addEventListener('DOMContentLoaded', () => {
    // Initialize document list
    documentOperations.loadDocuments();

    // Setup form submission
    const uploadForm = document.getElementById('uploadForm');
    if (uploadForm) {
        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            if (!uploadForm.checkValidity()) {
                uploadForm.classList.add('was-validated');
                return;
            }

            const formData = new FormData();
            const fileInput = document.getElementById('file');

            if (!fileInput.files || fileInput.files.length === 0) {
                utils.showAlert('Please select a file', 'warning');
                return;
            }

            formData.append('file', fileInput.files[0]);
            formData.append('description', document.getElementById('description').value);
            formData.append('addWatermark', document.getElementById('addWatermark').checked);
            formData.append('preventScreenCapture', document.getElementById('preventScreenCapture').checked);

            try {
                utils.showProgress();
                const response = await fetch('/api/documents/upload', {
                    method: 'POST',
                    body: formData
                });

                if (!response.ok) {
                    throw new Error(await response.text());
                }

                const result = await response.json();
                utils.showAlert('Document uploaded successfully', 'success');
                utils.resetForm('uploadForm');
                uploadForm.classList.remove('was-validated');
                await documentOperations.loadDocuments();
            } catch (error) {
                console.error('Upload error:', error);
                utils.showAlert(error.message || 'Upload failed', 'danger');
            } finally {
                utils.hideProgress();
            }
        });
    }

    // Setup search with debounce
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        // Debounce search on input
        searchInput.addEventListener('input', utils.debounce(async () => {
            try {
                await documentOperations.searchDocuments();
            } catch (error) {
                console.error('Search error:', error);
                utils.showAlert('Search failed', 'danger');
            }
        }, 500));

        // Search on Enter key
        searchInput.addEventListener('keypress', async (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                try {
                    await documentOperations.searchDocuments();
                } catch (error) {
                    console.error('Search error:', error);
                    utils.showAlert('Search failed', 'danger');
                }
            }
        });

        // Clear search results when input is empty
        searchInput.addEventListener('input', async (e) => {
            if (!e.target.value.trim()) {
                try {
                    await documentOperations.loadDocuments();
                } catch (error) {
                    console.error('Load error:', error);
                    utils.showAlert('Failed to load documents', 'danger');
                }
            }
        });
    }
    // Add protection setup function
    function setupDocumentProtection() {
        const content = document.getElementById('documentContent');

        // Prevent right click
        content.addEventListener('contextmenu', e => {
            e.preventDefault();
            return false;
        });

        // Prevent keyboard shortcuts
        document.addEventListener('keydown', e => {
            if (
                // Prevent screenshot shortcuts
                e.key === 'PrintScreen' ||
                // Prevent copy shortcuts
                (e.ctrlKey && e.key === 'c') ||
                // Prevent print shortcuts
                (e.ctrlKey && e.key === 'p') ||
                // Prevent save shortcuts
                (e.ctrlKey && e.key === 's')
            ) {
                e.preventDefault();
                utils.showAlert('This action is not allowed for protected documents', 'warning');
                return false;
            }
        });

        // Prevent copy/paste
        content.addEventListener('copy', e => {
            e.preventDefault();
            return false;
        });

        content.addEventListener('paste', e => {
            e.preventDefault();
            return false;
        });

        // Add blur protection
        window.addEventListener('blur', () => {
            content.style.filter = 'blur(10px)';
        });

        window.addEventListener('focus', () => {
            content.style.filter = 'none';
        });
    }
    // Setup search button
    const searchButton = document.querySelector('[onclick="documentOperations.searchDocuments()"]');
    if (searchButton) {
        searchButton.removeAttribute('onclick');
        searchButton.addEventListener('click', async (e) => {
            e.preventDefault();
            try {
                await documentOperations.searchDocuments();
            } catch (error) {
                console.error('Search error:', error);
                utils.showAlert('Search failed', 'danger');
            }
        });
    }

    // Setup screen capture prevention
    const preventScreenCapture = document.getElementById('preventScreenCapture');
    if (preventScreenCapture && preventScreenCapture.checked) {
        utils.setupScreenCapturePrevention();
    }


    // Add validation styles to forms
    const forms = document.querySelectorAll('.needs-validation');
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });
});