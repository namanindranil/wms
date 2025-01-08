

package com.uc.wms.aspect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


import com.uc.wms.aspect.locking.ILockingService;
import com.uc.wms.annotation.Locks;
import com.uc.wms.expressions.Expression;
import com.uc.wms.lock.exception.LockingException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

@Aspect
public class LockingAspect {

    private static final Logger LOG = LoggerFactory.getLogger(LockingAspect.class);

    @Autowired
    private ILockingService lockingService;

    @Around("execution(* *(..)) && @annotation(locksAnnotation)")
    public Object executeAfterLock(ProceedingJoinPoint pjp, Locks locksAnnotation) throws Throwable {
        int numLocks = locksAnnotation.value().length;
        List<Lock> locksTaken = new ArrayList<>(numLocks);
        Map<String, Object> contextParams = new HashMap<>(1);
        contextParams.put("args", pjp.getArgs());
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        boolean log = false;
        try {
            Lock lock = null;
            for (com.uc.wms.annotation.Lock lockAnnotation : locksAnnotation.value()) {
                Object lockKeyObj = Expression.compile(lockAnnotation.key()).evaluate(contextParams);
                String lockKey;
                if (lockKeyObj == null) {
                    throw new IllegalArgumentException("Failed to evaluate lock expression");
                } else {
                    String originalLockKey = lockKeyObj.toString();
                    lockKey = originalLockKey.replaceAll("[^a-zA-Z0-9_]", "_");
                    if (!lockKey.equals(originalLockKey)) {
                        LOG.info("Modified lockKey to: {} from: {}", lockKey, originalLockKey);
                    }
                }
                lock = lockingService.getLock(lockAnnotation.ns(), lockKey, lockAnnotation.level(), ms.getMethod().getName());
                long timeout = lockAnnotation.timeoutInSeconds();
                long start = System.currentTimeMillis();
                if (timeout == -1) {
                    if (lockAnnotation.log()) {
                        LOG.info("Acquiring lock on namespace: {} and key: {} without timeout", lockAnnotation.ns(), lockKey);
                    }
                    lock.lock();
                    if (lockAnnotation.log()) {
                        log = true;
                        LOG.info("Lock acquired on namespace: {} and key: {} in {} ms", lockAnnotation.ns(), lockKey,
                                System.currentTimeMillis() - start);
                    }
                    locksTaken.add(lock);
                } else {
                    if (timeout == Long.MIN_VALUE) {
                        timeout = getLockWaitTimeoutInSeconds();
                    }
                    if (lockAnnotation.log()) {
                        LOG.info("Acquiring lock on namespace: {} and key: {} with timeout: {} sec", new Object[] { lockAnnotation.ns(), lockKey, timeout });
                    }
                    if (lock.tryLock(timeout, TimeUnit.SECONDS)) {
                        if (lockAnnotation.log()) {
                            log = true;
                            LOG.info("Lock acquired on namespace: {} and key: {} in {} ms", new Object[] { lockAnnotation.ns(), lockKey, System.currentTimeMillis() - start });
                        }
                        locksTaken.add(lock);
                    } else {
                        if (lockAnnotation.log()) {
                            LOG.info("Unable to acquire lock on namespace: {} and key: {} in {} ms", lockAnnotation.ns(), lockKey, (System.currentTimeMillis() - start));
                        }
                        break;
                    }
                }
            }
            if (locksTaken.size() == numLocks) {
                return pjp.proceed();
            } else {
                LOG.error("Failed to obtain lock for request, {} ", lock);
                throw new LockingException("Failed to obtain lock. " + lock);
            }
        } finally {
            for (Lock lock : locksTaken) {
                try {
                    long start = System.currentTimeMillis();
                    lock.unlock();
                    if (log) {
                        LOG.info("Released lock on path: {} in {} ms", new Object[] { lock.toString(), System.currentTimeMillis() - start });
                    }
                } catch (Throwable t) {
                    LOG.error("[FATAL] Failed to release lock on path: {}", lock.toString(), t);
                }
            }
        }
    }

    private long getLockWaitTimeoutInSeconds() {
        return 60;
    }
}
