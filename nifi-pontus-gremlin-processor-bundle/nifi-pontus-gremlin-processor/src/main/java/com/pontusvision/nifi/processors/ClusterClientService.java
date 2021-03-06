package com.pontusvision.nifi.processors;

import org.apache.tinkerpop.gremlin.driver.Client;
import org.apache.tinkerpop.gremlin.driver.Cluster;

import java.io.FileNotFoundException;
import java.net.URISyntaxException;

public interface ClusterClientService {

  boolean isClosed();

  Cluster getCluster ();

  Client getClient () throws URISyntaxException, FileNotFoundException;

  Client createClient();

  void close(String event);
}
