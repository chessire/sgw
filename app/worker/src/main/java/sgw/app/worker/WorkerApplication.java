package sgw.app.worker;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class WorkerApplication {

    private static final Logger log = LoggerFactory.getLogger(WorkerApplication.class);

    public static void main(String[] args) throws InterruptedException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("worker-context.xml");
        CountDownLatch latch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down worker application");
            try {
                context.close();
            } catch (Exception ex) {
                log.warn("Error while closing application context", ex);
            }
            latch.countDown();
        }));

        log.info("Worker application started");
        latch.await();
    }
}

