package dgroomes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Please see the README for more information.
 * <p>
 * This program simulates a scenario of "Fictional Data Processing" using a scheduled task to
 * illustrate the "sawtooth" memory pattern in Java, caused by the creation and garbage
 * collection of objects.
 * <p>
 * We utilize a ScheduledExecutorService to periodically generate "DataPackets" for processing.
 * These DataPackets are created in large numbers and then discarded, mimicking a real-world
 * data processing scenario where objects are frequently created and disposed of.
 * <p>
 * The scheduled task facilitates the creation of a sawtooth pattern in memory usage
 * by periodically triggering the generation of objects, followed by their eligible disposal
 * for garbage collection.
 */
public class Runner {

    private static final int DATA_PACKET_BATCH_SIZE = 10_000;
    private static final int PERIOD_MS = 500;

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) {
        log.info("Let's simulate some fictional data processing...");
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        // Schedule the work to run periodically. Let's estimate the rate of memory usage. Instances of DataPacket are
        // 16 bytes because on a 64-bit JVM process, an object has a 12-byte header and is padded to a multiple of 8
        // bytes. We have a pointer to each object because we reference the object from an ArrayList. A pointer is called
        // an Ordinary Object Pointer (OOP) in the JVM. An OOP is 64-bit (8 bytes). Note: I might actually be getting compressed
        // OOPs (32-bit) but I'm not sure. So, if we create 10,000 DataPacket instances every 100ms, that's equal to:
        //
        //     (16 + 8 bytes) * 10,000 / 500ms = 24 bytes * 20,000 / second = 480 KiB / second = .4 MiB / second
        //
        // So, we're generating at least **.4 MiB** of domain data every second. The total rate of memory consumption
        // is much higher due to the OpenTelemetry agent.
        //
        // This memory pattern can be visualized in a profiler like VisualVM or in the raw data via our OpenTelemetry/Telegraf/InfluxDB
        // stack. The memory usage accumulates in a sloped line up for about two minutes, and then drops off sharply as
        // the garbage collector kicks in. This repeats over and over again and creates the "sawtooth" pattern.
        executorService.scheduleAtFixedRate(Runner::generateAndDiscardDataPackets, 0, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * This is the fictional "do some work" method.
     */
    private static void generateAndDiscardDataPackets() {
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        List<DataPacket> dataPackets = new ArrayList<>();
        for (int i = 0; i < Runner.DATA_PACKET_BATCH_SIZE; i++) {
            dataPackets.add(new DataPacket());
        }
        // DataPackets are now ready to be processed (in reality, they are just placeholders)
    }
}

class DataPacket {
    // This class is just a placeholder for a real data packet
}
