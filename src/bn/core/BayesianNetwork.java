/*
 * File: BayesianNetwork.java
 * Creator: George Ferguson
 * Created: Sun Mar 25 13:51:22 2012
 * Time-stamp: <Thu Apr  4 13:26:14 EDT 2013 ferguson>
 */

package bn.core;

import bn.util.ArraySet;
import bn.util.Printable;
import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;

import javax.sound.midi.SysexMessage;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A BayesianNetwork is a network of nodes, each of which corresponds
 * to a random variable.
 * <p>
 * Implementation note: Some aspects of a Bayesian network are typically
 * described as Sets. We maintain the parents of a node as a list since
 * their order determines the way conditional probabilities are managed
 * in the CPT associated with the node. This could, of course, be done
 * otherwise. We maintain child pointers as well as parent pointers
 * simply to make printing easier (not that we bother with graph layout,
 * etc.).
 */
public class BayesianNetwork {

    /**
     * Inner class representing nodes in a BayesianNetwork. Each node
     * corresponds to a random variable, and has parent nodes, child
     * nodes, and a conditional probability table associated with it.
     */
    protected class Node implements Printable {

		public RandomVariable variable;
		public List<Node> parents;
		public Set<Node> children = new ArraySet<Node>();
		public CPT cpt;

		public Node(RandomVariable variable) {
			this.variable = variable;
		}

		// Printable

		/**
		 * Print this Node to the given PrintWriter.
		 */
		public void print(PrintWriter out) {
			print(out, 0);
		}

		/**
		 * Print this Node to the given PrintWriter at the given
		 * indent level.
		 */
		protected void print(PrintWriter out, int indent) {
			for (int i=0; i < indent; i++) {
			out.print(" ");
			}
			out.print("[");
			out.print(variable.toString());
			if (children != null && !children.isEmpty()) {
			out.println(":");
			for (Node child : children) {
				child.print(out, indent+2);
				out.println();
			}
			}
			for (int i=0; i < indent; i++) {
			out.print(" ");
			}
			out.print("]");
		}

		/**
		 * Print this Node to the given PrintStream.
		 */
		public void print(PrintStream out) {
			PrintWriter writer = new PrintWriter(out, true);
			print(writer);
			writer.flush();
		}

		/**
		 * Print this Node to System.out.
		 */
		public void print() {
			print(System.out);
		}

		/**
		 * Return the string representation of this Node.
		 */
		public String toString() {
			StringWriter writer = new StringWriter();
			PrintWriter out = new PrintWriter(writer);
			print(out);
			out.flush();
			return writer.toString();
		}

		public Object randomSampleGivenParents(Assignment parentAssignment) {

			Domain rvDomain = variable.getDomain();

//			Assignment parentAssignment = new Assignment();
//			for (Node parentNode : this.parents) {
//
//			}

			ArrayList<Double> conditionalDist = new ArrayList<>();
			//System.out.println("current rvDomain: " + rvDomain);

			for (Object possibleVal : rvDomain) {
				Assignment assignedValAssignment = parentAssignment.copy();
				assignedValAssignment.set(this.variable, possibleVal);
//				System.out.println("assignedValAssignment: " + assignedValAssignment);
//				System.out.println("current node: " + this.variable);
				conditionalDist.add(this.cpt.get(assignedValAssignment));
			}
			//System.out.println("conditionalDist: " + conditionalDist);

			double randomVal = Math.random();
			//System.out.println("randomVal: " + randomVal);
			double sum = 0;
			for (int i = 0; i < conditionalDist.size(); i++) {
				sum += conditionalDist.get(i);
				if (sum >= randomVal) {
					//System.out.println("returning " + rvDomain.get(i));
					return rvDomain.get(i);
				}
			}
			return null;
		}

    }

    /**
     * Construct and return a new (empty) BayesianNetwork.
     */
    public BayesianNetwork() {
    }


    public Assignment priorSample() {
    	//Node x = null;

    	// x = an event with n elements
		Assignment event = new Assignment();

    	// loops through variables in this bayes net
    	for (RandomVariable r : this.getVariableListTopologicallySorted()) {


    		Node n = this.getNodeForVariable(r);
    		// x[i] <- random sample from P(X_i | parents(X_i))
			List<Node> nodeParents = n.parents;
			Assignment parentAssignments = new Assignment();

			if (nodeParents == null) {
				nodeParents = new ArrayList<>();
			}




			for (Node parent : nodeParents) {
				if (event.get(parent.variable) == null) {
					System.out.println("-- ERROR ON NODE " + n + ": PARENT " + parent + " NOT IN event ASSIGNMENT LIST");
					System.out.println("event: " + event);
				}
				parentAssignments.set(parent.variable, event.get(parent.variable));
			}

			Object setVal = n.randomSampleGivenParents(parentAssignments);
			event.set(n.variable, setVal);
			//System.out.println("assigning var " + r + " to " + setVal);


		}
		//return x;
		return event;
	}

    /**
     * The Set of Nodes representing this BayesianNetwork.
     */
    protected ArraySet<Node> nodes = new ArraySet<>();

    /**
     * Add a node for the given RandomVariable to this BayesianNetwork.
     * This is called when a {@code variable} entity is read in an XMLBIF
     * file.
     */
    public void add(RandomVariable var) {
		nodes.add(new Node(var));
    }

    /**
     * Connect the node for the given RandomVariable to the nodes for
     * the given list of parent RandomVariables, with the given CPT.
     * This is called when a {@code definition} entity is read in an XMLBIF
     * file.
     */
    public void connect(RandomVariable var, List<RandomVariable> parents, CPT cpt) {
	Node node = getNodeForVariable(var);
	node.parents = new ArrayList<Node>(parents.size());
	for (RandomVariable pvar : parents) {
	    Node pnode = getNodeForVariable(pvar);
	    node.parents.add(pnode);
	    pnode.children.add(node);
	}
	node.cpt = cpt;
    }

    /**
     * Return the Node for given RandomVariable from this BayesianNetwork.
     * @throws NoSuchElementException
     */
    protected Node getNodeForVariable(RandomVariable var) {
	for (Node node : nodes) {
	    if (node.variable == var) {
		return node;
	    }
	}
//		System.out.println(var);
	throw new NoSuchElementException();
    }

    /**
     * Return the RandomVariable with the given name from this BayesianNetwork.
     * @throws NoSuchElementException
     * <p>
     * This is currently only used when reading a network from an XMLBIF
     * file. If you used it more heavily, you'd probably want to index
     * the variables more efficiently.
     */
    public RandomVariable getVariableByName(String name) {

    	for (Node node : nodes) {
			RandomVariable var = node.variable;
			if (var.getName().equals(name)) {

				return var;
			}
		}
		throw new NoSuchElementException();
    }

    /**
     * Return the size (number of nodes == number of RandomVariables) in
     * this BayesianNetwork.
     */
    public int size() {
		return nodes.size();
    }

    /**
     * Return the RandomVariables in this BayesianNetwork as a List.
     * Current implementation uses an ArrayList for this, and the order of
     * the variables will be the same as the order they were added
     * to the nextwork.
     */
    public List<RandomVariable> getVariableList() {
	List<RandomVariable> vars = new ArrayList<RandomVariable>(nodes.size());
	for (Node node : nodes) {
	    vars.add(node.variable);
	}
	return vars;
    }

    /**
     * Return the conditional probability that RandomVariable X has
     * the value assigned to it in the given Assignment, given that
     * its parents have the values assigned to them in the Assignment.
     */
    public double getProb(RandomVariable X, Assignment e) {
		trace("BayesianNetwork.getProb: for variable " + X + ", e=" + e);
		Node node = getNodeForVariable(X);
		double result = node.cpt.get(e);
		trace("BayesianNetwork.getProb: result=" + result);
		return result;
    }

    /**
     * Sort the given list of RandomVariables topologically with respect
     * to this BayesianNetwork, and return the resulting list.
     * I thought about just having a {@code topsort} method, into which
     * one would pass a list of RandomVariables (e.g., from
     *{@link BayesianNetwork#getVariableList}), but there was just too much
     * converting between RandomVariables and Nodes for that. The only
     * reason you ever topsort is to topsort the entire list of variables,
     * so why not just do that here.
     * <p>
     * This implementation uses the DFS algorithm described in Wikipedia
     * and attributed to Tarjan.
     */
    public List<RandomVariable> getVariableListTopologicallySorted() {
		// ``L <- Empty list that will contain the sorted nodes''
		List<RandomVariable> L = new ArrayList<RandomVariable>(nodes.size());
		// ``S <- Set of all nodes with no outgoing edges''
		Set<Node> S = new ArraySet<Node>(nodes.size());
		for (Node node : nodes) {
			if (node.children.isEmpty()) {
			S.add(node);
			}
		}
		// Can't mark nodes visited; instead keep as a set
		Set<Node> visited = new ArraySet<Node>(nodes.size());
		// ``for each node n in S do''
		for (Node n : S) {
			// ``visit(n)''
			visit(n, L, visited);
		}
		return L;
    }

    /**
     * Recursive step of topological sort procedure.
     */
    protected void visit(Node n, List<RandomVariable> L, Set<Node> visited) {
	// ``if n has not been visited yet''
	if (!visited.contains(n)) {
	    // ``mark n as visited''
	    visited.add(n);
	    // ``for each node m with an edge from m to n do''
	    for (Node m : nodes) {
		if (m.children.contains(n)) {
		    // ``visit(m)''
		    visit(m, L, visited);
		}
	    }
	    // ``add n to L''
	    L.add(n.variable);
	}
    }

    /**
     * Returns the Set of RandomVariables that are the children of
     * the given RandomVariable. This is done in a really wasteful
     * way, so you'd be better off doing something different for
     * use in a certain kind of sampling...
     */
    public Set<RandomVariable> getChildren(RandomVariable X) {
		trace("BayesinNetwork.getChildren: X=" + X);
		ArraySet<RandomVariable> children = new ArraySet<RandomVariable>();
		Node node = getNodeForVariable(X);
		for (Node childNode: node.children) {
			children.add(childNode.variable);
			trace("BayesinNetwork.getChildren: " + childNode.variable);
		}
		return children;
    }
	
    // Printable

    /**
     * Print this BayesianNetwork to the given PrintWriter.
     * Let's see. The right thing is to do a topological sort of the
     * network, which must be a DAG, then output the nodes in some
     * way that brings out the hierarchy. Or not.
     */
    public void print(PrintWriter out) {
		for (Node node : nodes) {
			node.variable.print(out);
			out.print(" <- ");
			if (node.parents != null) {
				for (Node pnode : node.parents) {
					pnode.variable.print(out);
					out.print(" ");
				}
			}

			out.println();
			if (node.cpt != null) {
				// Might not want this if it clutters things up...
				node.cpt.print(out);
			}
		}
    }

    /**
     * Print this BayesianNetwork to the given PrintStream.
     */
    public void print(PrintStream out) {
	PrintWriter writer = new PrintWriter(out, true);
	print(writer);
	writer.flush();
    }

    /**
     * Print this BayesianNetwork to System.out.
     */
    public void print() {
		print(System.out);
    }

    /**
     * Return the string representation of this BayesianNetwork.
     */
    public String toString() {
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		print(out);
		out.flush();
		return writer.toString();
    }

    // Testing

    protected void trace(String msg) {
	//System.err.println(msg);
    }

    public void createDefault() {
		RandomVariable A = new RandomVariable("A");
		A.setDomain(new Domain("a1", "a2"));
		RandomVariable B = new RandomVariable("B");
		B.setDomain(new Domain("b1", "b2"));
		RandomVariable C = new RandomVariable("C");
		C.setDomain(new Domain("c1", "c2"));
		List<RandomVariable> givens = new ArrayList<RandomVariable>(2);
		givens.add(B);
		givens.add(C);
		CPT cpt = new CPT(A, givens);
		this.add(A);
		this.add(B);
		this.add(C);
		this.connect(A, givens, cpt);
		//network.print(System.out);

		System.out.println("nodes: " + this.nodes);

		Node ANode = this.getNodeForVariable(A);
		for (int i = 0; i < 2; i++) {
			for (int j = 0; j < 2; j++) {
				double a1p = Math.random();
				for (int k = 0; k < 2; k++) {
					Assignment a = new Assignment();
					a.set(B, B.domain.get(i));
					a.set(C, C.domain.get(j));
					a.set(A, A.domain.get(k));

					ANode.cpt.set(a, Math.random());
					if (k == 0) ANode.cpt.set(a, a1p);
					else ANode.cpt.set(a, 1-a1p);
				}
			}
		}

		Assignment b1ass = new Assignment();
		b1ass.set(B, B.domain.get(0));
		Assignment b2ass = new Assignment();
		b2ass.set(B, B.domain.get(1));
		this.getNodeForVariable(B).cpt = new CPT(B, new ArrayList<>());
		double b1assp = Math.random();
		this.getNodeForVariable(B).cpt.set(b1ass, b1assp);
		this.getNodeForVariable(B).cpt.set(b2ass, 1-b1assp);

		Assignment c1ass = new Assignment();
		c1ass.set(C, C.domain.get(0));
		Assignment c2ass = new Assignment();
		c2ass.set(C, C.domain.get(1));
		this.getNodeForVariable(C).cpt = new CPT(C, new ArrayList<>());
		double c1assp = Math.random();
		this.getNodeForVariable(C).cpt.set(c1ass, c1assp);
		this.getNodeForVariable(C).cpt.set(c2ass, 1-c1assp);
		System.out.println("nodes: " + this.nodes);
	}

    /**
     * Test driver for BayesianNetwork.
     */
    public static void main(String[] argv) {


//		network.print(System.out);
//
////		try {
////			TimeUnit.SECONDS.sleep(1);
////
////		} catch (InterruptedException e) {
////
////		}
//
//		ArrayList<Assignment> a = new ArrayList<>();
//		for (int i = 0; i < 100; i++) {
//			a.add(network.priorSample());
//		}
//		System.out.println(a);


//		Assignment parentAssignment = new Assignment();
//		parentAssignment.set(B, B.domain.get(0));
//		parentAssignment.set(C, C.domain.get(1));
//
//		Assignment a1ass = parentAssignment.copy();
//		a1ass.set(A, A.domain.get(0));
//		Assignment a2ass = parentAssignment.copy();
//		a2ass.set(A, A.domain.get(1));
//
//		Node ANode = network.getNodeForVariable(A);
//		ANode.cpt.set(a1ass, 0.2);
//		ANode.cpt.set(a2ass, 0.8);
//
//		int a1counter = 0;
//		int a2counter = 0;
//
//		for (int i = 0; i < 100; i++) {
//			Object randomAss = ANode.randomSampleGivenParents(parentAssignment);
//			System.out.println(randomAss);
//			if ((String)randomAss == "a1") a1counter++;
//			else a2counter++;
//		}
//
//		System.out.println("a1 percentage: " + (double)a1counter / (a1counter+a2counter));
    }
}
