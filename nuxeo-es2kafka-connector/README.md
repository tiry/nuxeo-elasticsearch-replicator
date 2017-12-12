# About Nuxeo ES Replicator

The goal of this module is to provide a way to replicate the `elsaticsearch` index between 2 Nuxeo clusters deployed on 2 datacenters.

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

## Using Kafka Connect

Looking at the [connector page](https://www.confluent.io/product/connectors/), it seems that the 3 linked opensource connectors are actually doing `Kafka => ES` and not the reverse.

This means we are still missing the first part of our pipe.

## Send App level OpLog in nuxeo-stream

This module allows to queue indexing command using `nuxeo-streams` so that the flow of indexing commands can be replicated in order to keep an off-site Nuxeo + Index up to date.

Until we update the ES indexing inside the platform to go through nuxeo-streams as it is done for the audit trail, the idea in this addon is to do the work as the elasticsearch client level.

Thia addon contribute a custom client for elasticsearch that in addition of sending the command to the local ES cluster also stored the commands in a log using nuxeo-stream.

                                  ===> ES Cluster (local)
    Nuxeo => ESDuplicatorClient ==
                                  ===> Kafka ===> LogStash/Connector => ES Cluster (remote)

NB: the remote part if more likely to be a Kafka Connector that a simple LogStash.

# Status

 - initial implementation of the ES => stream 
 
# Configuration

## nuxeo.conf

You need to unable Kafka integration.

    kafka.enabled=true

# Requirements

This module requires Java 8 and Maven 3.

# Building
 

# Licensing
 
This module is licensed under the GNU Lesser General Public License (LGPL) version 2.1 (http://www.gnu.org/licenses/lgpl-2.1.html).
 
# About Nuxeo
 
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with
SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Netflix, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris.
More information is available at [www.nuxeo.com](http://www.nuxeo.com).