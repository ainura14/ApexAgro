package com.apexagro.common.error;

/**
 * BE3-04: ресурс не найден или чужой. Для чужих ресурсов тоже 404, а не 403, чтобы не раскрывать, что id существует.
 */
public class NotFoundException extends ApiException {

    public NotFoundException() {
        super(CommonErrorCode.NOT_FOUND);
    }

    public NotFoundException(ErrorCode specificCode, Object... args) {
        super(specificCode, args);
    }
}
