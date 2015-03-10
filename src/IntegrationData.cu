
#include "IntegrationData.cuh"
#include "Network.cuh"
#include <stdio.h>


void IntegrationData::loadAbundances(const char *filename)
{
	char ignoredData[16];
	fern_real ignoredDouble;
	int ignoredInt;
	
	FILE *file = fopen(filename, "r");
	
	// Exit if the file doesn't exist or can't be read
	
	if (!file)
	{
		fprintf(stderr, "Could not read file '%s'\n", filename);
		exit(1);
	}
	
	// Read 4 lines at a time
	
	for (int n = 0; n < species; n++)
	{
		int status;
		
		// Line #1
		
		#ifdef FERN_SINGLE
			status = fscanf(file, "%s %d %d %d %f %f\n",
				ignoredData, &ignoredInt, &ignoredInt,
				&ignoredInt, &Y[n], &ignoredDouble);
		#else
			status = fscanf(file, "%s %d %d %d %lf %lf\n",
				ignoredData, &ignoredInt, &ignoredInt,
				&ignoredInt, &Y[n], &ignoredDouble);
		#endif
		
		if (status == EOF)
			break;
		
		// Line #2...4
		
		for (int i = 0; i < 8 * 3; i++)
		{
			#ifdef FERN_SINGLE
				status = fscanf(file, "%f", &ignoredDouble);
			#else
				status = fscanf(file, "%lf", &ignoredDouble);
			#endif
		}
	}
}


void IntegrationData::allocate(unsigned short species)
{
	this->species = species;
	Y = new fern_real[species];
	eq = new int;
}


void IntegrationData::cudaAllocate(unsigned short species)
{
	this->species = species;
	cudaMalloc(&Y, sizeof(fern_real) * species);
	cudaMalloc(&eq, sizeof(int));
}


void IntegrationData::cudaCopy(const IntegrationData &source, cudaMemcpyKind kind)
{
	// Copy scalars
	
	T9 = source.T9;
	t_init = source.t_init;
	t_max = source.t_max;
	dt_init = source.dt_init;
	rho = source.rho;
	
	// Copy vectors
	
	cudaMemcpy(Y, source.Y, sizeof(fern_real) * species, kind);
	cudaMemcpy(eq, source.eq, sizeof(int), kind);
}


void IntegrationData::print(Network *n)
{
	printf("species: %d\n", species);
	
	printf("T9: %e\n", T9);
	printf("t_init: %e\n", t_init);
	printf("t_max: %e\n", t_max);
	printf("dt_init: %e\n", dt_init);
	printf("rho: %e\n", rho);
	
	printf("Y: ");
    for (unsigned short i = 0; i < species; i++) {
        printf("Species %d: %s, Z = %d, Y = %e\n", i, n->isotopeLabel[i], n->Z[i], Y[i]);
        printf("eq: %d\n", eq);
    }
}
