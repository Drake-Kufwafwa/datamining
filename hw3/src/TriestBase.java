import java.util.ArrayList;
import java.util.*;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TriestBase implements DataStreamAlgo {

    // initialize some variables
    private int sampSize;
    private int time = 0;
    private int estimate = 0;

    // graph representation: uses a vertex as key and a set of its neighbors as the value
    HashMap<Integer, HashSet<Integer>> graph;

    // stores each edge and its weight
    ArrayList<Edge> edges;
    double pi_t = 1.0;

	public TriestBase(int size) {
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
        double bias = (double) (this.sampSize - 1) / this.time;

        if(Math.random() <= bias){
            // selects randomly
            int index = ThreadLocalRandom.current().nextInt(0, this.sampSize - 1);
            Edge toRemove = edges.get(index);

            removeCount(toRemove.u,toRemove.v);

            // remove
            removeEdge(toRemove.u, toRemove.v);
            removeEdge(toRemove.v, toRemove.u);

            // replace
            edges.add(index,edge);
            int x = edge.u;
            int y = edge.v;

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

            addCount(x,y);
        }

        this.pi_t = ((double) (this.sampSize) / (this.time))
                * ((double) (this.sampSize - 1) / (this.time - 1))
                * ((double) (this.sampSize - 2) / (this.time - 2));
    }

    private void removeCount(int a, int b) {
        Set<Integer> intersection = new HashSet<Integer>(this.graph.get(a));
        intersection.retainAll(this.graph.get(b));
        this.estimate -= intersection.size();

    }

    private void removeEdge(int a, int b) {
        Set<Integer> current = this.graph.get(a);
        current.remove(b);
    }

    public int getEstimate() {
        return (int) (this.estimate / this.pi_t);
    }

}