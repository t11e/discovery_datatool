The Discovery Data Tool targets Java 1.6 and thus requires a slightly older tool chain to build and thus
ensure maximum compatibility.

The following instructions all assume you are on a Mac.

Install the latest release of Java 6u45, this is supplied by Apple (Java 7 and higher are supplied by Oracle).
You can find it [here](https://support.apple.com/downloads/java%25206) or [here](https://support.apple.com/kb/DL1572?viewlocale=en_GB&locale=en_GB).

Install [Homebrew](https://brew.sh).

Install version 1.9 of ant (newer versions are no longer compatible with Java 6):

    $ brew install ant@1.9

Configure your shell to use the correct versions of Java and ant for development.

    $ export JAVA_HOME=$(/usr/libexec/java_home -v 1.6)
    $ alias ant19="/usr/local/Cellar/ant@1.9/1.9.8/bin/ant"

Run the tests:

    $ ant19 clean test

To run a local server you will need to create `discovery_datatool.xml` in the root of this project and then execute:

    $ ant19 server

This will listen on port 8089. If you want to test the SSL support, create a
[Java keystore](http://docs.discoverysearchengine.com/release/4.1/data_integration/datatool.html#create-the-rsa-key-certificate-and-java-key-store)
and uncomment the second handler in `src/test/webapp/WEB-INF/jetty.xml`.

To build a release:

    $ ant19 -Dexternal.version=1.13-SNAPSHOT -Dskip_tests clean release

The project comes configured for development in Eclipse so you can just import it straight into your workspace.

To run or debug the application directly from Eclipse, create a new `Run Configuration` with the following settings:

* Type: `Java Application`
* Main class: `org.mortbay.xml.XmlConfiguration`
* Program Arguments: `src/test/webapp/WEB-INF/jetty.xml`
* JVM Arguments: `-showversion -Djava.util.logging.config.file=src/test/resources/logging.properties -Djetty.port=8089`
