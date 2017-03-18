# bigpanda

## Prerequisites 

* sbt 0.13
* Executable generator-linux-amd64 (or relevant binary for your OS)

## Building

In cloned repo run `sbt complie`

## Running

* Have the generator binary in your working directory
* Run `sbt run`
* Hit Enter to stop running

## Quering

* Event type statistics: `curl http://localhost:8080/event-type`
* Event type count for specific event: `curl http://localhost:8080/event-type/<event_id>`
* Data statistics: `curl http://localhost:8080/data`
* Data count for specific data: `curl http://localhost:8080/data/<data>`
