package org.squiddev.plethora.integration.forestry;

import com.google.common.collect.Maps;
import forestry.api.genetics.IChromosomeType;
import forestry.api.genetics.IIndividual;
import forestry.core.config.Constants;
import org.squiddev.plethora.api.meta.BaseMetaProvider;
import org.squiddev.plethora.api.meta.IMetaProvider;
import org.squiddev.plethora.api.method.IPartialContext;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.Map;

@IMetaProvider.Inject(value = IIndividual.class, modId = Constants.MOD_ID)
public class MetaIndividual extends BaseMetaProvider<IIndividual> {
	@Nonnull
	@Override
	public Map<Object, Object> getMeta(@Nonnull IPartialContext<IIndividual> context) {
		IIndividual individual = context.getTarget();
		Map<Object, Object> out = Maps.newHashMap();
		out.put("id", individual.getIdent());
		out.put("analyzed", individual.isAnalyzed());

		if (individual.isAnalyzed()) {
			out.put("genome", context.makePartialChild(individual.getGenome()).getMeta());

			Map<String, Boolean> pureBred = Maps.newHashMap();
			for (IChromosomeType type : individual.getGenome().getSpeciesRoot().getKaryotype()) {
				pureBred.put(type.getName().toLowerCase(Locale.ENGLISH), individual.isPureBred(type));
			}

			out.put("pureBred", pureBred);
		}

		return out;
	}
}
