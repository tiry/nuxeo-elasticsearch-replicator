# About 

The goal is to build a pipeline to replicate elasticsearch indexes between 2 remote Nuxeo instances.

The pipe uses Kafka as vehicle to transmit operations between the source and the destination Nuxeo instance.

This pipe is composed of 2 parts:

 - a part that logs Nuxeo ES related operation inside Kafka
 	- this part is a Nuxeo Plugin
 - a part that moves data from Kafka to ES
 	- this part if a custom kafka connect 

## Why not simply using LogStash

As a first approach we could consider using LogStash

    ES1 --LogStach--> Kafka DC1 --MirrorMaker--> Kafka DC2 --LogStach--> ES2

Creating a LogStash pipe with Elasticsearch and Kafka is rather simple since ES and Kafka are both standard Input/Output plugins.

However, the ES input plugin has a major drawback: it is configured using an ES query and simply pipes all the JSON documents into the output plugin.

As a result, this will work for an initial replication, but then since I did not find a "tail mode", we would have to re-rerun periodically the LogStash pipe with a query that match only the recently added/updated entries.

While technically we could have a script that runs LogStash in a loop changing the query at each iteration, my fear is that:

 - having a reliable offset may be tricky
 - the cost on the ES side may end up being significant
 - this will not work for deletion

## Big picture

<img src="https://www.lucidchart.com/publicSegments/view/2649d206-b4c6-427d-873a-3c29a6f23624/image.png" width="700px"></img>

# Licensing
 
This module is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (http://www.gnu.org/licenses/lgpl-2.1.html).
 
# About Nuxeo
 
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).