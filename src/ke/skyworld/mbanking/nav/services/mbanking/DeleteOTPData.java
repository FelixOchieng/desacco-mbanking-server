package ke.skyworld.mbanking.nav.services.mbanking;

import ke.skyworld.mbanking.mappapi.MAPPAPIDB;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * mbanking-server-desacco (ke.skyworld.mbanking.nav.persistent_threads)
 * Created by: dmutende
 * On: 07 Jun, 2024 13:58
 **/
public class DeleteOTPData implements Runnable {

    private final String SERVICE_NAME;

    public DeleteOTPData() {
        this.SERVICE_NAME = "DeleteOTPData";
    }

    public static void start(long startDelay, long intervalPeriod) {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

        executorService.scheduleAtFixedRate(new DeleteOTPData(),
                startDelay, intervalPeriod, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            Instant instStart; Instant instEnd; instStart = Instant.now();

            System.out.println(SERVICE_NAME +".execute(): Making Function Call...");
            MAPPAPIDB.fnDeleteOTPDataAfterTimeOut();
            instEnd = Instant.now();
            Duration durTimeElapsed = Duration.between(instStart, instEnd);
            System.out.println(SERVICE_NAME +".execute(): Responded after: " + durTimeElapsed.getSeconds() +" Seconds");

            instStart = null; instEnd = null; durTimeElapsed = null;

            // Check for interruptions and reset the interrupt status if necessary
            if (Thread.interrupted()) {
                // Re-interrupt the thread to ensure the interrupt status is preserved
                Thread.currentThread().interrupt();
            }

        } catch (OutOfMemoryError e) {
            System.err.println(SERVICE_NAME +".execute(): Out of memory: " + e.getMessage());
        } catch (Throwable t) {
            // Handle all other exceptions to ensure thread doesn't terminate
            System.err.println(SERVICE_NAME +".execute(): Unexpected error: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
