package wsc.ecj.ga;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import ec.EvolutionState;
import ec.Individual;
import ec.simple.SimpleEvaluator;

public class GAEvaluator extends SimpleEvaluator {

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

		}

	}

}
