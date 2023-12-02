# open-telemetry-playground

📚 Learning and exploring OpenTelemetry via the official Java libraries.

> High-quality, ubiquitous, and portable telemetry to enable effective observability
>
> -- <cite>https://opentelemetry.io/ </cite>


## Overview

**NOTE**: This project was developed on macOS. It is for my own personal use.

OpenTelemetry has become a big deal in the observability space. It's an incubating project in the CNCF. It has a lot of
integrations and adoption. This repository is me learning OpenTelemetry.


## Standalone subprojects

This repository illustrates different concepts, patterns and examples via standalone subprojects. Each subproject is
completely independent of the others and do not depend on the root project. This _standalone subproject constraint_
forces the subprojects to be complete and maximizes the reader's chances of successfully running, understanding, and
re-using the code.

The subprojects include:


### `agent/`

An example program-under-observation instrumented with the OpenTelemetry Java agent.

See the README in [agent/](agent/).


## Wish List

General clean-ups, TODOs and things I wish to implement for this project:

* [x] DONE Extract the original project into an `agent` subproject.
* [ ] Create a `manual-instrumentation` subproject. This will contrast with the `agent` subproject. Particularly, I want
  a more explicit set of transitive dependencies. I want to use OpenTelemetry's [`JdkHttpSender`](https://github.com/open-telemetry/opentelemetry-java/blob/f1deb8ec78cd446bc6310b1528a5d71e1d42989e/exporters/sender/jdk/src/main/java/io/opentelemetry/exporter/sender/jdk/internal/JdkHttpSender.java)
  instead of the OkHttp one because I want to bring in fewer dependencies like OkHttp (which itself brings in Kotlin).
