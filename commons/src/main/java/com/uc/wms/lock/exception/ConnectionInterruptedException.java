/*
 * Copyright 2025 Unicommerce Technologies (P) Limited . All Rights Reserved.
 * UNICOMMERCE TECHONOLOGIES PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @author namanindranil
 */

package com.uc.wms.lock.exception;

public class ConnectionInterruptedException extends LockingException {

    public ConnectionInterruptedException() {
        super();
    }

    public ConnectionInterruptedException(String message) {
        super(message);
    }

    public ConnectionInterruptedException(Exception cause) {
        super(cause);
    }

    public ConnectionInterruptedException(String message, Exception cause) {
        super(message, cause);
    }
}
