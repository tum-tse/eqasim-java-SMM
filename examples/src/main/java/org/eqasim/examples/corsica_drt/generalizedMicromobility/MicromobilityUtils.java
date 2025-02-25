package org.eqasim.examples.corsica_drt.generalizedMicromobility;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.examples.corsica_drt.GBFSUtils.ReadFreeFloatingGBFS;
import org.eqasim.examples.corsica_drt.GBFSUtils.ReadStationBasedGBFS;
import org.eqasim.examples.corsica_drt.sharingPt.GeneralizedSharingPT.GeneralizedSharingPTModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.sharing.run.SharingConfigGroup;
import org.matsim.contrib.sharing.run.SharingModule;
import org.matsim.contrib.sharing.run.SharingServiceConfigGroup;
import org.matsim.contrib.sharing.service.SharingUtils;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;

import java.io.IOException;
import java.util.*;

public class MicromobilityUtils implements ParameterDefinition {

    public String mode;
    public String serviceScheme;
    public String serviceArea=null;
    public Double accessDist;

    public static void main(String[] args) throws IllegalAccessException {
//        addSharingService(ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml"),"Shared-Bike","Station Based",10000.0,null,null,null);
//        addSharingService(ConfigUtils.loadConfig("C:\\Users\\juan_\\Desktop\\TUM\\Semester5\\Thesis\\eqasimMicromobility\\examples\\src\\main\\java\\org\\eqasim\\examples\\corsica_drt\\siouxfalls\\config.xml"),"eScooter","None",10000.0,null,null,null);
    }
    public static void  addSharingServices(CommandLine cmd,Controler controller, Config config,Scenario scenario) throws IllegalAccessException {
        HashMap<String,HashMap<String,String>> sharingServicesInput=applyCommandLineServices("sharing-mode-name",cmd);
        try {
            generateServiceFile(sharingServicesInput,config,scenario);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for( String key :sharingServicesInput.keySet()) {
            HashMap<String,String>service=sharingServicesInput.get(key);

//            "Service_File", "Mode", "Scheme","Service_Name","Intermodal","AccessEgress_Distance"
                String serviceFile=service.get("Service_File");
                String mode=service.get("Mode");
                String name=service.get("Service_Name");
                Double accessEgress=Double.parseDouble(service.get("AccessEgress_Distance"));
                String scheme=service.get("Scheme");
                String multimodal=service.get("Multimodal");
                String serviceArea="";
                if(service.keySet().contains("Service_Area")){
                    serviceArea=service.get("Service_Area");
                }
                addSharingService(controller,config,mode,scheme,accessEgress,serviceArea,serviceFile,name,multimodal);
                addSharingServiceToEqasim(controller,config,cmd,scenario,service);
                if(multimodal.equals("Yes")){
                    controller.addOverridingModule(new GeneralizedSharingPTModule(scenario,name));
                }
        }
        controller.addOverridingModule(new SharingModule());
        controller.configureQSimComponents(SharingUtils.configureQSim((SharingConfigGroup) config.getModules().get("sharing")));

    }


    public static void  addSharingServicesCharyParNagel(CommandLine cmd,Controler controller, Config config,Scenario scenario) throws IllegalAccessException {
        HashMap<String,HashMap<String,String>> sharingServicesInput=applyCommandLineServices("sharing-mode-name",cmd);
        try {
            generateServiceFile(sharingServicesInput,config,scenario);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for( String key :sharingServicesInput.keySet()) {
            HashMap<String,String>service=sharingServicesInput.get(key);

//            "Service_File", "Mode", "Scheme","Service_Name","Intermodal","AccessEgress_Distance"
            String serviceFile=service.get("Service_File");
            String mode=service.get("Mode");
            String name=service.get("Service_Name");
            Double accessEgress=Double.parseDouble(service.get("AccessEgress_Distance"));
            String scheme=service.get("Scheme");
            String multimodal=service.get("Multimodal");
            String serviceArea="";
            if(service.keySet().contains("Service_Area")){
                serviceArea=service.get("Service_Area");
            }
            addSharingService(controller,config,mode,scheme,accessEgress,serviceArea,serviceFile,name,multimodal);
//
        }
        controller.addOverridingModule(new SharingModule());
        controller.configureQSimComponents(SharingUtils.configureQSim((SharingConfigGroup) config.getModules().get("sharing")));

    }
    public static void addSharingService(Controler controller, Config config, String mode, String serviceScheme, Double accessDist, String serviceArea, String serviceFile, String name, String multimodal) throws IllegalAccessException {


        SharingConfigGroup sharingConfigGroup = (SharingConfigGroup) config.getModules().get("sharing");
        if (sharingConfigGroup == null) {
            sharingConfigGroup = new SharingConfigGroup();
            config.addModule(sharingConfigGroup);
        }
        Map<String, PlansCalcRouteConfigGroup.ModeRoutingParams> routingParams = config.plansCalcRoute().getModeRoutingParams();
        if(routingParams.containsKey("Shared-Bike")== false && routingParams.containsKey("eScooter")==false) {
            addSharedModes(config);
        }
        // Save for super method
        String serviceSchemes[] = new String[]{"Station-Based", "Free-floating"};
        String possibleModes[] = new String[]{"Shared-Bike", "eScooter"};
        boolean contains = Arrays.stream(possibleModes).anyMatch(mode::equals);
        if (contains == false) {
            {
                throw new IllegalArgumentException(" The sharing mode is invalid; please insert Shared-Bike or eScooter");
            }
        } else {
            boolean serviceSchemeVerification = Arrays.stream(serviceSchemes).anyMatch(serviceScheme::equals);
            if (serviceSchemeVerification == false) {
                {
                    throw new IllegalArgumentException(" The service scheme is invalid; please insert Station Based or Free-floating");
                }
            } else {
                if (serviceScheme.equals("Station-Based")) {
                    addSharedStationBasedService(config, sharingConfigGroup, accessDist, name, mode, serviceFile);
                } else if (serviceScheme.equals("Free-floating")) {
                    addSharedFreeFloatingService(config, sharingConfigGroup, serviceArea, accessDist, name, mode, serviceFile);
                }
//                if(multimodal.equals("Yes")){
//                    List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
//                    if(modes.contains(SharingUtils.getServiceMode(serviceConfig))) {
//                        modes.add(SharingUtils.getServiceMode(serviceConfig));
//                    }
//                    config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
//
//                }
            }
        }
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

       // return sharingConfigGroup;

    }






    // Method adds the routing parameters for Shared Bike mode & eScooter ( Based on speeds from Hamad et al ,2020)
    public static void addSharedModes(Config config){
        PlansCalcRouteConfigGroup.ModeRoutingParams bikeRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("Shared-Bike");
        bikeRoutingParams.setTeleportedModeSpeed(3.44);
        bikeRoutingParams.setBeelineDistanceFactor(1.3);
        config.plansCalcRoute().addModeRoutingParams(bikeRoutingParams);


        PlansCalcRouteConfigGroup.ModeRoutingParams eScooterRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("eScooter");
        eScooterRoutingParams.setTeleportedModeSpeed(2.78);
        eScooterRoutingParams.setBeelineDistanceFactor(1.3);
        config.plansCalcRoute().addModeRoutingParams(eScooterRoutingParams);


//        PlansCalcRouteConfigGroup.ModeRoutingParams walkRoutingParams = new PlansCalcRouteConfigGroup.ModeRoutingParams("walk");
//        walkRoutingParams.setTeleportedModeSpeed(2.0);
//        walkRoutingParams.setBeelineDistanceFactor(1.3);
//        config.plansCalcRoute().addModeRoutingParams(walkRoutingParams);

        // We need to score bike
        PlanCalcScoreConfigGroup.ModeParams bikeScoringParams = new PlanCalcScoreConfigGroup.ModeParams("Shared-Bike");
        config.planCalcScore().addModeParams(bikeScoringParams);
        PlanCalcScoreConfigGroup.ModeParams eScooterScoringParams = new PlanCalcScoreConfigGroup.ModeParams("eScooter");
      config.planCalcScore().addModeParams(eScooterScoringParams);

    }


    public static void addSharedFreeFloatingService(Config config,SharingConfigGroup configGroup, String serviceArea,Double accessDistance, String name, String mode,String serviceFile) {

        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
        configGroup.addService(serviceConfig);

        // ... with a service id. The respective mode will be "sharing:velib".
        serviceConfig.setId(name);

        // ... with freefloating characteristics
        serviceConfig.setMaximumAccessEgressDistance(accessDistance);
        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        serviceConfig.setServiceAreaShapeFile(serviceArea);
        serviceConfig.setServiceInputFile(serviceFile);

        // Save for super method
        //String array[] =new String[]{"Station Based", "Free Floating" };

        //boolean contains = Arrays.stream(array).anyMatch(serviceScheme::equals);


        // ... and, we need to define the underlying mode, here "bike".
        serviceConfig.setMode(mode);

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(serviceConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    }
    public static void addSharedFreeFloatingService(Config config,SharingConfigGroup configGroup,HashMap<String,String>service) {

        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
        configGroup.addService(serviceConfig);

        // ... with a service id. The respective mode will be "sharing:velib".
        serviceConfig.setId(service.get("Name"));

        // ... with freefloating characteristics
        serviceConfig.setMaximumAccessEgressDistance(Double.parseDouble(service.get("AccessEgress_Distance")));
        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.Freefloating);
        if(service.keySet().contains("Service_Area")) {
            serviceConfig.setServiceAreaShapeFile(service.get("Service_Area"));
        }

        serviceConfig.setServiceInputFile(service.get("Service_File"));

        // ... and, we need to define the underlying mode, here "bike".
        serviceConfig.setMode(service.get("Mode"));

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        if(modes.contains(SharingUtils.getServiceMode(serviceConfig))) {
            modes.add(SharingUtils.getServiceMode(serviceConfig));
        }
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    }
    public static void addSharedStationBasedService(Config config, SharingConfigGroup configGroup, Double accessDistance, String name, String mode, String serviceFile) {

        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
        configGroup.addService(serviceConfig);

        // ... with a service id. The respective mode will be "sharing:velib".
        serviceConfig.setId(name);

        // ... with freefloating characteristics
        serviceConfig.setMaximumAccessEgressDistance(accessDistance);
        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
        serviceConfig.setServiceInputFile(serviceFile);

        serviceConfig.setMode(mode);

        // considered in mode choice.
        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(SharingUtils.getServiceMode(serviceConfig));
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    }
//    public static void addSharedStationBasedService(Config config, SharingConfigGroup configGroup, HashMap<String,String>service) {
//
//        SharingServiceConfigGroup serviceConfig = new SharingServiceConfigGroup();
//        configGroup.addService(serviceConfig);
//
//        // ... with a service id. The respective mode will be "sharing:velib".
//        serviceConfig.setId(service.get("Name"));
//
//        // ... with freefloating characteristics
//        serviceConfig.setMaximumAccessEgressDistance(Double.parseDouble(service.get("AccessEgress_Distance")));
//        serviceConfig.setServiceScheme(SharingServiceConfigGroup.ServiceScheme.StationBased);
//        serviceConfig.setServiceInputFile(service.get("Service_File"));
//
//        serviceConfig.setMode(service.get("Mode"));
//
//        // considered in mode choice.
//        List<String> modes = new ArrayList<>(Arrays.asList(config.subtourModeChoice().getModes()));
//        if(modes.contains(SharingUtils.getServiceMode(serviceConfig))) {
//            modes.add(SharingUtils.getServiceMode(serviceConfig));
//        }
//        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
//    }
    // Name must be as sharing:bikeShare !!
    public static void addSharingServiceToEqasim(Controler controller,Config config, CommandLine cmd, Scenario scenario,String name,String serviceFile){
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
// Scoring config definition to add the mode cat_pt parameters
        PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();
        PlanCalcScoreConfigGroup.ModeParams sharingPTParams = new PlanCalcScoreConfigGroup.ModeParams(name+"_PT");
        PlanCalcScoreConfigGroup.ModeParams pTSharingParams = new PlanCalcScoreConfigGroup.ModeParams("PT_"+name);
        PlanCalcScoreConfigGroup.ModeParams SharingPTSharingParams = new PlanCalcScoreConfigGroup.ModeParams(name+"_PT_"+name);
        scoringConfig.addModeParams(SharingPTSharingParams);
        scoringConfig.addModeParams(sharingPTParams);
        scoringConfig.addModeParams(pTSharingParams);

        // "car_pt interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsSharingPTInterAct = new PlanCalcScoreConfigGroup.ActivityParams("SharingPT_Interaction");
        paramsSharingPTInterAct.setTypicalDuration(100.0);
        paramsSharingPTInterAct.setScoringThisActivityAtAll(false);

        // "pt_car interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsPTSharingInterAct = new PlanCalcScoreConfigGroup.ActivityParams("PTSharing_Interaction");
        paramsPTSharingInterAct.setTypicalDuration(100.0);
        paramsPTSharingInterAct.setScoringThisActivityAtAll(false);

        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsSharingPTInterAct);
        scoringConfig.addActivityParams(paramsPTSharingInterAct);


        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsPTSharingInterAct);
        scoringConfig.addActivityParams(paramsSharingPTInterAct);

        //Key Apart from modifying the  binders , add the neww estimators, etc etc
        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

        Set<String> cachedModes = new HashSet<>();
        cachedModes.addAll(dmcConfig.getCachedModes());
        cachedModes.add("sharing:"+name);
        dmcConfig.setCachedModes(cachedModes);

        eqasimConfig.setCostModel("sharing:"+name,"sharing:"+name);
        eqasimConfig.setEstimator("sharing:"+name,"sharing:"+name);

        eqasimConfig.setEstimator("PT_bikeShare","PT_bikeShare");
        dmcConfig.setModeAvailability("ModeAvailability");


        controller.addOverridingModule(new MicroMobilityModeEqasimModeChoiceModule(cmd,scenario,name,serviceFile));


    }

    public static void addSharingServiceToEqasim(Controler controller,Config config, CommandLine cmd, Scenario scenario,HashMap<String,String> service){
        EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
// Scoring config definition to add the mode cat_pt parameters
        String name=service.get("Service_Name");
        PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();
        PlanCalcScoreConfigGroup.ModeParams sharingPTParams = new PlanCalcScoreConfigGroup.ModeParams(name+"_PT");
        PlanCalcScoreConfigGroup.ModeParams pTSharingParams = new PlanCalcScoreConfigGroup.ModeParams("PT_"+name);
        PlanCalcScoreConfigGroup.ModeParams SharingPTSharingParams = new PlanCalcScoreConfigGroup.ModeParams(name+"_PT_"+name);
        scoringConfig.addModeParams(SharingPTSharingParams);
        scoringConfig.addModeParams(sharingPTParams);
        scoringConfig.addModeParams(pTSharingParams);

        // "car_pt interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsSharingPTInterAct = new PlanCalcScoreConfigGroup.ActivityParams("SharingPT_Interaction");
        paramsSharingPTInterAct.setTypicalDuration(100.0);
        paramsSharingPTInterAct.setScoringThisActivityAtAll(false);

        // "pt_car interaction" definition
        PlanCalcScoreConfigGroup.ActivityParams paramsPTSharingInterAct = new PlanCalcScoreConfigGroup.ActivityParams("PTSharing_Interaction");
        paramsPTSharingInterAct.setTypicalDuration(100.0);
        paramsPTSharingInterAct.setScoringThisActivityAtAll(false);

        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsSharingPTInterAct);
        scoringConfig.addActivityParams(paramsPTSharingInterAct);


        // Adding "car_pt interaction" to the scoring
        scoringConfig.addActivityParams(paramsPTSharingInterAct);
        scoringConfig.addActivityParams(paramsSharingPTInterAct);

        //Key Apart from modifying the  binders , add the neww estimators, etc etc
        DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

        Set<String> cachedModes = new HashSet<>();
        cachedModes.addAll(dmcConfig.getCachedModes());
        cachedModes.add("sharing:"+name);
        dmcConfig.setCachedModes(cachedModes);

        eqasimConfig.setCostModel("sharing:"+name,"sharing:"+name);
        eqasimConfig.setEstimator("sharing:"+name,"sharing:"+name);
        Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
        tripConstraints.add(name);
        dmcConfig.setTripConstraints(tripConstraints);

        if(service.get("Multimodal").equals("Yes")){
            eqasimConfig.setEstimator("PT_"+name,"PT_"+name);
            eqasimConfig.setEstimator(name+"_PT_"+name,name+"_PT_"+name);
            eqasimConfig.setEstimator(name+"_PT",name+"_PT");
            cachedModes.add("PT_"+name);
            cachedModes.add(name+"_PT_"+name);
            cachedModes.add(name+"_PT");
            dmcConfig.setCachedModes(cachedModes);
            tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
            tripConstraints.add(name+"_PT_CONSTRAINT");
           dmcConfig.setTripConstraints(tripConstraints);
        }
//        eqasimConfig.setEstimator("PT_bikeShare","PT_bikeShare");
        dmcConfig.setModeAvailability(service.get("Service_Name")+"ModeAvailability");

        controller.addOverridingModule(new MicromobilityWIthMapModule(cmd,scenario,service));
        //controller.addOverridingModule(new MicroMobilityModeEqasimModeChoiceModule(cmd,scenario,name,serviceFile));


    }
    static public HashMap<String,HashMap<String,String>> applyCommandLineServices(String prefix, CommandLine cmd) {
        HashMap<String,HashMap<String,String>>  sharingModesInput= new HashMap<>();
        List<String> sharingModes=indentifySharingModes(prefix,cmd);
        int i=0;
        while(i<sharingModes.size()){
            sharingModesInput.put(sharingModes.get(i), new HashMap<String,String>());
            i+=1;
        }
        buildParameters(prefix,cmd,sharingModesInput);
        validateInputGBFS(sharingModesInput);
        return sharingModesInput;
    }
    static public List<String> indentifySharingModes(String prefix, CommandLine cmd) {
        List<String> sharingModes= new ArrayList<>();
        for (String option : cmd.getAvailableOptions()) {
            if (option.startsWith(prefix + ":")) {
                try {
                    String optionPart2=option.split(":")[1];
                    if (optionPart2.startsWith("Service_Name")) {
                        sharingModes.add(cmd.getOptionStrict(option));
                    }
                } catch (CommandLine.ConfigurationException e) {
                    //Should not happen
                }
            }
        }
        return sharingModes;
    }
    static public void buildParameters(String prefix,CommandLine cmd, HashMap<String,HashMap<String,String>>services) {

        for (String option : cmd.getAvailableOptions()) {
            if (option.startsWith(prefix + ":")) {
                try {
                    String optionPart2=option.split(":")[1];
                    String parameter=optionPart2.split("\\.")[0];
                    String serviceName=optionPart2.split("\\.")[1];
                    HashMap<String,String>mapService=services.get(serviceName);
                    mapService.put(parameter,cmd.getOptionStrict(option));

                } catch (CommandLine.ConfigurationException e) {
                    //Should not happen
                }
            }
        }
    }
    static public void validateInput( HashMap<String,HashMap<String,String>> services) {
        ArrayList<String> obligatoryValues = new ArrayList<>(Arrays.asList("Service_File", "Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance"));
        for (String key : services.keySet()) {
            HashMap<String,String>service=(HashMap)services.get(key);
            if (service.keySet().containsAll(obligatoryValues)== false) {

                throw new IllegalArgumentException("Please check the service parameters for the service: "+key+"there must be a GBFS, a Mode file , Scheme type and if its multimodal");
            }
        }
    }
    static public void validateInputGBFS( HashMap<String,HashMap<String,String>> services) {
        ArrayList<String> obligatoryValues = new ArrayList<>(Arrays.asList("Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance"));
        for (String key : services.keySet()) {
            HashMap<String,String>service=(HashMap)services.get(key);
            if(service.get("Scheme").equals("Station-Based")){
                 obligatoryValues = new ArrayList<>(Arrays.asList("Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance","StationsGBFS","StationsStatusGBFS"));
               if (service.keySet().containsAll(obligatoryValues)== false) {


                    throw new IllegalArgumentException("Please check the service parameters for the service: "+key+"there must be  two GBFS, a Mode file , Scheme type and if its multimodal");
                }

            }else{
                obligatoryValues = new ArrayList<>(Arrays.asList("Mode", "Scheme","Service_Name","Multimodal","AccessEgress_Distance","FreeVehiclesGBFS"));
                if (service.keySet().containsAll(obligatoryValues)== false) {


                    throw new IllegalArgumentException("Please check the service parameters for the service: "+key+"there must be a GBFS, a Mode file , Scheme type and if its multimodal");
                }

            }

        }
    }
    public static void generateServiceFile(HashMap<String,HashMap<String,String>>services, Config config,Scenario scenario) throws IOException {
        for (String key : services.keySet()) {
            HashMap<String, String> service = (HashMap) services.get(key);
            if(service.get("Scheme").equals("Station-Based")){
                Network network=scenario.getNetwork();
               service.put("Service_File",ReadStationBasedGBFS.readGBFSStationBased(service.get("StationsGBFS"),"Perro",network,service.get("StationsStatusGBFS"),service.get("Service_Name"))) ;
            }
            if(service.get("Scheme").equals("Free-floating")){
                Network network=scenario.getNetwork();
                service.put("Service_File", ReadFreeFloatingGBFS.readGBFSFreeFloating(service.get("FreeVehiclesGBFS"),"Perro",network,service.get("Service_Name"))) ;
            }
        }
    }
}


