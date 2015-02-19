void fern_create_context(int species, int numReactions, fern_real massTol, fern_real fluxFrac, char *network_library, char *rate_library, void *fern_context);

void fern_release_context(void *fern_context);

void fern_integrate(fern_real t_init, fern_real t_max, fern_real dt_init, fern_real tmp, fern_real rho, fern_real *xIn, fern_real *xOut, fern_real *sdot, char *network_library, void *fern_context);
