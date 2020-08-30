package edu.boun.edgecloudsim.edge_orchestrator;

import edu.boun.edgecloudsim.core.SimManager;
import edu.boun.edgecloudsim.edge_client.Task;
import edu.boun.edgecloudsim.edge_server.EdgeVM;
import edu.boun.edgecloudsim.mobility.StaticRangeMobility;
import edu.boun.edgecloudsim.storage.RedisListHandler;
import edu.boun.edgecloudsim.utils.Location;
import edu.boun.edgecloudsim.utils.SimLogger;
import edu.boun.edgecloudsim.utils.TaskProperty;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class StorageEdgeOrchestrator extends BasicEdgeOrchestrator {
    public StorageEdgeOrchestrator(String _policy, String _simScenario) {
        super(_policy, _simScenario);
    }
    //Randomly selects one location from list of location where object exists
    private int randomlySelectHostToRead(String locations) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        Random random = new Random();
//        random.setSeed(ObjectGenerator.seed);
        String randomLocation = objectLocations.get(random.nextInt(objectLocations.size()));
        return Integer.parseInt(randomLocation);
    }

    private int selectNearestHostToRead(String locations, Location deviceLocation) {
        List<String> objectLocations = new ArrayList<String>(Arrays.asList(locations.split(" ")));
        List<Integer> intObjectLocations = new ArrayList<>();
        for(String s : objectLocations) intObjectLocations.add(Integer.valueOf(s));
        return StaticRangeMobility.getNearestHost(intObjectLocations, deviceLocation);
    }

    @Override
    public EdgeVM selectVmOnHost(Task task) {
        EdgeVM selectedVM = null;

        Location deviceLocation = SimManager.getInstance().getMobilityModel().getLocation(task.getMobileDeviceId(), CloudSim.clock());
        //in our scenasrio, serving wlan ID is equal to the host id
        //because there is only one host in one place
//        int relatedHostId=deviceLocation.getServingWlanId();
//        List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);

        //Oleg: Get location of object according to policy
        //if not greedy shouldn't read parity at this stage
        //TODO: one more policy - read same object from several locations
        if(!policy.equalsIgnoreCase("NEAREST_WITH_PARITY")){
            //if first char in object name is 'p' it's parity
            if(task.getIsParity() == 1) {
                SimLogger.getInstance().taskRejectedDueToPolicy(task.getCloudletId(), CloudSim.clock());
                return selectedVM;
            }
        }
        if(policy.equalsIgnoreCase("RANDOM_HOST")){
            int relatedHostId= randomlySelectHostToRead(RedisListHandler.getObjectLocations(task.getObjectToRead()));
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }
        else if(policy.equalsIgnoreCase("NEAREST_HOST") || policy.equalsIgnoreCase("NEAREST_WITH_PARITY")){
            int relatedHostId= selectNearestHostToRead(RedisListHandler.getObjectLocations(task.getObjectToRead()),deviceLocation);
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }
/*        else if(policy.equalsIgnoreCase("NEAREST_OR_PARITY")){
            String dataLocations = RedisListHandler.getObjectLocations(task.getObjectToRead());
            int relatedHostId = selectNearestHostToRead(dataLocations, deviceLocation);
            //TODO: incorrect parities to read update
            if (task.getParitiesToRead()>0){
                //Get parity objects
                String[] stripeObjects = RedisListHandler.getStripeObjects(task.getStripeID());
                List<String> parityObjects = new ArrayList<String>(Arrays.asList(stripeObjects[1].split(" ")));
                //TODO: for multiple parities mark it if it was read
                //for each parity
                for (String parityObject:parityObjects){
                    String parityHostID = RedisListHandler.getObjectLocations(parityObject);
                    //Locations of parity+data
                    String allObjectLocations = parityHostID + " " + dataLocations;
                    relatedHostId = selectNearestHostToRead(allObjectLocations,deviceLocation);
                    //Check if parity is closer than data
                    if (relatedHostId==Integer.parseInt(parityHostID)){
                        task.setParitiesToRead(task.getParitiesToRead()-1);
                        task.setObjectRead(parityObject);
                        task.setHostID(relatedHostId);

                    }
                }
            }
            List<EdgeVM> vmArray = SimManager.getInstance().getEdgeServerManager().getVmList(relatedHostId);
            selectedVM = vmArray.get(0);
        }*/
        return selectedVM;
    }
}
