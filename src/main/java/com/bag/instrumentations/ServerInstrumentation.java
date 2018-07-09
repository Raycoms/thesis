package com.bag.instrumentations;

import com.bag.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Used to report performance information on the server
 */
public class ServerInstrumentation
{
    /**
     * Amount of aborts since last reset.
     */
    private AtomicInteger abortedTransactions = new AtomicInteger(0);

    /**
     * Amount of aborts since last reset.
     */
    private AtomicInteger abortedWrites = new AtomicInteger(0);

    /**
     * Amount of commits since last reset.
     */
    private AtomicInteger committedTransactions = new AtomicInteger(0);

    /**
     * Reads performed during the last measurement
     */
    private AtomicInteger readsPerformed = new AtomicInteger(0);

    /**
     * Writes performed during the last measurement
     */
    private AtomicInteger writesPerformed = new AtomicInteger(0);

    /**
     * Reads performed during the last measurement
     */
    private AtomicInteger averageCommitTime = new AtomicInteger(0);

    /**
     * Writes performed during the last measurement
     */
    private AtomicInteger averageValidationTime = new AtomicInteger(0);

    /**
     * Lock used to synchronize writes to the results file.
     */
    private final Object resultsFileLock = new Object();

    /**
     * Minutes elapsed since the start.
     */
    private int minutesElapsed;

    /**
     * Timer for the instrumentation.
     */
    private final Timer instrumentationTimer = new Timer();

    public ServerInstrumentation(final int id)
    {
        minutesElapsed = 0;
        instrumentationTimer.scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {

                synchronized (resultsFileLock)
                {
                    minutesElapsed += 1;

                    try (final FileWriter file = new FileWriter(System.getProperty("user.home") + "/results" + id + ".txt", true);
                         final BufferedWriter bw = new BufferedWriter(file);
                         final PrintWriter out = new PrintWriter(bw))
                    {
                        //out.print(elapsed + ";");
                        //out.print(abortedTransactions.get() + ";");
                        //out.print(committedTransactions.get() + ";");
                        //out.print(readsPerformed.get() + ";");
                        //out.print(writesPerformed.get() + ";");
                        out.println(readsPerformed.get() + writesPerformed.get());
                        //out.println(averageCommitTime.get() + "/" + averageValidationTime.get());


                        //out.println();

                        System.out.println(String.format("Elapsed: (%d seconds)\nAborted: %d\nCommited: %d\nReads, Writes, Throughput:\n %d, %d, %d\n \nAborted Writes: %d\n ",  minutesElapsed, abortedTransactions.get(), committedTransactions.get(), readsPerformed.get(),
                                writesPerformed.get(), readsPerformed.get() + (writesPerformed.get()/8), abortedWrites.get()));

                        abortedTransactions = new AtomicInteger(0);
                        committedTransactions = new AtomicInteger(0);
                        readsPerformed = new AtomicInteger(0);
                        writesPerformed = new AtomicInteger(0);
                        averageCommitTime = new AtomicInteger(0);
                        averageValidationTime = new AtomicInteger(0);
                        abortedWrites = new AtomicInteger(0);
                    }
                    catch (final IOException e)
                    {
                        Log.getLogger().info("Problem while writing to file!", e);
                    }
                }
            }
        }, 20000, 60000);
    }

    /**
     * Sets the commit time, average.
     * @param time the new time.
     */
    public void setCommitTime(final int time)
    {
        if (averageCommitTime.get() == 0)
        {
            averageCommitTime.set(time);
        }
        else
        {
            averageCommitTime.set((averageCommitTime.get() + time) / 2);
        }
    }

    /**
     * Sets the validation time, average.
     * @param time the new time.
     */
    public void setValidationTime(final int time)
    {
        if (averageValidationTime.get() == 0)
        {
            averageValidationTime.set(time);
        }
        else
        {
            averageValidationTime.set((averageValidationTime.get() + time) / 2);
        }
    }

    /**
     * Increment the aborted writes.
     */
    public void updateAbortedWrites()
    {
        abortedWrites.incrementAndGet();
    }

    /**
     * Updates the counts.
     * @param writes the write count to update.
     * @param reads the read count to update.
     * @param commits the commit count to update.
     * @param aborts the abort count to update.
     */
    public void updateCounts(final int writes, final int reads, final int commits, final int aborts)
    {
        if (writes > 0)
        {
            writesPerformed.addAndGet(writes);
        }
        if (reads > 0)
        {
            readsPerformed.addAndGet(reads);
        }
        if (commits > 0)
        {
            committedTransactions.addAndGet(commits);
        }
        if (aborts > 0)
        {
            abortedTransactions.addAndGet(aborts);
        }
    }
}
