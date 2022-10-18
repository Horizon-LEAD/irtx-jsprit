package fr.irtx.lead.jsprit;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import fr.irtx.lead.jsprit.data.instance.ProblemData;
import fr.irtx.lead.jsprit.data.solution.SolutionData;
import fr.irtx.lead.jsprit.infrastructure.EuclideanDistanceInfrastructureManager;
import fr.irtx.lead.jsprit.infrastructure.InfrastructureManager;
import fr.irtx.lead.jsprit.infrastructure.NetworkInfrastructureManager;

public class RunSolver {
	static public void main(String[] args) throws JsonParseException, JsonMappingException, IOException,
			ConfigurationException, NoSuchAuthorityCodeException, FactoryException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("problem-path", "solution-path", "crs") //
				.allowOptions("osm-path", "iterations", "threads", "random-seed", "freespeed-factor") //
				.build();

		File problemPath = new File(cmd.getOptionStrict("problem-path"));
		File solutionPath = new File(cmd.getOptionStrict("solution-path"));
		String crs = cmd.getOptionStrict("crs");

		Optional<File> osmPath = cmd.getOption("osm-path").map(File::new);

		int iterations = cmd.getOption("iterations").map(Integer::parseInt).orElse(10000);
		int threads = cmd.getOption("threads").map(Integer::parseInt).orElse(1);
		int randomSeed = cmd.getOption("random-seed").map(Integer::parseInt).orElse(1234);
		double freespeedFactor = cmd.getOption("freespeed-factor").map(Double::parseDouble).orElse(0.7);

		ProblemData problemData = new ObjectMapper().readValue(problemPath, ProblemData.class);
		GeotoolsTransformation wgsToEuclidean = new GeotoolsTransformation("EPSG:4326", crs);

		final InfrastructureManager infrastructure;
		if (osmPath.isEmpty()) {
			infrastructure = new EuclideanDistanceInfrastructureManager(problemData, wgsToEuclidean);
		} else {
			Network network = new SupersonicOsmNetworkReader.Builder() //
					.setCoordinateTransformation(wgsToEuclidean) //
					.build() //
					.read(osmPath.get().toPath());

			new NetworkCleaner().run(network);

			infrastructure = new NetworkInfrastructureManager(problemData, network, wgsToEuclidean, freespeedFactor);
		}

		LEADSolver solver = new LEADSolver(infrastructure, iterations, threads, randomSeed);
		SolutionData solutionData = solver.solve(problemData);

		new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(solutionPath, solutionData);
	}
}
