package wsc.ecj.ga;

import java.util.ArrayList;
import java.util.Set;

import com.google.common.collect.Lists;

import ec.*;
import ec.simple.*;
import wsc.data.pool.Service;

public class WSCProblem extends Problem implements SimpleProblemForm {
	private static final long serialVersionUID = 1L;

	public void evaluate(final EvolutionState state, final Individual ind, final int subpopulation,
			final int threadnum) {
		if (ind.evaluated)
			return;

		if (!(ind instanceof SequenceVectorIndividual))
			state.output.fatal("Whoa!  It's not a SequenceVectorIndividual!!!", null);

		SequenceVectorIndividual ind2 = (SequenceVectorIndividual) ind;
		WSCInitializer init = (WSCInitializer) state.initializer;

		if (!(ind2.fitness instanceof SimpleFitness))
			state.output.fatal("Whoa!  It's not a SimpleFitness!!!", null);

		// an evaluation 4 f(ind2)
		double f_ind2 = ind2.calculateSequenceFitness(ind2.genome, init, state);
		ind2.setFitness_value(f_ind2);

		// evaluations 4 robustness

//!!!Method for important sampling based on single dimension failures 

		if (state.generation < WSCInitializer.restartSize) {

			((SimpleFitness) ind2.fitness).setFitness(state, f_ind2, false);

		} else {

			double f = calculateRustnessFitnessBasedOnImportantSampling(ind2, f_ind2, init, state);
			((SimpleFitness) ind2.fitness).setFitness(state, f, false);

		}

		ind2.evaluated = true;

	}

	// calculate the robustness using Monte Carlo sampling technique

	private double calculateRustnessFitness(SequenceVectorIndividual ind2, WSCInitializer init, EvolutionState state) {

		double f_sum = 0.0;
		for (int i = 0; i < init.robustNum; i++) {
			// simulate an disturbance
			double f_ind2 = ind2.calculateSequenceFitness4Disturbance(ind2, init, state);
			f_sum += f_ind2;
		}

		double part2 = f_sum / init.robustNum;

		// set fitness value for part2
		ind2.setFitness_value2(part2);

		return part2;

	}

	// calculate the robustness using important sampling technique

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