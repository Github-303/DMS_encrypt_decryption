// utils.js
const utils = {
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    },

    formatDate(dateString) {
        return new Date(dateString).toLocaleString();
    },

    showAlert(message, type = 'info') {
        const alertContainer = document.getElementById('alertContainer');
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.innerHTML = `
            <div class="d-flex align-items-center">
                <i class="fas fa-${type === 'success' ? 'check-circle' :
                                type === 'warning' ? 'exclamation-triangle' :
                                type === 'danger' ? 'exclamation-circle' :
                                'info-circle'} me-2"></i>
                <div>${message}</div>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        `;

        alertContainer.appendChild(alertDiv);

        setTimeout(() => {
            alertDiv.classList.remove('show');
            setTimeout(() => alertDiv.remove(), 300);
        }, 5000);
    },

    showLoading(element) {
        const template = document.getElementById('loadingSpinner');
        const spinner = template.content.cloneNode(true);
        element.innerHTML = '';
        element.appendChild(spinner);
    },

    showEmptyState(element, message = 'No documents found') {
        const template = document.getElementById('emptyState');
        const clone = template.content.cloneNode(true);
        clone.querySelector('p').textContent = message;
        element.innerHTML = '';
        element.appendChild(clone);
    },

    resetForm(formId) {
        const form = document.getElementById(formId);
        if (form) {
            form.reset();
            utils.hideProgress();
        }
    },

    showProgress() {
        const progressDiv = document.querySelector('.upload-progress');
        if (progressDiv) {
            progressDiv.classList.remove('d-none');
            const progressBar = progressDiv.querySelector('.progress-bar');
            if (progressBar) {
                progressBar.style.width = '0%';
                progressBar.setAttribute('aria-valuenow', '0');
            }
        }
    },

    hideProgress() {
        const progressDiv = document.querySelector('.upload-progress');
        if (progressDiv) {
            progressDiv.classList.add('d-none');
        }
    },

    updateProgress(percent) {
        const progressBar = document.querySelector('.progress-bar');
        if (progressBar) {
            progressBar.style.width = `${percent}%`;
            progressBar.setAttribute('aria-valuenow', percent);
        }
    },

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    confirmDialog(message) {
        return confirm(message);
    },

    setupScreenCapturePrevention() {
        document.addEventListener('keydown', (e) => {
            if (e.key === 'PrintScreen' || (e.ctrlKey && e.key === 'p')) {
                e.preventDefault();
                utils.showAlert('Screen capture is not allowed', 'warning');
            }
        });
    },

    escapeHtml(unsafe) {
        return unsafe
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    },

    getStatusColor(status) {
        const colors = {
            'Active': 'success',
            'Archived': 'warning',
            'Deleted': 'danger'
        };
        return colors[status] || 'secondary';
    }
};