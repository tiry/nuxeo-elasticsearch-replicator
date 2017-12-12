# About 

# Status

 - initial implementation of the ES => stream  


# Requirements

This module requires Java 8 and Maven 3.

# Building

Run the Maven build:

   mvn clean insta;;

# Configuration

**Define the Kafka Connect plugin**

Edit `config/connect-standalone.properties` (or the config you use to start Kafka Connect) and give a value to `plugin.path` 

**Copy the Uber jar inside the plugin directory**

Use the jar locared in `target/nuxeo-kafka2es-connect-1.0.0-SNAPSHOT-jar-with-dependencies.jar`

**Copy and edit the connector configuration**

Copy and edit the configuration file located in `kafka2es-connector/connect-es-sink.properties`;

Because we use nuxei-stream format and not standard JSON, you will also need to copy

`kafka2es-connector/connect-standandalone-nuxeo.properties`.

# Starting the connector

    bin/connect-standalone.sh config/connect-standalone-nuxeo.properties config/connect-es-sink.properties
    
# Licensing
 
This module is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (http://www.gnu.org/licenses/lgpl-2.1.html).
 
# About Nuxeo
 
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).