# Let's do events

## Intro

letsdo.events is a lightweight, collaborative platform to organize events.
Company-internal tech talks, regular meetups, activities around a conference - with letsdo.events everyone can suggest a topic, others can show their interest and sign up.


## Technical overview

The application is built around topics and events belonging to a topic.
Topics can be public or invite-only.

The application is designed in such a way that additionally to using the version available at https://letsdo.events it is also simple to run it **on your own server**.
All you need is a **JVM** and a **single jar file**.
No external database is needed, all data is stored in a local file directory using [Crux](https://juxt.pro/crux/index.html) as database and [RocksDB](https://rocksdb.org/) as storage.

Authentication can be done in the classical way with email + password but also via email-only.
**Email-only authentication** works via login links that contain JWT tokens.

An SMTP configuration must to be provided to enable login and invite emails.

Optionally [Honeycomb](https://www.honeycomb.io/) can be configured to get observability into the operation of the application.


## Running the application

Run the application directly with `clojure -m lde.main`


## Development

For development run `clojure -Adev`

This makes sure the web server rebuilds all routing functions with every request
so you can make overwrite functions while the application is running.

There is also a helper to reload all open browser sessions directly from your REPL/editor:

```
(do (in-ns 'dev.reload) (reload-browser))
```

The function uses a web socket connect to tell the browsers to reload.


## Building the application

Pack the application into a single jar file with `clojure -Auberjar`

Start the application with `java -cp target/lde.jar clojure.main -m lde.main`


## Configuration

No configuration is required but most likely you like to make some adjustments.
To overwrite defaults you can pass the path to a EDN file as first argument when running `lde.main`.
For available options please have a look at the [config namespace](./src/lde/config.clj).


## License

[MIT](./license.txt)
