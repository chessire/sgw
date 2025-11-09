package sgw.app.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BatchApplication {

    public static void main(String[] args) {
        System.out.println("Starting Batch Application with scheduler support...");
        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext("batch-context.xml");
            context.registerShutdownHook();
            System.out.println("Batch Application started. Scheduled jobs will run automatically. Press Ctrl+C to exit.");
            Thread.currentThread().join();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            System.out.println("Batch Application interrupted.");
        } catch (Exception ex) {
            System.err.println("Batch application failed to start: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        } finally {
            if (context != null && context.isActive()) {
                context.close();
            }
        }
    }
}

