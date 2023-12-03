# manual-instrumentation

An example program-under-observation instrumented with OpenTelemetry using *manual instrumentation*.


## Overview

In some cases, you may use the OpenTelemetry Java _agent_ to instrument your program because it's powerful and requires
no changes to the program's source code. OpenTelemetry refers to this style of instrumentation as [*Automatic Instrumentation*](https://opentelemetry.io/docs/instrumentation/java/automatic/).
This is especially useful for third-party programs where you don't have access to the source code. For your own software
projects, you may want to exercise more precise control over the exact dependencies, configuration, and behavior of the
OpenTelemetry instrumentation. In this project, we instrument an example program the manual way. Refer to the OpenTelemetry
docs on [*Manual Instrumentation* for Java](https://opentelemetry.io/docs/instrumentation/java/manual/).

In the same spirit of exercising more control, we'll also opt out of *auto configuration* and instead configure the
OpenTelemetry Java instrumentation directly. To take it a step further we'll opt out of the OkHttp-based OpenTelemetry
*sender* because we would prefer to use the HTTP client built-in to JDK itself: `java.net.http.HttpClient`. We want to
keep our dependencies to a minimum, so that our software maintenance burden is low. OkHttp itself brings in a dependency
on Okio and the Kotlin standard library and runtime. Read more about the dependencies involved in exporting telemetry
data in the [*Dependencies* section](https://opentelemetry.io/docs/instrumentation/java/exporters/#otlp-dependencies) of
the OpenTelemetry Java instrumentation docs. 

The tech stack in this subproject:

* A program-under-observation
  * This is a fictional "data processing" program written in Java. This program is instrumented manually with the
    OpenTelemetry Java instrumentation libraries.
* An HTTP/Protobuf-OTLP metrics collector (OpenTelemetry Collector)
  * This runs as a Docker container and receives metrics data pushed from the OpenTelemetry instrumentation in the
    program-under-observation. The OpenTelemetry Collector forwards the metrics data to the Telegraf server using gRPC.
* A gRPC/Protobuf-OTLP metrics collector and ILP converter/forwarder (Telegraf)
  * This runs as a Docker container and accepts OTLP metrics from the OpenTelemetry Collector via gRPC, and then
    re-formats the metrics into an acceptable format for the metrics database (Influx Line Protocol) and then writes the
    metrics into the metrics backend (InfluxDB). 
* A metrics database (InfluxDB)
  * InfluxDB is an open source time series database that's often used for metrics. 

Note: The fact that we're using two metrics collectors is silly. We're working around a patchy matrix of technology
support (gRPC/HTTP/OTLP/ILP) among a matrix of telemetry and metrics systems (Influx/OpenTelemetry). We want our
program-under-observation to be constrained to Protobuf and HTTP. We don't want to pay for gRPC support in our program.
Unfortunately, Telegraf's OpenTelemetry receiver only supports gRPC, so we have to use the OpenTelemetry Collector as
an intermediary. Relatedly, in the spirit of "keep it simple", check out OpenTelemetry's support for JSON-encoded OTLP
data which is described in the [*JSON Protobuf Encoding *](https://opentelemetry.io/docs/specs/otlp/#otlphttp) section
of the [OTLP 1.0 spec](https://opentelemetry.io/docs/specs/otlp/). Can we remove the Protobuf dependency from our program-under-observation?
Usually we're using JSON already. I'd rather send gzipped JSON than pay for the software maintenance of a Protobuf dependency.


## Instructions

Follow these instructions to build and run the example system.

1. Pre-requisites: Java and Docker
    * I used Java 17.
2. Start infrastructure services
    * ```shell
      docker-compose up
      ```
    * This starts the OpenTelemetry Collector, Telegraf and InfluxDB.
    * Pay attention to the output of these containers as they run. It's a tricky system to set up, and you'll want to
      know if there are any errors, like if Telegraf is unable to connect to InfluxDB.
3. Build the program distribution
    * ```shell
      ./gradlew installDist
      ```
4. Run the program 
    * ```shell
      ./build/install/manual-instrumentation/bin/manual-instrumentation
      ```
    * The program will run indefinitely and continuously submit OTLP-based metrics data to the OpenTelemetry Collector.
      The program output should look something like the following.
    * ```text
      15:14:13 [main] INFO dgroomes.manual_instrumentation.Runner - Let's simulate some fictional data processing...
      15:14:13 [main] DEBUG io.opentelemetry.exporter.internal.http.HttpExporterBuilder - Using HttpSender: io.opentelemetry.exporter.sender.jdk.internal.JdkHttpSender
      15:14:13 [main] DEBUG io.opentelemetry.sdk.internal.JavaVersionSpecific - Using the APIs optimized for: Java 9+
      ```
5. Wait a minute
    * This is important! There is a natural delay throughout the system. The OpenTelemetry instrumentation only collects
      new values once a minute.
6. Inspect the metrics in InfluxDB directly
    * Start an `influx` session inside the InfluxDB container with the following command.
    * ```shell
      docker exec -it manual-instrumentation-influxdb-1 influx -precision rfc3339
      ```
    * The `influx` session may remind you of a SQL session. In it, you can run commands like `show databases` and
      `show measurements` to explore the data. We named our database `playground`. You should be able to connect to it
      by issuing a `use playground` command. Then, execute a `show measurements` command, and hopefully it shows the
      following metrics that have flowed from our program through the OpenTelemetry Collector, then through Telegraf and
      finally into the Influx database. It should look something like the following.
    * ```text
      $ docker exec -it manual-instrumentation-influxdb-1 influx
      Connected to http://localhost:8086 version 1.8.10
      InfluxDB shell version: 1.8.10
      > use playground
      Using database playground
      > show measurements
      name: measurements
      name
      ----
      process.runtime.jvm.classes.current_loaded
      process.runtime.jvm.classes.loaded
      process.runtime.jvm.classes.unloaded
      process.runtime.jvm.cpu.utilization
      process.runtime.jvm.gc.duration
      process.runtime.jvm.memory.committed
      process.runtime.jvm.memory.init
      process.runtime.jvm.memory.limit
      process.runtime.jvm.memory.usage
      process.runtime.jvm.memory.usage_after_last_gc
      process.runtime.jvm.system.cpu.load_1m
      process.runtime.jvm.system.cpu.utilization
      process.runtime.jvm.threads.count
      ```
    * Let's inspect the memory usage over time for our "data processing" program. This is captured in the `process.runtime.jvm.memory.usage`
      metric. Look at the below snippet for an example. The output shows the memory usage in MiB over time. The memory
      usage varies between 21Mib and 289MiB in the ten-minute window shown below.
    * ```text
      > SELECT SUM(gauge) / 1024 / 1024 AS "MiB" FROM "process.runtime.jvm.memory.usage" WHERE type = 'heap' GROUP BY time(1m)
      name: process.runtime.jvm.memory.usage
      time                 MiB
      ----                 ---
      2023-12-03T21:15:00Z 21.398468017578125
      2023-12-03T21:16:00Z 17.587753295898438
      2023-12-03T21:17:00Z 57.58775329589844
      2023-12-03T21:18:00Z 93.58775329589844
      2023-12-03T21:19:00Z 133.58775329589844
      2023-12-03T21:20:00Z 173.58775329589844
      2023-12-03T21:21:00Z 213.58775329589844
      2023-12-03T21:22:00Z 249.58775329589844
      2023-12-03T21:23:00Z 289.58775329589844
      2023-12-03T21:24:00Z 29.925498962402344
      2023-12-03T21:25:00Z 69.92549896240234
      ``` 
7. Stop the Java program
    * Press `Ctrl+C` to stop the program from the same terminal window where you ran the program.
8. Stop the infrastructure services
    * ```shell
      docker-compose down
      ```
    * I think it's important to do a proper `down` command so that the network is cleaned up. Otherwise, you might
      experience some weirdness if you change the Docker Compose file and then try to bring the services back up. Not
      really sure.


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE Scaffold the project by copy/pasting from the agent project, but configure it with the logging exporter
  because I need to walk before I can run.
* [x] DONE Export OTLP to Telegraf
   * DONE Darn, the Telegraf OTLP receiver doesn't support the HTTP endpoint for OTLP data, only the gRPC endpoint. I'm going
     to explore the OpenTelemetry Collector instead.
* [ ] Configure the metrics export to every 10 seconds instead of every 60 seconds.
* [ ] Do I need to set the otel "service"?
* [ ] Remove the auto-conf dependencies
* [ ] Do we need the semantic conventions dependency declaration? Isn't it already pulled in transitively? 
* [x] DONE (done but there's only one lonesome log?) Get JUL-to-SLF4J working. It's nice to be able to debug the OpenTelemetry instrumentation and it's also nice to use
  SLF4J because we like it. 
* [ ] Are we using the legacy metric conventions? We want the 1.0 semantic conventions and I think you actually need to
  opt in to that.
* [ ] Where is the Protobuf Java implementation shaded? Which of the OpenTelemetry dependencies brings it in? 


## Reference

* [OpenTelemetry docs: *Manual Instrumentation* for Java](https://opentelemetry.io/docs/instrumentation/java/manual/)
  * > Manual instrumentation is the act of adding observability code to an app yourself.
* [OpenTelemetry *JVM Runtime Metrics* library](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/runtime-telemetry/runtime-telemetry-java8/library)
* [OpenTelemetry *Collector*](https://opentelemetry.io/docs/collector/)
  * > Vendor-agnostic way to receive, process and export telemetry data.
