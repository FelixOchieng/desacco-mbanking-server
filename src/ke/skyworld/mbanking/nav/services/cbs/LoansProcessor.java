package ke.skyworld.mbanking.nav.services.cbs;

import ke.skyworld.mbanking.nav.cbs.CBSAPI;

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
public class LoansProcessor implements Runnable {

    private final String CBS_SERVICE_NAME;
    private final int CBS_SERVICE;

    public LoansProcessor() {
        this.CBS_SERVICE_NAME = "LoansProcessor";
        this.CBS_SERVICE = 2; //LOANS
    }

    public static void start(long startDelay, long intervalPeriod) {
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);

        executorService.scheduleAtFixedRate(new LoansProcessor(),
                startDelay, intervalPeriod, TimeUnit.SECONDS);
    }

    @Override
    public void run() {
        try {
            Instant instStart; Instant instEnd; instStart = Instant.now();

            System.out.println(CBS_SERVICE_NAME+".callServiceFunction("+CBS_SERVICE+"): Making CallService API Call...");
            CBSAPI.callServiceFunction(CBS_SERVICE);
            instEnd = Instant.now();
            Duration durTimeElapsed = Duration.between(instStart, instEnd);
            System.out.println(CBS_SERVICE_NAME+".callServiceFunction("+CBS_SERVICE+"): Responded after: " + durTimeElapsed.getSeconds() +" Seconds");

            instStart = null; instEnd = null; durTimeElapsed = null;

            // Check for interruptions and reset the interrupt status if necessary
            if (Thread.interrupted()) {
                // Re-interrupt the thread to ensure the interrupt status is preserved
                Thread.currentThread().interrupt();
            }

        } catch (OutOfMemoryError e) {
            System.err.println(CBS_SERVICE_NAME+".callServiceFunction("+CBS_SERVICE+"): Out of memory: " + e.getMessage());
        } catch (Throwable t) {
            // Handle all other exceptions to ensure thread doesn't terminate
            System.err.println(CBS_SERVICE_NAME+".callServiceFunction("+CBS_SERVICE+"): Unexpected error: " + t.getMessage());
            t.printStackTrace();
        }
    }
}
