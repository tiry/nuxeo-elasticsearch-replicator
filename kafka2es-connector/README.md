# About 

This module provide a Kafka Connector that uses elasticsearch as a sink.

Unlike other available connector this Connector handle:

 - INDEX/UPDATE/DELETE
 - Index configuration and mapping

The idea is to be able to replicate the ES con figuration changes that are driven by the application so that the newly indexed document will benefit from the mapping change even if the standby servers have not been started (meaning the ES mapping can not have been deployed by the Nuxeo servers). 

# Status

Basic implementation: should be ready for a real life test.

# Requirements

This module requires Java 8 and Maven 3.

# Building

Run the Maven build:

   mvn clean install

# Configuration

**Configure Kafka Connect **

Kafka connect needed to be configured so that the nuxeo plugin will be found: this means you need to provide a value for `plugin.path`, typically in the defaul configuration file (`config/connect-standalone.properties`).

However, because Kafka is fed via `nuxeo-stream` , we can not use the standard converters and marshaller.

You can directly use the configuration file provided in `kafka2es-connector/connect-standandalone-nuxeo.properties` (and edit the `plugin.path` accordingly).

**Deploy the  Nuxeo Kafka Connect Plugin**

Use the Uber jar locared in `target/nuxeo-kafka2es-connect-1.0.0-SNAPSHOT-jar-with-dependencies.jar`

**Copy and edit the connector configuration**

Copy and edit the configuration file located in `kafka2es-connector/connect-es-sink.properties`;

You want to edit the 2 parameters to locate your target ES cluster:

    es.host=127.0.0.1
    es.port=9201

# Starting the connector

    bin/connect-standalone.sh config/connect-standalone-nuxeo.properties config/connect-es-sink.properties
    
# Licensing
 
This module is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (http://www.gnu.org/licenses/lgpl-2.1.html).
 
# About Nuxeo
 
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).