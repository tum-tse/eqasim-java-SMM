package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import com.google.common.io.Resources;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.*;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;

import java.net.URL;
import java.util.*;

/**
 * This is an example run script that runs the Corsica test scenario with an
 * on-demand vehicle fleet using DRT.
 * 
 * The scenario files for the Corisca scenario are located in the resources of
 * the ile_de_france module and the additional fleet definition file is located
 * in the resources of the examples module.
 */
public class ModeChoiceExample {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.allowOptions("use-rejection-constraint") //
				.allowPrefixes("mode-parameter", "cost-parameter","sharing-mode-name") //
				.build();
		URL configUrl = Resources.getResource("corsica/corsica_config.xml");

		Config config = ConfigUtils.loadConfig(configUrl, IDFConfigurator.getConfigGroups());

		config.controler().setLastIteration(3);
		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);
		final String PEDELEC = "pedelec";



// We need to add the sharing config group
		SharingConfigGroup sharingConfig = new SharingConfigGroup();
		config.addModule(sharingConfig);

		// We need to define a service ...
		SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
		sharingConfig.addService(serviceConfig);

		// ... with a service id. The respective mode will be "sharing:velib".
		serviceConfig.setId("bikeShare");

		// ... with freefloating characteristics
		serviceConfig.setMaximumAccessEgressDistance(100000);
		serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
		serviceConfig.setServiceAreaShapeFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester 4\\Matsim\\eqasim-java-develop\\SBBSDummy.xml");

		// ... with a number of available vehicles and their initial locations
		serviceConfig.setServiceInputFile("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\MATSim_Root\\SBBSDummy.xml");

		// ... and, we need to define the underlying mode, here "bike".
		serviceConfig.setMode("bike");

		// Finally, we need to make sure that the service mode (sharing:velib) is
		// considered in mode choice.
		List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
		modes.add(SharingUtils.getServiceMode(serviceConfig));
		config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

		// We need to add interaction activity types to scoring
		PlanCalcScoreConfigGroup.ActivityParams pickupParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.PICKUP_ACTIVITY);
		pickupParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(pickupParams);

		PlanCalcScoreConfigGroup.ActivityParams dropoffParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.DROPOFF_ACTIVITY);
		dropoffParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(dropoffParams);

		PlanCalcScoreConfigGroup.ActivityParams bookingParams = new PlanCalcScoreConfigGroup.ActivityParams(SharingUtils.BOOKING_ACTIVITY);
		bookingParams.setScoringThisActivityAtAll(false);
		config.planCalcScore().addActivityParams(bookingParams);

		cmd.applyConfiguration(config);

		{ // Add the DRT mode to the choice model
			DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);


			Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
			tripConstraints.add("ShapeFile");
			Set<String> tripFilters = new HashSet<>(Arrays.asList("TourLengthFilter"));
			dmcConfig.setTripConstraints(tripConstraints);
			//dmcConfig.removeParameterSet(erasedSet);
			ConfigGroup set = dmcConfig.createParameterSet("tripConstraint:ShapeFile");
			set.addParam("constrainedModes", "sharing:bikeShare");
			set.addParam("path", "C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasim-java\\ile_de_france\\src\\main\\resources\\corsica\\extent.shp");
			set.addParam("requirement", "BOTH");
			dmcConfig.addParameterSet(set);

//			// Add the Trip constraints to tour filters
//			Set<String> tourConstraints = new HashSet<>(dmcConfig.getTourConstraints());
//			tourConstraints.add("FromTripBased");
//			dmcConfig.setTourConstraints(tourConstraints);
			// Add DRT to cached modes
			Set<String> cachedModes = new HashSet<>();
			cachedModes.addAll(dmcConfig.getCachedModes());
//			cachedModes.add("drt");
			cachedModes.add("pedelec");
			dmcConfig.setCachedModes(cachedModes);
			dmcConfig.setModeAvailability("KModeAvailability");
		}




			// Set up choice model
			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

			//Key Apart from modifying the  binders , add the neww estimators, etc etc 
			eqasimConfig.setEstimator("bike","KBike");
			eqasimConfig.setCostModel("sharing:bikeShare","sharing:bikeShare");
			eqasimConfig.setEstimator("sharing:bikeShare","sharing:bikeShare");
		    eqasimConfig.setEstimator("pt","KPT");

		    eqasimConfig.setEstimator("car","KCar");
			eqasimConfig.setEstimator("walk","KWalk");
			// Set analysis interval
			eqasimConfig.setTripAnalysisInterval(1);

//

		Scenario scenario = ScenarioUtils.createScenario(config);
		IDFConfigurator.configureScenario(scenario);


		ScenarioUtils.loadScenario(scenario);

		// Set up controller (no specific settings needed for scenario)

		Controler controller = new Controler(config);

		// Does not really "override" anything
		controller.addOverridingModule(new SharingModule());


		IDFConfigurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
//		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new ModeChoiceModuleExample(cmd,scenario));
		controller.addOverridingModule(new MicroMobilityModeEqasimModeChoiceModule(cmd,scenario,"sharing:bikeShare","C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\MATSim_Root\\SBBSDummy.xml"));
     	//controller.addOverridingModule(new MultiModalModule());
		//controller.addOverridingModule(new SwissRailRaptorModule());
		new NetworkCleaner().run(scenario.getNetwork());

		controller.run();


		ConfigWriter cw= new ConfigWriter(config);
		cw.write("tryingConfig.xml");
	}
}
