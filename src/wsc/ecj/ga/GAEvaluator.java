package wsc.ecj.ga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import ec.EvolutionState;
import ec.Individual;
import ec.simple.SimpleEvaluator;
import ec.simple.SimpleFitness;
import wsc.data.pool.Service;

public class GAEvaluator extends SimpleEvaluator {

	double avgRobustHis = 0;

	/**
	 * 
	 */
	private static final long serialVersionUID = -1441262664608251008L;

	@Override
	public void evaluatePopulation(EvolutionState state) {
		// TODO Auto-generated method stub
		super.evaluatePopulation(state);

		// We are build archive before the restartSize

		if (state.generation < WSCInitializer.restartSize) {

			Individual[] currentPopArray = state.population.subpops[0].individuals;

			List<Individual> currentPopList = Lists.newArrayList(currentPopArray);

			WSCInitializer.arvIndi.addAll(currentPopList);

			// remove duplicates based on fitness values

			Map<Double, Individual> noduplicatedIndi = new HashMap<Double, Individual>();
			for (Individual indi : WSCInitializer.arvIndi) {
				Double key = indi.fitness.fitness();
				if (!noduplicatedIndi.containsKey(key)) {
					noduplicatedIndi.put(key, indi);
				}
			}

			Collection<Individual> noduplicatedIndiCon = noduplicatedIndi.values();
			WSCInitializer.arvIndi.clear();
			WSCInitializer.arvIndi.addAll(noduplicatedIndiCon);

			Ordering<Individual> orderingByFit = new Ordering<Individual>() {
				@Override
				public int compare(Individual p1, Individual p2) {
					return Double.compare(p2.fitness.fitness(), p1.fitness.fitness());
				}
			};

			// sort it based on fitness values
			WSCInitializer.arvIndi.sort(orderingByFit);
//			Collections.shuffle(WSCInitializer.arvIndi);

			if (WSCInitializer.arvIndi.size() > WSCInitializer.archiveSize) {

				WSCInitializer.arvIndi = WSCInitializer.arvIndi.subList(0, 30);
//				System.out.print(WSCInitializer.arvIndi.size());
			}

			// shall we continue GA or GA4Robust?"
//			WSCInitializer.restartSize = 1;
			// observe the avg robustness over the the archive
			// if positive then GA otherwise GA4Robust

			if (state.generation >= WSCInitializer.restartLowerSize) // at least GA runs 50 generations
				avgRobustness4Archive(state);

		}

	}

	private void avgRobustness4Archive(EvolutionState state) {

		double avgRobust = 0;
		double sumRobust = 0;

		for (Individual ind : WSCInitializer.arvIndi) {
			SequenceVectorIndividual ind2 = (SequenceVectorIndividual) ind;
			WSCInitializer init = (WSCInitializer) state.initializer;

			if (ind2.getFitness_value2() == 0.0) {
				double f_ind2 = ind2.getFitness_value();
				double f = calculateRustnessFitnessBasedOnImportantSampling(ind2, f_ind2, init, state);
				ind2.setFitness_value2(f);

				System.out.println("X");

			}
		}

		for (Individual ind : WSCInitializer.arvIndi) {

			SequenceVectorIndividual ind2 = (SequenceVectorIndividual) ind;
			sumRobust += ind2.getFitness_value2();
		}

		avgRobust = sumRobust / WSCInitializer.arvIndi.size();

		if (avgRobust > avgRobustHis) {

			System.out.print("Keep GA without restart!" + "Before:" + avgRobustHis + "After:" + avgRobust);

			// update historical robustness
			avgRobustHis = avgRobust;
			WSCInitializer.restartSize = state.generation + WSCInitializer.every_num_gen;

		}
	}

	private double calculateRustnessFitnessBasedOnImportantSampling(SequenceVectorIndividual ind2, double orig_f_ind2,
			WSCInitializer init, EvolutionState state) {

		double f_sum = 0.00;
		double f_ind2 = 0.00;

		for (int i = 0; i < ind2.genome.length; i++) {
			Service ser_Important = ind2.genome[i];

			// simulate an disturbance based on the dimension of permutation

			if (ind2.getGraph().vertexSet().contains(ser_Important.getServiceID())) {
				f_ind2 = ind2.calculateSequenceFitness4DisturbanceBasedOnImportance(ind2.genome, ser_Important, init,
						state);

			} else { // if ser_Important is not contained in ind2, then f_ind2 remains the same

				f_ind2 = orig_f_ind2;
			}

			// we need to calculate the weight for this scenario

			double failed_Ser_Important = ser_Important.getFailure_probability();

			double weight4f_ind2 = WSCInitializer.weight4NoFailrues / (WSCInitializer.weight4NoFailrues
					- logScale4Success(failed_Ser_Important) + logScale4Failure(failed_Ser_Important));

//			System.out.println(weight4f_ind2+ ": "+ i);

			f_sum += (weight4f_ind2 * f_ind2);

		}
		double part2 = f_sum;

		// set fitness value for part2
		ind2.setFitness_value2(part2);
		return part2;
	}

	private double logScale4Failure(double failure_probability) {

		return Math.log10(failure_probability);
	}

	private double logScale4Success(double failure_probability) {

		return Math.log10(1 - failure_probability);
	}

}
