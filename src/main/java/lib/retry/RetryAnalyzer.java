package lib.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(RetryAnalyzer.class);
    private static final int MAX_RETRY_COUNT = 2;
    private int retryCount = 0;

    private static final Set<String> retriedTests = ConcurrentHashMap.newKeySet();

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            String testKey = result.getTestClass().getName() + "." + result.getName();
            retriedTests.add(testKey);
            log.warn("Retrying test '{}' - attempt {}/{}",
                    result.getName(), retryCount, MAX_RETRY_COUNT);
            return true;
        }
        return false;
    }

    public static boolean wasRetried(ITestResult result) {
        String testKey = result.getTestClass().getName() + "." + result.getName();
        return retriedTests.contains(testKey);
    }

    public static Set<String> getRetriedTests() {
        return Collections.unmodifiableSet(retriedTests);
    }

    public static void reset() {
        retriedTests.clear();
    }
}
