package wsc.ecj.ga;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import ec.BreedingPipeline;
import ec.EvolutionState;
import ec.Individual;
import ec.Population;
import ec.simple.SimpleBreeder;

public class GABreeder extends SimpleBreeder {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1191690307192991445L;

	@Override
	public Population breedPopulation(EvolutionState state) {

		if (state.generation != WSCInitializer.restartSize - 1) {
			return super.breedPopulation(state);
		} else {

			Population oldPop = (Population) state.population;
			Population newPop = (Population) state.population.emptyClone();

			Individual[] oldInds = oldPop.subpops[0].individuals;

			if (WSCInitializer.arvIndi.size() < WSCInitializer.archiveSize) {

				int randomSeriesLength = WSCInitializer.archiveSize - WSCInitializer.arvIndi.size();

				ArrayList<Individual> oldIndsList = Lists.newArrayList(oldInds);
				Collections.shuffle(oldIndsList);

				List<Individual> ramdonOldInds = oldIndsList.subList(0, randomSeriesLength);

				WSCInitializer.arvIndi.addAll(ramdonOldInds);

			}

			Individual[] newInds = WSCInitializer.arvIndi.toArray(new Individual[oldPop.subpops[0].individuals.length]);

			System.out.println("arv: " + WSCInitializer.arvIndi.size());

			newPop.subpops[0].individuals = newInds;

//			re-set status of evaluation
			for (Individual indi : newPop.subpops[0].individuals) {
				indi.evaluated = false;
			}

			return newPop;

		}

	}

}
