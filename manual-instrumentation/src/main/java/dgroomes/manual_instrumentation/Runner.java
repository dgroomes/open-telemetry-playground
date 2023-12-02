package dgroomes.manual_instrumentation;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.instrumentation.runtimemetrics.java8.*;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

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
 *
 * The Java process is manually instrumented with the OpenTelemetry Java instrumentation library.
 */
public class Runner {

    private static final int DATA_PACKET_BATCH_SIZE = 10_000;
    private static final int PERIOD_MS = 500;

    private static final Logger log = LoggerFactory.getLogger(Runner.class);

    public static void main(String[] args) {
        log.info("Let's simulate some fictional data processing...");
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        registerMetrics();

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

    private static void registerMetrics() {

        // The OpenTelemetry instrumentation logs with JUL (java.util.logging). We want it to log through our preferred
        // logging system which is SLF4J (including the logging implementation 'slf4j-simple'). We can bridge JUL to
        // SLF4J using the 'jul-to-slf4j' library and the line below.
        //
        // This doesn't quite work. I'm not seeing OpenTelemetry logs. Keep researching. See https://stackoverflow.com/a/9117188
        SLF4JBridgeHandler.install();

        Resource resource = Resource.getDefault().toBuilder()
                .put(ResourceAttributes.SERVICE_NAME, "manual-instrumentation-server")
                .put(ResourceAttributes.SERVICE_VERSION, "0.1.0")
                .build();

        @SuppressWarnings("resource") SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(LoggingMetricExporter.create()).build())
                .setResource(resource)
                .build();

        @SuppressWarnings("resource") OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(sdkMeterProvider)
                // I don't think I need a propagator because I'm only using metrics, not traces.
                .setPropagators(ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance())))
                .buildAndRegisterGlobal();

        Classes.registerObservers(openTelemetry);
        Cpu.registerObservers(openTelemetry);
        MemoryPools.registerObservers(openTelemetry);
        Threads.registerObservers(openTelemetry);
        GarbageCollector.registerObservers(openTelemetry);
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
