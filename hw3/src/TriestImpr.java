import java.util.ArrayList;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TriestImpr implements DataStreamAlgo {

    // initialize some variables
    private int sampSize;
    private int time = 0;
    private int estimate = 0;
    double pi_t = 1.0;

    // graph representation: uses a vertex as key and a set of its neighbors as the value
    HashMap<Integer, HashSet<Integer>> graph;

    // stores each edge
    ArrayList<Edge> edges;

    public TriestImpr(int size) {
        this.sampSize = size;
        this.edges = new ArrayList<Edge>(sampSize);
        graph = new HashMap<Integer, HashSet<Integer>>();
    }

    public void handleEdge(Edge edge) {
        Integer x = edge.u;
        Integer y = edge.v;

        if (x.equals(y)) {
            return;
        }

        // add this edge to graph
        if(this.time <= this.sampSize){
            edges.add(edge);

            if(graph.containsKey(x)){
                graph.get(x).add(y);
            } else {
                HashSet<Integer> newE = new HashSet<>();
                newE.add(y);
                graph.put(x, newE);
            }

            if(graph.containsKey(y)){
                graph.get(y).add(x);
            } else {
                HashSet<Integer> newE1 = new HashSet<>();
                newE1.add(x);
                graph.put(y, newE1);
            }

            addCount(x,y);
        }
        else{
            replace(edge);
        }

        this.time++;

    }

    private void addCount(int a, int b) {
        Set<Integer> intersection = new HashSet<Integer>(this.graph.get(a));
        intersection.retainAll(this.graph.get(b));
        this.estimate += intersection.size();
    }

    public void replace(Edge edge){

        int x = edge.u;
        int y = edge.v;
        double bias = (double) (this.sampSize - 1) / this.time;
        // triangles count
        int numTriangles = trianglesCount(x, y);

        this.pi_t = numTriangles * ((double) (this.time - 1) / (this.sampSize - 1)) * ((double) (this.time - 2) / (this.sampSize - 2));

        this.estimate += this.pi_t;

        if(Math.random() <= bias){
            // selects randomly
            int index = ThreadLocalRandom.current().nextInt(0, this.sampSize - 1);
            Edge toRemove = edges.get(index);

            // remove
            removeEdge(toRemove.u, toRemove.v);
            removeEdge(toRemove.v, toRemove.u);

            // replace
            edges.add(index,edge);

            if(graph.containsKey(x)){
                graph.get(x).add(y);
            }
            else {
                HashSet<Integer> newE = new HashSet<>();
                newE.add(y);
                graph.put(x, newE);
            }

            if(graph.containsKey(y)){
                graph.get(y).add(x);
            }
            else {
                HashSet<Integer> newE1 = new HashSet<>();
                newE1.add(x);
                graph.put(y, newE1);
            }

        }
    }

    private int trianglesCount(int a, int b) {
        // find added triangles
        if(this.graph.get(a) == null|| this.graph.get(b) == null){
            return 0;
        }

        Set<Integer> intersection = new HashSet<Integer>(this.graph.get(a));
        intersection.retainAll(this.graph.get(b));
        return intersection.size();

    }

    private void removeEdge(int a, int b) {
        Set<Integer> current = this.graph.get(a);
        current.remove(b);
    }

    public int getEstimate() {
        return this.estimate;
    }

}