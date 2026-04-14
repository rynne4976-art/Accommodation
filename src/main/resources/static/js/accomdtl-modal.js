(function () {
    'use strict';

    const config = window.accomDtlConfig || {};
    let selectedReviewFiles = [];
    let selectedEditReviewFiles = [];

    function getCsrfHeader() {
        return document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || '';
    }

    function getCsrfToken() {
        return document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
    }

    function getAuthHeaders() {
        const headers = {
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'application/json'
        };
        const csrfHeader = getCsrfHeader();
        const csrfToken = getCsrfToken();

        if (csrfHeader && csrfToken) {
            headers[csrfHeader] = csrfToken;
        }

        return headers;
    }

    function openModal(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) {
            return;
        }

        modal.classList.add('is-open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('modal-open');
    }

    function closeModal(modalId) {
        const modal = document.getElementById(modalId);
        if (!modal) {
            return;
        }

        if (modal.contains(document.activeElement)) {
            document.activeElement.blur();
        }

        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');

        if (!document.querySelector('.review-modal.is-open')) {
            document.body.classList.remove('modal-open');
        }
    }

    function updateStarButtons(containerId, rating) {
        const container = document.getElementById(containerId);
        if (!container) {
            return;
        }

        container.querySelectorAll('button').forEach(function (button, index) {
            button.classList.toggle('active', index < rating);
        });
    }

    function renderReadOnlyStars(container, rating) {
        if (!container) {
            return;
        }

        container.innerHTML = '';

        for (let i = 1; i <= 5; i += 1) {
            const star = document.createElement('span');
            star.className = 'review-star' + (i <= rating ? ' filled' : '');
            star.textContent = '★';
            container.appendChild(star);
        }
    }

    function initReadOnlyStarRatings() {
        document.querySelectorAll('[data-readonly-rating-container]').forEach(function (container) {
            const rating = Number(container.getAttribute('data-rating') || 0);
            renderReadOnlyStars(container, rating);
        });
    }

    function setModalRating(rating) {
        const ratingInput = document.getElementById('modalReviewRating');
        if (ratingInput) {
            ratingInput.value = String(rating);
        }
        updateStarButtons('modalStarRating', rating);
    }

    function setEditModalRating(rating) {
        const ratingInput = document.getElementById('editModalReviewRating');
        if (ratingInput) {
            ratingInput.value = String(rating);
        }
        updateStarButtons('editModalStarRating', rating);
    }

    function renderFilePreview(previewListId, files) {
        const previewList = document.getElementById(previewListId);
        if (!previewList) {
            return;
        }

        previewList.innerHTML = '';

        Array.from(files || []).forEach(function (file) {
            const item = document.createElement('div');
            item.className = 'modal-preview-item';

            const image = document.createElement('img');
            image.alt = file.name;
            image.src = URL.createObjectURL(file);
            image.onload = function () {
                URL.revokeObjectURL(image.src);
            };

            const label = document.createElement('span');
            label.textContent = file.name;

            const removeButton = document.createElement('button');
            removeButton.type = 'button';
            removeButton.className = 'modal-preview-remove';
            removeButton.textContent = config.reviewImageRemoveLabel || '선택 취소';
            removeButton.addEventListener('click', function () {
                removeSelectedFile(previewListId, file);
            });

            item.appendChild(image);
            item.appendChild(label);
            item.appendChild(removeButton);
            previewList.appendChild(item);
        });
    }

    function applyLimitedFiles(input, files) {
        const safeFiles = Array.from(files || []).filter(function (file) {
            return file && file.type.startsWith('image/');
        });

        if (safeFiles.length !== Array.from(files || []).length) {
            alert(config.reviewImageOnlyMessage || '이미지 파일만 등록할 수 있습니다.');
        }

        return safeFiles;
    }

    function mergeSelectedFiles(existingFiles, newFiles) {
        const mergedFiles = Array.from(existingFiles || []);

        Array.from(newFiles || []).forEach(function (newFile) {
            const duplicated = mergedFiles.some(function (existingFile) {
                return existingFile.name === newFile.name
                    && existingFile.size === newFile.size
                    && existingFile.lastModified === newFile.lastModified;
            });

            if (!duplicated) {
                mergedFiles.push(newFile);
            }
        });

        return mergedFiles;
    }

    function syncInputFiles(inputId, files) {
        const input = document.getElementById(inputId);
        if (!input) {
            return;
        }

        const dataTransfer = new DataTransfer();
        Array.from(files || []).forEach(function (file) {
            dataTransfer.items.add(file);
        });
        input.files = dataTransfer.files;
    }

    function removeSelectedFile(previewListId, targetFile) {
        if (previewListId === 'modalReviewPreviewList') {
            selectedReviewFiles = selectedReviewFiles.filter(function (file) {
                return !(file.name === targetFile.name
                    && file.size === targetFile.size
                    && file.lastModified === targetFile.lastModified);
            });
            syncInputFiles('modalReviewImages', selectedReviewFiles);
            renderFilePreview(previewListId, selectedReviewFiles);
            return;
        }

        selectedEditReviewFiles = selectedEditReviewFiles.filter(function (file) {
            return !(file.name === targetFile.name
                && file.size === targetFile.size
                && file.lastModified === targetFile.lastModified);
        });
        syncInputFiles('editModalReviewImages', selectedEditReviewFiles);
        renderFilePreview(previewListId, selectedEditReviewFiles);
    }

    function handleReviewFiles(input) {
        const newFiles = applyLimitedFiles(input, input.files);
        selectedReviewFiles = mergeSelectedFiles(selectedReviewFiles, newFiles);
        syncInputFiles('modalReviewImages', selectedReviewFiles);
        renderFilePreview('modalReviewPreviewList', selectedReviewFiles);
    }

    function handleEditReviewFiles(input) {
        const newFiles = applyLimitedFiles(input, input.files);
        selectedEditReviewFiles = mergeSelectedFiles(selectedEditReviewFiles, newFiles);
        syncInputFiles('editModalReviewImages', selectedEditReviewFiles);
        renderFilePreview('editModalReviewPreviewList', selectedEditReviewFiles);
    }

    function buildReviewFormData(form, files) {
        const formData = new FormData(form);

        formData.delete('reviewImgFileList');

        Array.from(files || []).forEach(function (file) {
            formData.append('reviewImgFileList', file);
        });

        return formData;
    }

    function moveToReviewSection(accomId) {
        const targetUrl = (config.reviewDetailBaseUrl || '/accom/') + accomId + '#review-info';
        const currentPath = window.location.pathname;
        const targetPath = '/accom/' + accomId;

        // Same-page hash navigation does not trigger a server render,
        // so add a throwaway query string to force the detail page to reload.
        if (currentPath === targetPath) {
            window.location.href = targetPath + '?reviewRefresh=' + Date.now() + '#review-info';
            return;
        }

        window.location.href = targetUrl;
    }

    function moveToLoginWithRedirect() {
        const redirectUrl = window.location.pathname + window.location.search + '#review-info';
        window.location.href = '/members/login?redirectUrl=' + encodeURIComponent(redirectUrl);
    }

    function isLoginRedirectResponse(response) {
        return response.status === 401
            || response.redirected
            || response.url.includes('/members/login');
    }

    async function readJsonResponse(response) {
        if (isLoginRedirectResponse(response)) {
            throw new Error('LOGIN_REQUIRED');
        }

        const contentType = response.headers.get('content-type') || '';
        if (!contentType.includes('application/json')) {
            throw new Error('UNEXPECTED_RESPONSE');
        }

        return response.json();
    }

    function openReviewModal(button) {
        const isLoggedIn = button.getAttribute('data-logged-in') === 'true';
        const canWriteReview = button.getAttribute('data-can-write-review') === 'true';
        const denyMessage = button.getAttribute('data-deny-message');

        if (!isLoggedIn) {
            alert(config.loginRequiredMessage || '로그인 후 리뷰를 작성할 수 있습니다.');
            return;
        }

        if (!canWriteReview) {
            alert(denyMessage || config.reviewEligibilityMessage || '리뷰를 작성할 수 없습니다.');
            return;
        }

        const form = document.getElementById('reviewModalForm');
        if (form) {
            form.reset();
        }

        selectedReviewFiles = [];
        renderFilePreview('modalReviewPreviewList', []);
        setModalRating(0);
        openModal('reviewModal');
    }

    function closeReviewModal() {
        closeModal('reviewModal');
    }

    function openMyReviewModal() {
        const currentRating = Number(document.getElementById('editModalReviewRating')?.value || 0);
        setEditModalRating(currentRating);

        selectedEditReviewFiles = [];
        renderFilePreview('editModalReviewPreviewList', []);
        openModal('myReviewModal');
    }

    function closeMyReviewModal() {
        closeModal('myReviewModal');
    }

    async function submitReviewAjax() {
        const form = document.getElementById('reviewModalForm');
        if (!form) {
            return;
        }

        const rating = Number(document.getElementById('modalReviewRating')?.value || 0);
        const content = document.getElementById('modalReviewContent')?.value?.trim() || '';

        if (rating < 1 || rating > 5) {
            alert(config.reviewRatingRequiredMessage || '별점을 선택해 주세요.');
            return;
        }

        if (!content) {
            alert(config.reviewContentRequiredMessage || '리뷰 내용을 입력해 주세요.');
            return;
        }

        try {
            const formData = buildReviewFormData(form, selectedReviewFiles);
            const response = await fetch(config.reviewCreateUrl || '/reviews/new/ajax', {
                method: 'POST',
                headers: getAuthHeaders(),
                body: formData
            });

            const result = await readJsonResponse(response);
            alert(result.message || config.reviewRequestFailedMessage || '리뷰 요청을 처리하지 못했습니다.');

            if (result.success) {
                closeReviewModal();
                moveToReviewSection(result.accomId);
            }
        } catch (error) {
            if (error.message === 'LOGIN_REQUIRED') {
                moveToLoginWithRedirect();
                return;
            }
            alert(config.reviewRequestFailedMessage || '리뷰 요청을 처리하지 못했습니다.');
            console.error(error);
        }
    }

    async function submitReviewUpdateAjax(reviewId, accomId) {
        const form = document.getElementById('myReviewForm');
        if (!form) {
            return;
        }

        const rating = Number(document.getElementById('editModalReviewRating')?.value || 0);
        const content = document.getElementById('editModalReviewContent')?.value?.trim() || '';

        if (rating < 1 || rating > 5) {
            alert(config.reviewRatingRequiredMessage || '별점을 선택해 주세요.');
            return;
        }

        if (!content) {
            alert(config.reviewContentRequiredMessage || '리뷰 내용을 입력해 주세요.');
            return;
        }

        const formData = buildReviewFormData(form, selectedEditReviewFiles);
        const url = (config.reviewUpdateUrlTemplate || '/reviews/{reviewId}/update/ajax')
            .replace('{reviewId}', reviewId);

        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: formData
            });

            const result = await readJsonResponse(response);
            alert(result.message || config.reviewRequestFailedMessage || '리뷰 요청을 처리하지 못했습니다.');

            if (result.success) {
                closeMyReviewModal();
                moveToReviewSection(result.accomId || accomId);
            }
        } catch (error) {
            if (error.message === 'LOGIN_REQUIRED') {
                moveToLoginWithRedirect();
                return;
            }
            alert(config.reviewRequestFailedMessage || '리뷰 요청을 처리하지 못했습니다.');
            console.error(error);
        }
    }

    async function deleteReviewAjax(reviewId, accomId) {
        if (!confirm('리뷰를 삭제하시겠습니까?')) {
            return;
        }

        const url = (config.reviewDeleteUrlTemplate || '/reviews/{reviewId}/delete/ajax')
            .replace('{reviewId}', reviewId);

        const formData = new URLSearchParams();
        formData.append('accomId', accomId);

        try {
            const response = await fetch(url, {
                method: 'POST',
                headers: {
                    ...getAuthHeaders(),
                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
                },
                body: formData.toString()
            });

            const result = await readJsonResponse(response);
            alert(result.message || config.reviewRequestFailedMessage || '리뷰 요청을 처리하지 못했습니다.');

            if (result.success) {
                closeMyReviewModal();
                moveToReviewSection(result.accomId || accomId);
            }
        } catch (error) {
            if (error.message === 'LOGIN_REQUIRED') {
                moveToLoginWithRedirect();
                return;
            }
            alert(config.reviewRequestFailedMessage || '리뷰 요청을 처리하지 못했습니다.');
            console.error(error);
        }
    }

    function syncExistingReviewImageEmptyState() {
        const existingImageItems = document.querySelectorAll('[data-review-image-item]');
        const emptyState = document.getElementById('existingReviewImageEmptyState');
        const actionBar = document.querySelector('.existing-review-image-actions');

        if (emptyState) {
            emptyState.style.display = existingImageItems.length === 0 ? 'block' : 'none';
        }

        if (actionBar) {
            actionBar.style.display = existingImageItems.length === 0 ? 'none' : 'flex';
        }
    }

    function getSelectedExistingReviewImageIds() {
        return Array.from(document.querySelectorAll('.existing-review-image-checkbox:checked'))
            .map(function (checkbox) {
                return checkbox.value;
            });
    }

    function toggleAllExistingReviewImages(checked) {
        document.querySelectorAll('.existing-review-image-checkbox').forEach(function (checkbox) {
            checkbox.checked = checked;
        });
    }

    async function requestDeleteReviewImage(reviewId, reviewImgId, accomId) {
        const url = (config.reviewImageDeleteUrlTemplate || '/reviews/{reviewId}/images/{reviewImgId}/delete/ajax')
            .replace('{reviewId}', reviewId)
            .replace('{reviewImgId}', reviewImgId);

        const formData = new URLSearchParams();
        formData.append('accomId', accomId);

        const response = await fetch(url, {
            method: 'POST',
            headers: {
                ...getAuthHeaders(),
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            body: formData.toString()
        });

        return readJsonResponse(response);
    }

    async function deleteSelectedReviewImagesAjax(reviewId, accomId) {
        const selectedIds = getSelectedExistingReviewImageIds();

        if (selectedIds.length === 0) {
            alert(config.reviewImageSelectionRequiredMessage || '삭제할 이미지를 선택해 주세요.');
            return;
        }

        if (!confirm((config.reviewImageBulkDeleteConfirmMessage || '선택한 이미지를 삭제하시겠습니까?')
            .replace('{count}', String(selectedIds.length)))) {
            return;
        }

        let deletedCount = 0;

        for (const reviewImgId of selectedIds) {
            try {
                const result = await requestDeleteReviewImage(reviewId, reviewImgId, accomId);
                if (result.success) {
                    const imageItem = document.querySelector('[data-review-image-item="' + reviewImgId + '"]');
                    if (imageItem) {
                        imageItem.remove();
                    }
                    deletedCount += 1;
                }
            } catch (error) {
                if (error.message === 'LOGIN_REQUIRED') {
                    moveToLoginWithRedirect();
                    return;
                }
                console.error(error);
            }
        }

        syncExistingReviewImageEmptyState();

        if (deletedCount === 0) {
            alert(config.reviewRequestFailedMessage || '리뷰 요청을 처리하지 못했습니다.');
            return;
        }

        alert((config.reviewImageBulkDeleteResultMessage || '{count}개의 이미지를 삭제했습니다.')
            .replace('{count}', String(deletedCount)));
    }

    document.addEventListener('DOMContentLoaded', function () {
        const reviewImageInput = document.getElementById('modalReviewImages');
        const editReviewImageInput = document.getElementById('editModalReviewImages');

        if (reviewImageInput) {
            reviewImageInput.addEventListener('change', function () {
                handleReviewFiles(reviewImageInput);
            });
        }

        if (editReviewImageInput) {
            editReviewImageInput.addEventListener('change', function () {
                handleEditReviewFiles(editReviewImageInput);
            });
        }

        setModalRating(Number(document.getElementById('modalReviewRating')?.value || 0));
        setEditModalRating(Number(document.getElementById('editModalReviewRating')?.value || 0));
        initReadOnlyStarRatings();
        syncExistingReviewImageEmptyState();

        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                closeReviewModal();
                closeMyReviewModal();
            }
        });
    });

    window.handleReviewFiles = handleReviewFiles;
    window.handleEditReviewFiles = handleEditReviewFiles;
    window.openReviewModal = openReviewModal;
    window.closeReviewModal = closeReviewModal;
    window.openMyReviewModal = openMyReviewModal;
    window.closeMyReviewModal = closeMyReviewModal;
    window.setModalRating = setModalRating;
    window.setEditModalRating = setEditModalRating;
    window.submitReviewAjax = submitReviewAjax;
    window.submitReviewUpdateAjax = submitReviewUpdateAjax;
    window.deleteReviewAjax = deleteReviewAjax;
    window.deleteSelectedReviewImagesAjax = deleteSelectedReviewImagesAjax;
    window.toggleAllExistingReviewImages = toggleAllExistingReviewImages;
})();
