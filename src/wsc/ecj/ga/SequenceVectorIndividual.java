package wsc.ecj.ga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ec.EvolutionState;
import ec.simple.SimpleFitness;
import ec.util.Parameter;
import ec.vector.VectorIndividual;
import wsc.InitialWSCPool;
import wsc.data.pool.Service;
import wsc.dynamic.localsearch.LocalSearch;
import wsc.graph.ServiceGraph;

public class SequenceVectorIndividual extends VectorIndividual {

	private static final long serialVersionUID = 1L;

	private double availability;
	private double reliability;
	private double time;
	private double cost;
	private double matchingType;
	private double semanticDistance;

	private String strRepresentation; // a string of graph-based representation
	private double fitness_value = 0.0; // fitness value for the static 
	private double fitness_value2 = 0.0; // fitness value for the robustness

	private ServiceGraph graph;

	public Service[] genome;
	public List<Service> relevantList;
	public List<Service> failedSerList;

	private int splitPosition;
	public List<Integer> serQueue = new ArrayList<Integer>(); // service Index arraylist

	public double getAvailability() {
		return availability;
	}

	public void setAvailability(double availability) {
		this.availability = availability;
	}

	public double getReliability() {
		return reliability;
	}

	public void setReliability(double reliability) {
		this.reliability = reliability;
	}

	public double getTime() {
		return time;
	}

	public void setTime(double time) {
		this.time = time;
	}

	public double getCost() {
		return cost;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public double getMatchingType() {
		return matchingType;
	}

	public void setMatchingType(double matchingType) {
		this.matchingType = matchingType;
	}

	public double getSemanticDistance() {
		return semanticDistance;
	}

	public void setSemanticDistance(double semanticDistance) {
		this.semanticDistance = semanticDistance;
	}

	public String getStrRepresentation() {
		return strRepresentation;
	}

	public void setStrRepresentation(String strRepresentation) {
		this.strRepresentation = strRepresentation;
	}

	public int getSplitPosition() {
		return splitPosition;
	}

	public void setSplitPosition(int splitPosition) {
		this.splitPosition = splitPosition;
	}

	public double getFitness_value() {
		return fitness_value;
	}

	public void setFitness_value(double fitness_value) {
		this.fitness_value = fitness_value;
	}

	public double getFitness_value2() {
		return fitness_value2;
	}

	public void setFitness_value2(double fitness_value2) {
		this.fitness_value2 = fitness_value2;
	}

	@Override
	public Parameter defaultBase() {
		return new Parameter("sequencevectorindividual");
	}

	public ServiceGraph getGraph() {
		return graph;
	}

	public void setGraph(ServiceGraph graph) {
		this.graph = graph;
	}

	@Override
	/**
	 * Initializes the individual.
	 */
	public void reset(EvolutionState state, int thread) {
		WSCInitializer init = (WSCInitializer) state.initializer;
		List<Service> relevantList = init.initialWSCPool.getServiceSequence();
		Collections.shuffle(relevantList, init.random);

		genome = new Service[relevantList.size()];
		relevantList.toArray(genome);
		this.evaluated = false;
	}

	@Override
	public boolean equals(Object ind) {
		boolean result = false;

		if (ind != null && ind instanceof SequenceVectorIndividual) {
			result = true;
			SequenceVectorIndividual other = (SequenceVectorIndividual) ind;

			for (int i = 0; i < genome.length; i++) {
				if (!genome[i].equals(other.genome[i])) {
					result = false;
					break;
				}

			}
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(genome);
	}

	@Override
	public String toString() {
		return strRepresentation;
	}

	// public String toGraphString(EvolutionState state) {
	// WSCInitializer init = (WSCInitializer) state.initializer;
	//
	// // set the service candidates according to the sampling
	// InitialWSCPool.getServiceCandidates().clear();
	//
	// InitialWSCPool.setServiceCandidates(relevantList);
	//
	// ServiceGraph graph = init.graGenerator.generateGraphBySerQueue();
	// return graph.toString();
	// }

	public double calculateSequenceFitness(Service[] sequence, WSCInitializer init, EvolutionState state) {
		// generate DAG corresponding to vector

		InitialWSCPool.getServiceCandidates().clear();

		relevantList = new ArrayList<Service>();

		for (Service s : sequence) {
			relevantList.add(s);
		}

		InitialWSCPool.setServiceCandidates(relevantList);
		ServiceGraph graph = init.graGenerator.generateGraphBySerQueue();

		// set DAG string to individual
		this.setGraph(graph);
		this.setStrRepresentation(graph.toString());

		// evaluation
		init.eval.aggregationAttribute(this, graph);
		this.fitness_value = init.eval.calculateFitness(this);

		return this.fitness_value;

		// set fitness to individual
//		((SimpleFitness) fitness).setFitness(state, f, false); // XXX Move this inside the other one
//		this.evaluated = true;

	}

	public double calculateSequenceFitness4Disturbance(SequenceVectorIndividual ind2, WSCInitializer init,
			EvolutionState state) {
		// generate DAG corresponding to vector

		InitialWSCPool.getServiceCandidates().clear();

		relevantList = new ArrayList<Service>();

		failedSerList = new ArrayList<Service>();

		List<Integer> fullSerQueue = new ArrayList<Integer>();

		Service[] sequence = ind2.genome;

		for (Service s : sequence) {
			// check the status based on the failure probability
			double dice = init.random_disturbace.nextDouble();
			if (dice >= s.getFailure_probability()) {
				relevantList.add(s);
				fullSerQueue.add(s.serviceIndex);
			} else {
				failedSerList.add(s);
			}
		}

		// if a component service fails, we need to re-calcuate it, otherwise, same
		// fitness value is returned
		// set a flag = 1, 1 for applying local search

		int flag = 0;
		for (Service failedSer : failedSerList) {
			if (ind2.getGraph().vertexSet().contains(failedSer.getServiceID())) {
				flag = 1;
				break;
			}
		}

		if (flag == 0) {
			return ind2.getFitness_value();

		} else {

			// starting individual based on the given sequence of services
			SequenceVectorIndividual indi_start = new SequenceVectorIndividual();

			// set the service candidates according to the status
			InitialWSCPool.setServiceCandidates(relevantList);

			List<Integer> usedSerQueue = new ArrayList<Integer>();

			ServiceGraph update_graph = init.graGenerator.generateGraphBySerQueue();
			// adjust the bias according to the valid solution from the service queue

			if (!update_graph.containsVertex("startNode")) {

//			System.out.println("No solution could be found!");
				// under this case, we set fitness values as 0
				return 0;
			} else {

				// create a queue of services according to breathfirstsearch algorithm
				List<Integer> usedQueue = init.graGenerator.usedQueueofLayers("startNode", update_graph, usedSerQueue);
				// set up the split index for the updated individual
				indi_start.setSplitPosition(usedQueue.size());

				// add unused queue to form a complete a vector-based individual
				List<Integer> serQueue = init.graGenerator.completeSerQueueIndi(usedQueue, fullSerQueue);

				// set the serQueue to the updatedIndividual
				indi_start.serQueue = serQueue;

				// evaluate updated updated_graph
				// eval.aggregationAttribute(indi_updated, updated_graph);
				indi_start.setStrRepresentation(update_graph.toString());
				init.eval.aggregationAttribute(indi_start, update_graph);
				indi_start.setFitness_value(init.eval.calculateFitness(indi_start));

				LocalSearch ls = new LocalSearch();
				SequenceVectorIndividual indi_fixed = ls.randomSwapOnefromLayers(indi_start, init, state);

				return indi_fixed.getFitness_value();
			}
		}
	}

	public double calculateSequenceFitness4DisturbanceBasedOnImportance(Service[] sequence, Service ser_Important,
			WSCInitializer init, EvolutionState state) {
		// generate DAG corresponding to vector

		InitialWSCPool.getServiceCandidates().clear();

		relevantList = new ArrayList<Service>();
		List<Integer> fullSerQueue = new ArrayList<Integer>();

		for (Service s : sequence) {
			// check the status based on the failure probability
			if (!(s.serviceID.equals(ser_Important.serviceID))) {
				relevantList.add(s);
				fullSerQueue.add(s.serviceIndex);
			}

		}

		// starting individual based on the given sequence of services
		SequenceVectorIndividual indi_start = new SequenceVectorIndividual();

		// set the service candidates according to the status
		InitialWSCPool.setServiceCandidates(relevantList);

		List<Integer> usedSerQueue = new ArrayList<Integer>();

		ServiceGraph update_graph = init.graGenerator.generateGraphBySerQueue();
		// adjust the bias according to the valid solution from the service queue

		if (!update_graph.containsVertex("startNode")) {

//			System.out.println("No solution could be found!");
			// under this case, we set fitness values as 0
			return 0;
		} else {

			// create a queue of services according to breathfirstsearch algorithm
			List<Integer> usedQueue = init.graGenerator.usedQueueofLayers("startNode", update_graph, usedSerQueue);
			// set up the split index for the updated individual
			indi_start.setSplitPosition(usedQueue.size());

			// add unused queue to form a complete a vector-based individual
			List<Integer> serQueue = init.graGenerator.completeSerQueueIndi(usedQueue, fullSerQueue);

			// set the serQueue to the updatedIndividual
			indi_start.serQueue = serQueue;

			// evaluate updated updated_graph
			// eval.aggregationAttribute(indi_updated, updated_graph);
			indi_start.setStrRepresentation(update_graph.toString());
			init.eval.aggregationAttribute(indi_start, update_graph);
			indi_start.setFitness_value(init.eval.calculateFitness(indi_start));

			LocalSearch ls = new LocalSearch();
			SequenceVectorIndividual indi_fixed = ls.randomSwapOnefromLayers(indi_start, init, state);

			return indi_fixed.getFitness_value();
		}
	}

}
