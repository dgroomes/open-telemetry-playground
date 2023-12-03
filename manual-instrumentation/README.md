# manual-instrumentation

An example program-under-observation instrumented with OpenTelemetry using *manual instrumentation*.


## Overview

In some cases, you may use the OpenTelemetry Java _agent_ to instrument your program because it's powerful and requires
no changes to the program's source code. OpenTelemetry refers to this style of instrumentation as [*Automatic Instrumentation*](https://opentelemetry.io/docs/instrumentation/java/automatic/).
This is especially useful for third-party programs where you don't have access to the source code. For your own software
projects, you may want to exercise more precise control over the exact dependencies, configuration, and behavior of the
OpenTelemetry instrumentation. In this project, we instrument an example program the manual way. Refer to the OpenTelemetry
docs on [*Manual Instrumentation* for Java](https://opentelemetry.io/docs/instrumentation/java/manual/).

The tech stack in this subproject:

* A program-under-observation
  * This is a fictional "data processing" program written in Java. This program is instrumented manually with the OpenTelemetry
    Java instrumentation libraries.
* A metrics sink/collector (Telegraf)
  * Telegraf acts as a sink for the metrics pushed by the OpenTelemetry exporter. Telegraf re-formats the metrics into an
    acceptable format for the metrics database and then writes the metrics into the database. 
* A metrics database (InfluxDB)
  * InfluxDB is an open source time series database that's usually used for metrics. 


## Instructions

Follow these instructions to build and run the example system.

1. Pre-requisites: Java and Docker
    * I used Java 17.
2. Start infrastructure services
    * ```shell
      docker-compose up
      ```
    * This starts Telegraf and InfluxDB.
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
    * The program will run indefinitely and continuously submit OTLP-based metrics data to the Telegraf server.
5. Wait two minutes
    * This is important! There is a natural delay throughout the system. The OpenTelemetry instrumentation only collects
      new values at some interval (I can't find docs on this), and it only submits the data to Telegraf at some interval
      (I can't find docs on this). Telegraf only submits data to InfluxDB at some interval. We have to wait until the
      first batch of data is submitted to InfluxDB before we can inspect it.
6. Inspect the metrics in InfluxDB directly
    * Not yet implemented. 
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
* [ ] Configure the metrics export to every 10 seconds instead of every 60 seconds.
* [ ] Do I need to set the otel "service"?
* [x] DONE (done but there's only one lonesome log?) Get JUL-to-SLF4J working. It's nice to be able to debug the OpenTelemetry instrumentation and it's also nice to use
  SLF4J because we like it. 


## Reference

* [OpenTelemetry docs: *Manual Instrumentation* for Java](https://opentelemetry.io/docs/instrumentation/java/manual/)
  > Manual instrumentation is the act of adding observability code to an app yourself.
* [OpenTelemetry *JVM Runtime Metrics* library](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation/runtime-telemetry/runtime-telemetry-java8/library)
