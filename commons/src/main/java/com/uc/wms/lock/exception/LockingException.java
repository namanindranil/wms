/*
 * Copyright 2025 Unicommerce Technologies (P) Limited . All Rights Reserved.
 * UNICOMMERCE TECHONOLOGIES PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *    
 * @author namanindranil
 */

package com.uc.wms.lock.exception;

public class LockingException extends RuntimeException {

    private static final long serialVersionUID = -4630176287353001872L;

    public LockingException() {
        super();
    }

    public LockingException(String message) {
        super(message);
    }

    public LockingException(Exception cause) {
        super(cause);
    }

    public LockingException(String message, Exception cause) {
        super(message, cause);
    }
}
