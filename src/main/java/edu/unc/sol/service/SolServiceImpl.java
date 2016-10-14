package edu.unc.sol.service;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.unc.sol.app.Optimization;
import edu.unc.sol.app.PathUpdateListener;
import edu.unc.sol.app.TrafficClass;
import edu.unc.sol.util.Config;
import org.apache.felix.scr.annotations.*;
import org.onlab.graph.Edge;
import org.onlab.graph.Vertex;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Component(immediate = true)
@Service
public class SolServiceImpl implements SolService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected Map<ApplicationId, List<TrafficClass>> tcMap;
    protected Map<ApplicationId, List<PathUpdateListener>> listenerMap;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;
    private Boolean running;
    private Client restClient;

    private class SolutionCalculator implements Runnable {
        @Override
        public void run() {
            log.debug("Recompute thread started");
            while (running) {
                // TODO: monitor changes to the traffic classes
                // Upon change, trigger recompute
//                recompute();
                // TODO: results of recompute should be sent to the apps
                // using the PathUpdateListener object
            }
            log.debug("Recompute thread ending");
        }
    }

    public SolServiceImpl() {
        // Initialize basic structures
        tcMap = new HashMap<>();
        listenerMap = new HashMap<>();
        running = false;
        restClient = ClientBuilder.newClient();
    }

    @Override
    public void registerApp(ApplicationId id, List<TrafficClass> trafficClasses, Optimization opt,
                            PathUpdateListener listener) {
        tcMap.put(id, trafficClasses);
        listenerMap.put(id, new LinkedList<>());
        listenerMap.get(id).add(listener);
    }

    @Override
    public void updateTrafficClasses(ApplicationId id, List<TrafficClass> trafficClasses) {
        tcMap.put(id, trafficClasses);
    }

    @Override
    public void addListener(ApplicationId id, PathUpdateListener listener) {
        // Adds a listener that awaits updated path results from the SOL instance
        listenerMap.get(id).add(listener);
    }

    @Override
    public void removeListener(ApplicationId id, PathUpdateListener listener) {
        // Removes a callback listener for a given app
        listenerMap.get(id).remove(listener);
        if (listenerMap.get(id).isEmpty()) {
            unregisterApp(id);
        }
    }

    @Override
    public void unregisterApp(ApplicationId id) {
        //Cleanup the app from the list of apps
        tcMap.remove(id);
        listenerMap.remove(id);
    }

    @Activate
    protected void activate() {
        running = true;
        // Get the address of the SOL server from an environment variable
        String solServer = System.getenv(Config.SOL_ENV_VAR);
        if (solServer != null) {
            // Build a proper url for the rest client
            StringBuilder builder = new StringBuilder();
            String url = builder.append("http://").append(solServer).append("/api/v1/").toString();
            // Send the topology to the SOL server
            sendTopology(url);
        } else {
            log.error("No SOL server configured");
        }
        // Start the monitor-solve loop in a new thread
        new Thread(new SolutionCalculator()).run();
        log.info("Started");
    }

    private void sendTopology(String url) {
        // Get a target from the rest client
        WebTarget target = restClient.target(url).path("/topology");
        // WARNING: we are running with the implicit assumption that that the topology does not change
        // Extract the topology from the topology service
        TopologyGraph topo = topologyService.getGraph(topologyService.currentTopology());
        // Now build our request
        Invocation.Builder builder = target.request(APPLICATION_JSON_TYPE);
        ObjectNode topoj = new ObjectNode(JsonNodeFactory.instance);
        topoj.putObject("graph");
        topoj.put("directed", true);
        ArrayNode nodes = topoj.putObject("nodes").putArray("items");
        ArrayNode links = topoj.putObject("links").putArray("items");
	Set<TopologyVertex> topology_vertexes = topo.getVertexes();
	Set<TopologyEdge> topology_edges = topo.getEdges();
	int num_vertexes = topology_vertexes.size();
	int num_edges = topology_edges.size();
	Vertex[] vertex_mapping = new Vertex[num_vertexes];
	Edge [] edge_mapping = new Edge[num_edges];
	int vertex_index = 0;
	int edge_index = 0;
        for (Vertex v : topology_vertexes) {
            ObjectNode node = nodes.addObject();
            //TODO: keep a vertex to int mapping and do something like this:
//            node.put("id", vertexid);
	    
	    //\/\\Chose to use an int array here because we are storing the vertex
	    //\/\\number so I figured, we would only need to look up one way -sanjay
	    vertex_mapping[vertex_index] = v; 
	    
	    node.put("id",Integer.toString(vertex_index));
            node.put("services", "switch");
            node.putObject("resources");
            // TODO: Put resources of nodes, if any, like CPU (skip for now)
	    vertex_index += 1;
        }
        for (Edge e : topology_edges) {
	    ObjectNode node = nodes.addObject();
            // TODO: similarly put edges
            // Every edge should have bandwidth ('bw') as resource

	    //\/\\Not sure if we may need a mapping for edges also later,
	    //\/\\Would this mapping better be indexed by two verticies
	    //\/\\rather than a unique id number? -sanjay

	    edge_mapping[edge_index] = e;

	    node.put("id",Integer.toString(edge_index));
	    //\/\\Need to check if 'resource' is the right field name
	    node.put("resource","bw");
	    //\/\\What else needs to be added for edges
	    edge_index += 1;
        }
        // Send request to the specified URL as a HTTP POST request.
        Response response = builder.post(Entity.json(topoj));
        if (response.getStatus() != 200) {
            log.error(response.getStatusInfo().toString());
        }
    }

    @Deactivate
    protected void deactivate() {
        // TODO: do any cleanup that is necessary
        log.info("Stopped");
    }

    private void recompute() {
        //TODO: Feed the data to SOL instance via REST(?) API
    }

}
