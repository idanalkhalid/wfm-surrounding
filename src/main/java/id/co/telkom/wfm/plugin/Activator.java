package id.co.telkom.wfm.plugin;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();
        registrationList.add(context.registerService(GenerateVRF.class.getName(), new GenerateVRF(), null));
        registrationList.add(context.registerService(GenerateVRFNameExisting.class.getName(), new GenerateVRFNameExisting(), null));
        registrationList.add(context.registerService(GenerateVLANReservation.class.getName(), new GenerateVLANReservation(), null));
        registrationList.add(context.registerService(IPVLANConnecitivty.class.getName(), new IPVLANConnecitivty(), null));
        registrationList.add(context.registerService(GeneratePeName.class.getName(), new GeneratePeName(), null));
        registrationList.add(context.registerService(GenerateMeService.class.getName(), new GenerateMeService(), null));
        registrationList.add(context.registerService(GenerateMeAccess.class.getName(), new GenerateMeAccess(), null));
        registrationList.add(context.registerService(GenerateSidConnectivity.class.getName(), new GenerateSidConnectivity(), null));
        registrationList.add(context.registerService(ValidateSto.class.getName(), new ValidateSto(), null));
        registrationList.add(context.registerService(ValidateVrf.class.getName(), new ValidateVrf(), null));
        registrationList.add(context.registerService(GenerateIPReservation.class.getName(), new GenerateIPReservation(), null));
        registrationList.add(context.registerService(GenerateUplinkPort.class.getName(), new GenerateUplinkPort(), null));
        registrationList.add(context.registerService(GenerateDownlinkPort.class.getName(), new GenerateDownlinkPort(), null));
        registrationList.add(context.registerService(GenerateIpV4.class.getName(), new GenerateIpV4(), null));
        registrationList.add(context.registerService(GenerateStpNetLoc.class.getName(), new GenerateStpNetLoc(), null));
        //Register plugin here
        //registrationList.add(context.registerService(MyPlugin.class.getName(), new MyPlugin(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}
