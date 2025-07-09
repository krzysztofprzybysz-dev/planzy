import { useState, useCallback } from 'react';

const usePagination = (totalPages, initialPage = 0) => {
    const [currentPage, setCurrentPage] = useState(initialPage);

    const handlePageChange = useCallback((newPage) => {
        setCurrentPage(newPage);
        // Scroll to top when changing page
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }, []);

    // Calculate page ranges for pagination display
    const getPaginationRange = useCallback(() => {
        const siblingCount = 1;
        const totalNumbers = siblingCount * 2 + 3; // siblings + current + first + last
        const totalBlocks = totalNumbers + 2; // +2 for dots

        if (totalPages <= totalBlocks) {
            return Array.from({ length: totalPages }, (_, i) => i);
        }

        const leftSiblingIndex = Math.max(0, currentPage - siblingCount);
        const rightSiblingIndex = Math.min(totalPages - 1, currentPage + siblingCount);

        const shouldShowLeftDots = leftSiblingIndex > 1;
        const shouldShowRightDots = rightSiblingIndex < totalPages - 2;

        const firstPageIndex = 0;
        const lastPageIndex = totalPages - 1;

        if (!shouldShowLeftDots && shouldShowRightDots) {
            const leftRange = Array.from(
                { length: 3 + 2 * siblingCount },
                (_, i) => i
            );

            return [...leftRange, -1, lastPageIndex];
        }

        if (shouldShowLeftDots && !shouldShowRightDots) {
            const rightRange = Array.from(
                { length: 3 + 2 * siblingCount },
                (_, i) => lastPageIndex - (3 + 2 * siblingCount) + i + 1
            );

            return [firstPageIndex, -1, ...rightRange];
        }

        if (shouldShowLeftDots && shouldShowRightDots) {
            const middleRange = Array.from(
                { length: rightSiblingIndex - leftSiblingIndex + 1 },
                (_, i) => leftSiblingIndex + i
            );

            return [firstPageIndex, -1, ...middleRange, -1, lastPageIndex];
        }
    }, [currentPage, totalPages]);

    return {
        currentPage,
        handlePageChange,
        getPaginationRange,
        hasNextPage: currentPage < totalPages - 1,
        hasPrevPage: currentPage > 0
    };
};

export default usePagination;