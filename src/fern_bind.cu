#include <stdlib.h>
#include <stdio.h>
#include "FERNIntegrator.cuh"

void fern_create_context(int species, int numReactions, fern_real massTol, fern_real fluxFrac, char *network_library, char *rate_library, void *fern_context)
{
	FERNIntegrator *integrator;
	integrator = new FERNIntegrator;

	integrator->network.species = species;;
	integrator->network.reactions = numReactions;
	integrator->network.massTol = massTol;
	integrator->network.fluxFrac = fluxFrac;

	integrator->network.allocate();
	integrator->network.loadNetwork(network_library);
	integrator->network.loadReactions(rate_library);

	integrator->initializeCuda();
	integrator->prepareKernel();

	fern_context = (void *) integrator;
}

void fern_release_context(void *fern_context)
{
  FERNIntegrator *integrator;
  integrator = (FERNIntegrator *) fern_context;

  delete integrator;
}

void fern_integrate(fern_real t_init, fern_real t_max, fern_real dt_init, fern_real tmp, fern_real rho, fern_real *xIn, fern_real *xOut, fern_real *sdot, char *network_library, void *fern_context)
{
	FERNIntegrator *integrator;
	integrator = (FERNIntegrator *) fern_context;

	IntegrationData integrationData;
	integrationData.allocate(integrator->network.species);
	integrationData.loadAbundances(network_library);

	integrationData.T9 = tmp; /* Only actually convert to T9. */
	integrationData.t_init = t_init;
	integrationData.t_max = t_max;
	integrationData.dt_init = dt_init;
	integrationData.rho = rho;

	integrator->integrate(integrationData, Xin);
}
