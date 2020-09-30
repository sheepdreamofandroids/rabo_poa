## Power of Attorney aggregator.

This is the Power-of-Attorney aggregator built using kotlin and kTor.

- Kotlin because it is a fast, concise, typesafe and null-safe language, essentially a better Java.
- kTor because it is small, can be configure completely in code, is magic-free and runs requests in coroutines.
  - Configuration can be factored out to a reusable library that knows the Rabo environment, 
  leaving nearly only business logic.
  - Running (incoming and outgoing) requests in coroutines means this server is very scalable
   because only a few threads, and therefore relatively little memory, are needed to fully use the CPU.
   
The server can be started using the net.bloemsma.guus.ApplicationKt.main() method. It runs on
http://localhost:8081
and with SSL on
https://localhost:8443 . Just accept the self-signed certificate when the browser complains.

https://start.ktor.io/ Has been used to generate a skeleton server and client for the given api.yaml.
That code has then been enhanced.

An arbitrary decision has been taken to simply leave out data that cannot be retrieved for whatever reason.

Coroutines are used to retrieve data in parallel from the json stub. I didn't add caching to make the difference versus
serial retrieval more pronounced.

Many more tests could have been written but these component tests are the most interesting.