const documentOperations = {
    async loadDocuments() {
        const tbody = document.getElementById('documentsList');
        if (!tbody) return;

        try {
            utils.showLoading(tbody);

            const response = await fetch('/api/documents');
            if (!response.ok) {
                throw new Error('Failed to fetch documents');
            }

            const documents = await response.json();

            if (!Array.isArray(documents) || documents.length === 0) {
                utils.showEmptyState(tbody);
                return;
            }

            tbody.innerHTML = '';
            documents.forEach(doc => {
                if (!doc || typeof doc !== 'object') return;

                const tr = document.createElement('tr');
                tr.className = 'document-item';
                tr.innerHTML = `
                    <td>
                        <i class="fas fa-file-alt me-2 text-muted"></i>
                        ${utils.escapeHtml(doc.name)}
                    </td>
                    <td>${utils.escapeHtml(doc.description || '-')}</td>
                    <td><span class="version-badge">${utils.escapeHtml(doc.currentVersion)}</span></td>
                    <td class="file-size">${utils.formatFileSize(doc.fileSize)}</td>
                    <td>
                        <span class="badge bg-${utils.getStatusColor(doc.status)}">
                            ${utils.escapeHtml(doc.status)}
                        </span>
                    </td>
                    <td>${utils.formatDate(doc.createdAt)}</td>
                    <td class="document-actions">
                        <div class="btn-group btn-group-sm">
                            <button type="button" class="btn btn-outline-primary"
                                    onclick="documentOperations.downloadDocument(${doc.id})"
                                    title="Download">
                                <i class="fas fa-download"></i>
                            </button>
                            <button type="button" class="btn btn-outline-info"
                                    onclick="documentOperations.showVersions(${doc.id})"
                                    title="Version History">
                                <i class="fas fa-history"></i>
                            </button>
                            <button type="button" class="btn btn-outline-secondary"
                                    onclick="documentOperations.showNewVersionModal(${doc.id})"
                                    title="Upload New Version">
                                <i class="fas fa-upload"></i>
                            </button>
                            <button type="button" class="btn btn-outline-danger"
                                    onclick="documentOperations.deleteDocument(${doc.id})"
                                    title="Delete">
                                <i class="fas fa-trash"></i>
                            </button>
                            <button type="button" class="btn btn-outline-success"
                                                                 onclick="documentOperations.viewDocument(${doc.id})"
                                                                 title="View">
                                                                 <i class="fas fa-eye"></i>
                                                        </button>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        } catch (error) {
            console.error('Error loading documents:', error);
            utils.showAlert('Failed to load documents', 'danger');
            utils.showEmptyState(tbody, 'Error loading documents');
        }
    },

    async downloadDocument(id) {
        try {
            const response = await fetch(`/api/documents/${id}`);
            if (!response.ok) {
                throw new Error('Failed to get document details');
            }

            const doc = await response.json();

            // Fix: Sử dụng window.document thay vì document
            const link = window.document.createElement('a');
            link.href = `/api/files/download/${encodeURIComponent(doc.path)}?encryptedKey=${doc.encryptedKey}`;
            link.download = doc.name;
            window.document.body.appendChild(link);
            link.click();
            window.document.body.removeChild(link);

            utils.showAlert('Document downloaded successfully', 'success');
        } catch (error) {
            console.error('Download error:', error);
            utils.showAlert('Failed to download document', 'danger');
        }
    },

    // Thêm phương thức download version
    async downloadVersion(versionId) {
        try {
            const response = await fetch(`/api/documents/versions/${versionId}`);
            if (!response.ok) {
                throw new Error('Failed to get version details');
            }

            const version = await response.json();

            const link = window.document.createElement('a');
            link.href = `/api/files/download/${encodeURIComponent(version.path)}?encryptedKey=${version.encryptedKey}`;
            link.download = version.path.split('/').pop();
            window.document.body.appendChild(link);
            link.click();
            window.document.body.removeChild(link);

            utils.showAlert('Version downloaded successfully', 'success');
        } catch (error) {
            console.error('Version download error:', error);
            utils.showAlert('Failed to download version', 'danger');
        }
    },

    async deleteDocument(id) {
        if (!utils.confirmDialog('Are you sure you want to delete this document? This action cannot be undone.')) {
            return;
        }

        try {
            const response = await fetch(`/api/documents/${id}`, {
                method: 'DELETE'
            });

            if (!response.ok) {
                throw new Error('Failed to delete document');
            }

            utils.showAlert('Document deleted successfully', 'success');
            await this.loadDocuments();
        } catch (error) {
            console.error('Delete error:', error);
            utils.showAlert('Failed to delete document', 'danger');
        }
    },

    async showVersions(id) {
        try {
            const response = await fetch(`/api/documents/${id}/versions`);
            if (!response.ok) {
                throw new Error('Failed to load versions');
            }

            const versions = await response.json();
            const tbody = document.getElementById('versionList');
            tbody.innerHTML = '';

            if (versions.length === 0) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="5" class="text-center">No versions found</td>
                    </tr>
                `;
                return;
            }

            versions.forEach(version => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><span class="version-badge">${utils.escapeHtml(version.version)}</span></td>
                    <td>${utils.formatDate(version.createdAt)}</td>
                    <td>${utils.formatFileSize(version.fileSize)}</td>
                    <td>${utils.escapeHtml(version.changeLog || '-')}</td>
                    <td>
                        <button type="button" class="btn btn-sm btn-outline-primary"
                                onclick="documentOperations.downloadVersion(${version.id})">
                            <i class="fas fa-download"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });

            const modal = new bootstrap.Modal(document.getElementById('versionModal'));
            modal.show();
        } catch (error) {
            console.error('Error loading versions:', error);
            utils.showAlert('Failed to load versions', 'danger');
        }
    },

    showNewVersionModal(id) {
        document.getElementById('documentId').value = id;
        const modal = new bootstrap.Modal(document.getElementById('newVersionModal'));
        modal.show();
    },

    async uploadNewVersion() {
        const formData = new FormData();
        const fileInput = document.getElementById('versionFile');
        const documentId = document.getElementById('documentId').value;

        if (!fileInput.files || fileInput.files.length === 0) {
            utils.showAlert('Please select a file', 'warning');
            return;
        }

        formData.append('file', fileInput.files[0]);
        formData.append('changeLog', document.getElementById('changeLog').value);

        try {
            utils.showProgress();
            const response = await fetch(`/api/documents/${documentId}/versions`, {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                throw new Error('Failed to upload new version');
            }

            utils.showAlert('New version uploaded successfully', 'success');
            bootstrap.Modal.getInstance(document.getElementById('newVersionModal')).hide();
            utils.resetForm('newVersionForm');
            await this.loadDocuments();
        } catch (error) {
            console.error('Upload error:', error);
            utils.showAlert('Failed to upload new version', 'danger');
        } finally {
            utils.hideProgress();
        }
    },
    async viewDocument(id) {
        try {
            const response = await fetch(`/api/viewer/document/${id}`);
            if (!response.ok) throw new Error('Failed to load document');

            const data = await response.json();

            // Set document content
            const contentDiv = document.getElementById('documentContent');
            contentDiv.innerHTML = '';

            // Add content with optional protection
            const contentWrapper = document.createElement('div');
            contentWrapper.className = 'position-relative';

            // Add main content
            const contentText = document.createElement('pre');
            contentText.textContent = data.content;
            contentText.className = data.preventScreenCapture ? 'document-content prevent-capture' : 'document-content';
            contentWrapper.appendChild(contentText);

            // Add watermark if needed
            if (data.hasWatermark) {
                const watermark = document.createElement('div');
                watermark.className = 'watermark-overlay';
                watermark.textContent = `CONFIDENTIAL - ${new Date().toLocaleString()}`;
                contentWrapper.appendChild(watermark);
            }

            contentDiv.appendChild(contentWrapper);

            // Update modal info
            document.getElementById('watermarkInfo').style.display = data.hasWatermark ? 'block' : 'none';
            document.getElementById('screenCaptureInfo').style.display = data.preventScreenCapture ? 'block' : 'none';

            // Setup protection if needed
            if (data.preventScreenCapture) {
                setupDocumentProtection();
            }

            // Show modal
            const modal = new bootstrap.Modal(document.getElementById('documentViewerModal'));
            modal.show();

        } catch (error) {
            console.error('View error:', error);
            utils.showAlert('Failed to view document', 'danger');
        }
    },

    async searchDocuments() {
        const searchTerm = document.getElementById('searchInput').value.trim();
        const tbody = document.getElementById('documentsList');

        try {
            utils.showLoading(tbody);

            let url = '/api/documents';
            if (searchTerm) {
                url = `/api/documents/search?term=${encodeURIComponent(searchTerm)}`;
            }

            const response = await fetch(url);
            if (!response.ok) {
                throw new Error('Search failed');
            }

            const documents = await response.json();

            if (documents.length === 0) {
                utils.showEmptyState(tbody, searchTerm ?
                    `No documents found matching "${searchTerm}"` :
                    'No documents found');
                return;
            }

            tbody.innerHTML = '';
            documents.forEach(doc => {
                const tr = document.createElement('tr');
                tr.className = 'document-item';
                tr.innerHTML = `
                    <td>
                        <i class="fas fa-file-alt me-2 text-muted"></i>
                        ${utils.escapeHtml(doc.name)}
                    </td>
                    <td>${utils.escapeHtml(doc.description || '-')}</td>
                    <td><span class="version-badge">${utils.escapeHtml(doc.currentVersion)}</span></td>
                    <td class="file-size">${utils.formatFileSize(doc.fileSize)}</td>
                    <td>
                        <span class="badge bg-${utils.getStatusColor(doc.status)}">
                            ${utils.escapeHtml(doc.status)}
                        </span>
                    </td>
                    <td>${utils.formatDate(doc.createdAt)}</td>
                    <td class="document-actions">
                        <div class="btn-group btn-group-sm">

                            <button type="button" class="btn btn-outline-primary"
                                    onclick="documentOperations.downloadDocument(${doc.id})"
                                    title="Download">
                                <i class="fas fa-download"></i>
                            </button>
                            <button type="button" class="btn btn-outline-info"
                                    onclick="documentOperations.showVersions(${doc.id})"
                                    title="Version History">
                                <i class="fas fa-history"></i>
                            </button>
                            <button type="button" class="btn btn-outline-secondary"
                                    onclick="documentOperations.showNewVersionModal(${doc.id})"
                                    title="Upload New Version">
                                <i class="fas fa-upload"></i>
                            </button>
                            <button type="button" class="btn btn-outline-danger"
                                    onclick="documentOperations.deleteDocument(${doc.id})"
                                    title="Delete">
                                <i class="fas fa-trash"></i>
                            </button>
                            <button type="button" class="btn btn-outline-success"
                                     onclick="documentOperations.viewDocument(${doc.id})"
                                     title="View">
                                     <i class="fas fa-eye"></i>
                            </button>
                        </div>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        } catch (error) {
            console.error('Search error:', error);
            utils.showAlert('Failed to search documents', 'danger');
            utils.showEmptyState(tbody, 'Error searching documents');
        }
    },

    async refreshDocuments() {
        const refreshButton = document.querySelector('a[onclick="documentOperations.refreshDocuments()"] i');
        refreshButton.classList.add('fa-spin');

        try {
            await this.loadDocuments();
            utils.showAlert('Documents refreshed successfully', 'success');
        } catch (error) {
            utils.showAlert('Failed to refresh documents', 'danger');
        } finally {
            refreshButton.classList.remove('fa-spin');
        }
    }
};