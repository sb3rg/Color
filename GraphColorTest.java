import java.util.Iterator;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class GraphColorTest {
    ////////////////////////////////////////////////////////////////////////
    // BEGIN TEST CLIENT                                                  //
    ////////////////////////////////////////////////////////////////////////
    public static void main( String...args ) {
	GraphColorTest test = new GraphColorTest();
	test.run( args );
    }

    // test runner
    public void run( String...args ) {
	Path path = Path.of( args[0] );
	try (Stream<String> lines = Files.lines( path )) {
	    Graph g = new Graph( lines.iterator() );
	    // System.out.println( g );
	    // System.out.println( degree( g, 1));
	    // System.out.println( max_degree( g ));
	    // System.out.println( Arrays.toString( g.degrees() ));
	    // System.out.println( "max node_id: " + g.max_degree().x +
	    // 			"\nmax node_val: " + g.max_degree().y );
	    ColorSearch s = new ColorSearch( g );
	    s.print_results();
	} catch ( IOException e ) {
	    System.out.println( "failed to read file: " + e );
	}
    }
    ////////////////////////////////////////////////////////////////////////
    // END TEST CLIENT                                                    //
    ////////////////////////////////////////////////////////////////////////


    
    ////////////////////////////////////////////////////////////////////////
    // BEGIN IMPLEMENTATION                                               //
    ////////////////////////////////////////////////////////////////////////

    // --------------------------------------------------
    // Search for Chromatic Number
    // --------------------------------------------------
    public class ColorSearch {
	private List<Integer> degrees;
	private Pair<Integer, Integer> max_vertex;
	private List<Vertex> rem;
	private List<List<Vertex>> colors_queue;
	private Vertex[] results;
	private Graph g;

	public ColorSearch( Graph g ) {
	    this.g = g;
	    int n = g.V();
	    // get degrees from graph
	    degrees = IntStream.of( g.degrees() ) 
		.boxed()
		.collect( Collectors.toList() );
	    // get Tuple of max degree vertex
	    max_vertex = g.max_degree();

	    // initialize and populate queue by priority of degree (descending order)
	    PriorityQueue<Vertex> tmp = new PriorityQueue<>( Collections.reverseOrder() );
	    for( int i = 0; i < degrees.size(); i++ ) {
		tmp.add( new Vertex( i, degrees.get(i) ));
	    }

	    // convert to a List for convenience
	    rem = new ArrayList<>();
	    while( !tmp.isEmpty() ) {
		rem.add( tmp.poll() );
	    }

	    // initialize nested list of results indexed by color
	    colors_queue =  new ArrayList<>();
	    // initialize results indexed by node id (for printing results)
	    results = new Vertex[n];

	    // start by searching highest degree vertex first
	    search( max_vertex.x );
	}

	public void search( int s ) {
	    // System.out.println( "### --- START: chromatic number calculation --- ###");
	    int colorCount = 0;

	    // recursively apply colors until all vertices have been assigned
	    while( !rem.isEmpty() ) {
		apply_color( colors_queue, colorCount );
		// if you reach this point, you've exhausted the graph of
		// all eligible vertices for this color, so we increment
		colorCount++;
	    }
	    // System.out.println( "### --- END: chromatic number calculation --- ###");
	}

	private void apply_color( List<List<Vertex>> colors_queue, int color ) {
	    colors_queue.add( new ArrayList<>() );
	    List<Vertex> queue = colors_queue.get( color );
	    int n = 0;
	    
	    while( n < rem.size() ){
		// get the next highest degree from priority
		Vertex w = rem.get( n );
		boolean is_adj_to_curr_color = false;
		// check its adjacencies
		for( int i : g.adjacent( w.get_id() )) {  
		    for( Vertex j : queue )
			if( i == j.get_id() )
			    is_adj_to_curr_color = true;
		}
		// if it's adjacent to a vertex of the current color
		if( is_adj_to_curr_color ) { // skip it
		    n++;
		    continue;
		} else {	// otherwise remove, set color and enqueue
		    Vertex k = rem.remove(n);
		    w.set_color( color );
		    queue.add( w );
		    n = 0; 	// reset vertex cursor to point to head

		    // convenience for printing
		    results[k.get_id()] = k;
		}
	    }
	    // --- DEBUGGING ---
	    // System.out.println( "Color Count: " + color );
	    // inspect_q( colors_queue.get( color ) );
	    // inspect_q( rem );
	}

	public List<List<Integer>> BronKerbosch( List<List<Integer>> q,
						 List<Integer> r,
						 List<Integer> p,
						 List<Integer> x ) {
	    // --- DEBUG ---
	    // System.out.println();
	    // System.out.println( "Q: " + q );
	    // System.out.println( "R: " + r );
	    // System.out.println( "P: " + p );
	    // System.out.println( "X: " + x );
	    // System.out.println();
	    if( p.isEmpty() && x.isEmpty() ) {
		// necessary to clone r vs pass by reference
		q.add( new ArrayList<>(r) ); // THIS!!! ah! Java... sigh...
		return q;
	    }

	    List<Integer> p1 = new ArrayList<>( p );
	    
	    for( Integer v : p ) {
		// find neighbors of v
		Iterable<Integer> neighbors = g.adjacent( v );
		// calculate the set intersects of p|x and Neighbors(v)
		List<Integer> new_p = intersect( p1, neighbors );
		List<Integer> new_x = intersect( x, neighbors );
		r.add( v );	// union of r with {v}

		// --- DEBUG ---
		// System.out.println( "Vertex: " + v );
		// System.out.format( "  intersect P, N(v=%d): " + new_p + "\n", v);
		// System.out.format( "  intersect X, N(v=%d): " + new_x + "\n", v);
		// System.out.println( "inner R: " + r );
		// System.out.println( "inner P: " + new_p );
		// System.out.println( "inner X: " + new_x );
		//

		q = BronKerbosch( q, r, new_p, new_x );
		// System.out.println( "inner Q: " + q );
		
		r.remove( v );
		p1.remove( v );
		x.add( v );

		// System.out.println( "after R: " + r );
		// System.out.println( "after P: " + new_p );
		// System.out.println( "after X: " + new_x );
		// System.out.println();
	    }
	    return q;
	}

	public Integer get_max_clique() {
	    // System.out.println( "### --- START: max cliques calculation --- ###");
	    List<Vertex> idList = Arrays.asList( results ); // convenience
	    // use Bron-Kerbosch algorithm without pivots to determine maximal cliques
	    List<List<Integer>> cliques =
		BronKerbosch(
			     new ArrayList<>(),
			     new ArrayList<>(),
			     idList.stream().map(Vertex::get_id)
			     .collect(Collectors.toList()),
			     new ArrayList<>() );

	    List<Integer> sizes = cliques
		.stream()
		.map((List l) -> l.size())
		.collect(Collectors.toList());

	    // System.out.println( "### --- END: max cliques calculation --- ###");
	    return Collections.max( sizes );
	}
				      

	private List<Integer> intersect( List<Integer> px,
					 Iterable<Integer> neighbors ){
	    List<Integer> new_px = new ArrayList<>();
	    for( int n : neighbors ) {
		if( px.contains( n )) new_px.add( n );
		// System.out.println( "  px: " + px + ", neighbor: " + n );
	    }
	    return new_px;
	}

	public void print_results() {
	    // number of colors used
	    System.out.print( colors_queue.size() + " ");
	    // predicate for optimality
	    System.out.println( get_max_clique() == colors_queue.size() ? 1 : 0 );
	    // solution encoding ordered by vertex id
	    for( int i = 0; i < results.length; i++ )
		System.out.print( results[i].get_color() + " ");
	    System.out.println();
	}

	@SuppressWarnings("unchecked")
	private void inspect_q( Collection pq ) {
	    Iterator<Vertex> it = pq.iterator();
	    while( it.hasNext() ) System.out.println( it.next() );
	    System.out.println( "\n" );
	}

    }
    
    // --------------------------------------------------
    // Simple Graph Implementation
    // --------------------------------------------------
    public class Graph {
	// private Bag<Integer>[] adjacency_list;
	private Bag<Integer>[] adjacency_list;
	private int[] degrees;	// track degree of each node
	private Pair<Integer, Integer>  max_degree; // track max degree of graph
	private final int v;
	private int e;

	// Constructors
	@SuppressWarnings("unchecked")
	public Graph( Edge<Integer, Integer> first_pair) {
	    this.v = first_pair.v;
	    this.e = first_pair.w;
	    this.max_degree = new Pair<>( 0, 0 );
	    
	    // adjacency_list = (Bag<Integer>[]) new Bag[v]; // create array of lists;
	    adjacency_list = (Bag<Integer>[]) new Bag[v]; // create array of lists;
	    degrees = new int[v];			  // create degrees array
	    for( int i = 0; i < v; i++ ){		  // initialize all collections to empty
		adjacency_list[i] = new Bag<Integer>();
		degrees[i] = 0;
	    }
	}

	
	public Graph( Iterator<String> iterator ) {
	    this( Edge.parse_line( iterator.next() ));
	    // System.out.println( "### --- START: reading and building graph --- ###");
	    while( iterator.hasNext() ) {
		Edge<Integer, Integer> edge = Edge.parse_line( iterator.next() );
		add_edge( edge );
	    }
	    // System.out.println( "### --- END: reading and building graph --- ###");
	}

	public int V() { return v; }
	public int E() { return e; }

	public void add_edge( Edge<Integer, Integer> edge ) {
	    int v = edge.v;
	    int w = edge.w;
	    adjacency_list[v].add( w );
	    adjacency_list[w].add( v );
	    degrees[v]++;	// increment degrees on add
	    degrees[w]++;
	    e++;
	    // update max_degree
	    max_degree = update_max_degree( v, w, degrees[v], degrees[w] );
	}

	public Iterable<Integer> adjacent( int v ) { return adjacency_list[v]; }
	public int degree( int v ) { return degrees[v]; }
	public int[] degrees() { return degrees; }
	public Pair<Integer, Integer> max_degree() { return max_degree; }

	public String toString() {
	    String s = v + " vertices, " + e + " edges\n";
	    for( int i = 0; i < v; i++ ){
		s += i + ": ";
		for( int w : this.adjacent( i )) s += w + " ";
		s += "\n";
	    }
	    return s;
	}

	// helper function to update max degree
	private Pair<Integer, Integer> update_max_degree( int v, int w, int val_v, int val_w ) {
	    int max_v_id = max_degree.x;
	    int max_val = max_degree.y;
	    // compare largest, default to current max if equal
	    if( max_degree.y >= val_w && max_degree.y >= val_v ) {}
	    else {
		// compare to largest 
		if( val_v >= val_w && val_v > max_degree.y ) {
		    max_v_id = v;
		    max_val = val_v;
		}
		if( val_w > val_v && val_w > max_degree.y ) {
		    max_v_id = w;
		    max_val = val_w;
		}
	    }
	    
	    return new Pair<>( max_v_id, max_val );
	}
    }

    // --------------------------------------------------
    // Vertex
    // --------------------------------------------------
    public class Vertex implements Comparable<Vertex> {
	private final int id;
	private final int degree;
	private int color;

	public Vertex( int id, int degree ) {
	    this.id = id;
	    this.degree = degree;
	}
	
	// public set_degree( int d ) { degree = d; }
	public void set_color( int c ) { color = c; }

	public int get_id() { return id; }
	public Integer get_degree() { return degree; }
	public int get_color() { return color; }

	@Override
	public int compareTo( Vertex v ) {
	    return this.get_degree().compareTo( v.get_degree() );
	}
	
	public String toString() {
	    String s = "id: " + id + ", degree: " + degree + ", color: " + color;
	    return s;
	}
    }

    // --------------------------------------------------
    // Tuple
    // --------------------------------------------------
    public static class Pair<X,Y> {
	public final X x;
	public final Y y;
	public Pair( X x, Y y ) {
	    this.x = x;
	    this.y = y;
	}
    }
    
    // --------------------------------------------------
    // Edges
    // --------------------------------------------------
    public static class Edge<V,W> {
	public final V v;
	public final W w;
	public Edge( V v, W w ) {
	    this.v = v;
	    this.w = w;
	}

	// helper function to parse String lines from input
	private static Edge<Integer, Integer> parse_line( String line ) {
	    String[] tokens = line.split(" ");
	    int v_one = Integer.valueOf( tokens[0] );
	    int v_two = Integer.valueOf( tokens[1] );
	    
	    return new Edge<>( v_one, v_two );
	}
    }

    // --------------------------------------------------
    // Simple LinkedList Bag Implementation
    // --------------------------------------------------
    public class Bag<Item> implements Iterable<Item> {
	private Node first;
	private class Node {
	    Item item;
	    Node next;
	}
	public void add( Item item ) {
	    Node oldfirst = first;
	    first = new Node();
	    first.item = item;
	    first.next = oldfirst;
	}
	public Iterator<Item> iterator(){ return new ListIterator(); }
	private class ListIterator implements Iterator<Item> {
	    private Node current = first;
	    public boolean hasNext() { return current != null; }
	    public void remove() {}
	    public Item next() {
		Item item = current.item;
		current = current.next;
		return item;
	    }
	}
    }
    ////////////////////////////////////////////////////////////////////////
    // END IMPLEMENTATION                                                 //
    ////////////////////////////////////////////////////////////////////////
}
